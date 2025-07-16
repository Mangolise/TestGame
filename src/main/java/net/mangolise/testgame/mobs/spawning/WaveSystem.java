package net.mangolise.testgame.mobs.spawning;

import net.kyori.adventure.text.Component;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.mods.Mod;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.mobs.MeleeJockeyMob;
import net.mangolise.testgame.mobs.MeleeMob;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.lang.constant.Constable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WaveSystem {
    
    private static double waveStrength(int finishedWaves, int msSinceStart) {
        // Wave strength increases the strength of mobs (and to a certain extent, the number of mobs) in a wave.
        // We want it to scale exponentially, but allow players to survive at first.
        // This will likely need lots of tweaking.
        double wavesMultiplier = 1.5; // increase to make waves stronger faster
        double timeMultiplier = 32.0; // number of seconds until your time multiplier goes to 1.0
        
        double wavesComponent = Math.pow(wavesMultiplier, finishedWaves);
        double timeComponent = Math.max(1.0, timeMultiplier / (msSinceStart * 0.001 + 1.0));
        
        if (timeComponent > 1.0) {
            // If the time component is greater than 1.0, the players finished a wave early!
            Audiences.players().sendMessage(Component.text("Wave " + (finishedWaves + 1) + " completed early! The next wave will be boosted!"));
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
    private static final Tag<List<AttackableMob>> MOBS_IN_WAVE_TAG = Tag.<List<AttackableMob>>Transient("mobs_in_wave").defaultValue(List.of());
    
    public void start() {
        if (instance.hasTag(CURRENT_WAVE_TAG)) {
            throw new IllegalStateException("Wave system already started in this instance!");
        }
        
        instance.setTag(CURRENT_WAVE_TAG, 0);

        // no need to start the next wave, the instanceTick function will handle that
        instance.eventNode().addListener(InstanceTickEvent.class, event -> instanceTick());
    }
    
    private void startNextWave() {
        // TODO: Countdown (and wait for) next wave
        int currentWave = instance.getTag(CURRENT_WAVE_TAG);
        
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
        
        // announce the number of mobs
//        Audiences.players().sendMessage(Component.text("Wave " + (currentWave + 1) + " starting! " + mobs.size() + " mobs will spawn!"));
        Map<Class<? extends AttackableMob>, Integer> mobCount = mobs.stream()
                .collect(Collectors.groupingBy(AttackableMob::getClass, Collectors.summingInt(mob -> 1)));
        StringBuilder mobCountMessage = new StringBuilder("Wave " + (currentWave + 1) + " (power " + String.format("%.2f", originalStrength) + ") starting! Mobs: ");
        for (Map.Entry<Class<? extends AttackableMob>, Integer> entry : mobCount.entrySet()) {
            mobCountMessage.append(entry.getKey().getSimpleName()).append(": ").append(entry.getValue()).append(", ");
        }
        // remove the last comma and space
        if (mobCountMessage.length() > 0) {
            mobCountMessage.setLength(mobCountMessage.length() - 2);
        }
        Audiences.players().sendMessage(Component.text(mobCountMessage.toString()));

        for (AttackableMob mob : mobs) {
            mob.asEntity().getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(1.0 + (currentWave * 0.5));
            mob.asEntity().getAttribute(Attribute.MAX_HEALTH).setBaseValue(10.0 + (currentWave * 2.0));
            mob.asEntity().getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier("speed_boost", 0.1 * currentWave, AttributeOperation.ADD_VALUE));

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
        
        SpawnWaveEvent event = new SpawnWaveEvent(instance, mobs, currentWave + 1);
        EventDispatcher.call(event);
    }
    
    private void instanceTick() {
        var mobsInWave = instance.getTag(MOBS_IN_WAVE_TAG);
        
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
        while (strength > 4.0) {
            if (Math.random() < 0.5) {
                return new EntitySelection(new MeleeJockeyMob(EntityType.CAMEL, EntityType.CREEPER), 1.0);
            }
            
            // TODO: Add more mobs with higher strength levels here
        }

        // fallback to basic mob
        return new EntitySelection(new MeleeMob(EntityType.BOGGED), 0.1);
    }
}
