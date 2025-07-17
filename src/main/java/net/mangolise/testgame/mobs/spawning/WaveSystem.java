package net.mangolise.testgame.mobs.spawning;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.gamesdk.util.Timer;
import net.mangolise.testgame.GameConstants;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.mods.GenericMods;
import net.mangolise.testgame.combat.weapons.MaceWeapon;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.mobs.MeleeJockeyMob;
import net.mangolise.testgame.mobs.MeleeMob;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WaveSystem {
    
    private double waveStrength(int finishedWaves, int msSinceStart) {
        // Wave strength increases the strength of mobs (and to a certain extent, the number of mobs) in a wave.
        // We want it to scale exponentially, but allow players to survive at first.
        // This will likely need lots of tweaking.
        double wavesMultiplier = 3.0; // increase to make waves stronger faster
        double timeMultiplier = 32.0; // number of seconds until your time multiplier goes to 1.0
        
        double wavesComponent = 3.0 + Math.pow(wavesMultiplier, finishedWaves);
        double timeComponent = Math.max(1.0, timeMultiplier / (msSinceStart * 0.001 + 1.0));
        
        if (timeComponent > 1.0) {
            // If the time component is greater than 1.0, the players finished a wave early!
            instance.sendMessage(Component.text("Wave " + (finishedWaves + 1) + " completed early! The next wave will be boosted!"));
        }
        return 1.0 + (wavesComponent * timeComponent);
    }
    
    private static final Tag<WaveSystem> WAVE_SYSTEM_TAG = Tag.Transient("wave_system");
    
    public static WaveSystem from(Instance instance) {
        if (!instance.hasTag(WAVE_SYSTEM_TAG)) {
            WaveSystem waveSystem = new WaveSystem(instance);
            instance.setTag(WAVE_SYSTEM_TAG, waveSystem);
        }
        return instance.getTag(WAVE_SYSTEM_TAG);
    }
    
    private final Instance instance;
    
    public WaveSystem(Instance instance) {
        this.instance = instance;
    }
    
    private static final Tag<Integer> CURRENT_WAVE_TAG = Tag.Integer("current_wave");
    private static final Tag<Long> LAST_WAVE_START_TAG = Tag.Long("last_wave_start").defaultValue(0L);
    private static final Tag<Boolean> NEXT_WAVE_WAITING_TAG = Tag.Boolean("next_wave_waiting").defaultValue(false);
    private static final Tag<List<AttackableMob>> MOBS_IN_WAVE_TAG = Tag.<List<AttackableMob>>Transient("mobs_in_wave").defaultValue(List.of());
    
    public void start() {
        if (instance.hasTag(CURRENT_WAVE_TAG)) {
            throw new IllegalStateException("Wave system already started in this instance!");
        }
        
        instance.setTag(CURRENT_WAVE_TAG, 0);

        // no need to start the next wave, the instanceTick function will handle that
        instance.eventNode().addListener(InstanceTickEvent.class, event -> instanceTick());
    }

    public int getCurrentWave() {
        return instance.getTag(CURRENT_WAVE_TAG);
    }

    // Starts the timer for the next wave, if it is not already waiting.
    public void startNextWave() {
        if (instance.getTag(NEXT_WAVE_WAITING_TAG)) {
            return;  // already waiting for the next wave
        }

        int currentWave = instance.getTag(CURRENT_WAVE_TAG);
        int displayWave = currentWave + 1;  // display wave is 1-based, currentWave is 0-based
        
        int msSinceStart = (int) (System.currentTimeMillis() - instance.getTag(LAST_WAVE_START_TAG));
        
        double strength = waveStrength(currentWave, msSinceStart);
        double originalStrength = strength;

        List<AttackableMob> mobs = new ArrayList<>();
        
        while (strength > 0.1) {
            EntitySelection selection = sampleEntity(strength);
            strength -= selection.cost;
            AttackableMob entity = selection.entity;
            mobs.add(entity);
        }
        final double finalStrength = strength;

        instance.setTag(NEXT_WAVE_WAITING_TAG, true);

        Timer.countDown(GameConstants.SECONDS_BETWEEN_WAVES, 20, i -> {
            instance.sendActionBar(ChatUtil.toComponent("&aWave &6" + displayWave + " &astarting in &6" + (i) + " &aseconds!"));
        }).thenAccept(ignored -> {
            instance.showTitle(Title.title(
                    ChatUtil.toComponent("&6&lWave " + displayWave),
                    ChatUtil.toComponent("&7Get ready!")
            ));
            instance.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_GROWL.key(), Sound.Source.PLAYER, 0.2f, 1.0f));

            // announce the number of mobs
            Map<Class<? extends AttackableMob>, Integer> mobCount = mobs.stream()
                    .collect(Collectors.groupingBy(AttackableMob::getClass, Collectors.summingInt(mob -> 1)));
            StringBuilder mobCountMessage = new StringBuilder("Wave " + displayWave + " (power " + String.format("%.2f", originalStrength) + ") starting! Mobs: ");
            for (Map.Entry<Class<? extends AttackableMob>, Integer> entry : mobCount.entrySet()) {
                mobCountMessage.append(entry.getKey().getSimpleName()).append(": ").append(entry.getValue()).append(", ");
            }
            // remove the last comma and space
            if (!mobCountMessage.isEmpty()) {
                mobCountMessage.setLength(mobCountMessage.length() - 2);
            }
            instance.sendMessage(Component.text(mobCountMessage.toString()));

            for (AttackableMob mob : mobs) {
                mob.asEntity().getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(1.0 + (currentWave * 0.5));
                mob.asEntity().getAttribute(Attribute.MAX_HEALTH).setBaseValue(10.0 + (currentWave * 2.0));
                mob.asEntity().getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier("speed_boost", Math.pow(1.01, finalStrength) - 1.8, AttributeOperation.ADD_MULTIPLIED_BASE));

                mob.asEntity().scheduler().scheduleTask(() -> {
                    // after 30 seconds, make them glowing
                    if (!mob.asEntity().isRemoved()) {
                        mob.asEntity().setGlowing(true);
                    }
                }, TaskSchedule.seconds(30), TaskSchedule.stop());
                SpawnSystem.spawn(instance, mob.asEntity());
            }

            instance.setTag(LAST_WAVE_START_TAG, System.currentTimeMillis());
            instance.setTag(MOBS_IN_WAVE_TAG, mobs);
            instance.setTag(CURRENT_WAVE_TAG, currentWave + 1);
            instance.setTag(NEXT_WAVE_WAITING_TAG, false);

            SpawnWaveEvent event = new SpawnWaveEvent(instance, mobs, currentWave + 1);
            EventDispatcher.call(event);
        });
    }
    
    private void instanceTick() {
        if (instance.getTag(NEXT_WAVE_WAITING_TAG)) {
            return;  // currently waiting for the next wave to start
        }

        List<AttackableMob> mobsInWave = instance.getTag(MOBS_IN_WAVE_TAG);
        
        if (mobsInWave.isEmpty()) {
            // no mobs in the current wave, start the next one
            startNextWave();
            return;
        }

        // check if all mobs are dead
        boolean allDead = mobsInWave.stream().allMatch(mob -> mob.asEntity().isRemoved());
        if (allDead) {
            // call event
            if (instance.hasTag(CURRENT_WAVE_TAG) && instance.getTag(CURRENT_WAVE_TAG) > 0) {
                CompleteWaveEvent completeWaveEvent = new CompleteWaveEvent(instance, mobsInWave, instance.getTag(CURRENT_WAVE_TAG));
                EventDispatcher.call(completeWaveEvent);
            }
            // all mobs are dead, increment wave count start the next wave
            startNextWave();
        }
    }
    
    private record EntitySelection(AttackableMob entity, double cost) {
    }
    
    private EntitySelection sampleEntity(double strength) {
        while (strength > 16.0) {
            MeleeMob warden = new MeleeMob(EntityType.WARDEN);
            warden.weapon = new MaceWeapon();

            AttackSystem.instance(instance).add(warden, new GenericMods.DoubleAttack(3));
            AttackSystem.instance(instance).add(warden, new GenericMods.TripleAttack(3));
            
            warden.getAttribute(Attribute.MAX_HEALTH).setBaseValue(128.0);
            warden.heal();

            return new EntitySelection(warden, 5.0);
        }
        while (strength > 4.0) {
            if (Math.random() < 0.5) {
                return new EntitySelection(new MeleeJockeyMob(EntityType.CAMEL, EntityType.CREEPER), 1.0);
            }
            
            // TODO: Add more mobs with higher strength levels here
        }

        // fallback to basic mob
        return new EntitySelection(new MeleeMob(EntityType.BOGGED), 0.1);
    }

    public boolean isNextWaveWaiting() {
        return instance.getTag(NEXT_WAVE_WAITING_TAG);
    }
}
