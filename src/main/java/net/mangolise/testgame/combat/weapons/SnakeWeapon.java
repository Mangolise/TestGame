package net.mangolise.testgame.combat.weapons;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.util.MathUtils;
import net.mangolise.testgame.util.ThrottledScheduler;
import net.mangolise.testgame.util.Utils;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.collision.Aerodynamics;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * A weapon that snakes along the ground, and forks when an enemy is hit.
 */
public record SnakeWeapon() implements Weapon {

    public static final Tag<Double> ALIVE_TICKS = Tag.Double("testgame.attack.snake.aliveticks").defaultValue(20.0 * 3.0);
    
    // blocks per second, per second
    public static final Tag<Double> ACCELERATION = Tag.Double("testgame.attack.snake.acceleration").defaultValue(3.0);

    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        // modifiers only
        attack.setTag(Attack.DAMAGE, 1.0);
        attack.setTag(Attack.CRIT_CHANCE, 0.1);
        attack.setTag(Attack.COOLDOWN, 0.1);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        Set<AttackableMob> alreadyMarked = Collections.newSetFromMap(new WeakHashMap<>());
        
        for (Attack attack : attacks) {
            // next == null means that we perform the attack
            Entity user = attack.getTag(Attack.USER);
            if (user == null) {
                throw new IllegalStateException("Attack called without a user set in the tags.");
            }

            // find the closest damageable entity
            AttackableMob target = (AttackableMob) Utils.fastClosestEntity(user.getInstance(), user.getPosition(), entity ->
                    entity instanceof AttackableMob mob &&
                    attack.canTarget(mob) &&
                    entity != user &&
                    !alreadyMarked.contains(entity)
            );

            if (target == null) {
                // no target found, nothing to do
                return;
            }

            alreadyMarked.add(target);

            // spawn snake
            Snake snake = new Snake(user.getInstance(), attack, user.getPosition(), target, attack.getTag(SnakeWeapon.ALIVE_TICKS).intValue(), alreadyMarked);
            snake.init();
        }
    }

    @Override
    public ItemStack.Builder generateItem() {
        return ItemStack.builder(Material.STICK)
                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .customName(ChatUtil.toComponent("&r&a&lSnake"))
                .lore(
                        ChatUtil.toComponent("&7Spawn a slithery snake that forks upon critting an enemy."),
                        ChatUtil.toComponent("&7When it hits a hoard it can cause a chain reaction!")
                )
                .set(DataComponents.ITEM_MODEL, "minecraft:hypnosis");
    }

    @Override
    public String getId() {
        return "snake";
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }

    private static class Snake {

        private final Instance instance;
        private final Attack attack;
        private final AttackableMob target;
        private final Set<AttackableMob> alreadyMarked;

        private Pos pos;
        private int remainingTicks;

        private int msSinceLastHit = 0;
        private final Vec randomCurve = Vec.ONE.mul(Math.random()).sub(0.5, 0, 0.5).mul(0.1);
        private final SnakeDisplay display;

        public Snake(Instance instance, Attack attack, Pos pos, AttackableMob target, int remainingTicks, Set<AttackableMob> alreadyMarked) {
            this.instance = instance;
            this.attack = attack;
            this.pos = pos;
            this.target = target;
            this.remainingTicks = remainingTicks;
            this.alreadyMarked = alreadyMarked;
            
            this.display = new SnakeDisplay();
            this.display.move(instance, pos, Vec.ZERO);
        }

        public void init() {
            instance.eventNode().addListener(EventListener.builder(InstanceTickEvent.class)
                    .handler(event -> handleMoveTick(event.getDuration()))
                    .expireWhen(e -> {
                        if (!instance.isRegistered() || remainingTicks <= 0) {
                            // remove the snake display
                            display.remove();
                            return true;
                        }
                        return false;
                    })
                    .build());
        }

        private void handleMoveTick(int tickMs) {
            LivingEntity entity = target.asEntity();

            // if the target is dead, find a new target
            if (entity.isRemoved() || entity.isDead()) {
                forkSnake(pos, remainingTicks);
                
                // KILL this snake (peacefully)
                remainingTicks = 0;
            }

            // move towards the target
            double speedBonus = (msSinceLastHit / 1000.0) * attack.getTag(SnakeWeapon.ACCELERATION);
            Vec direction = entity.getPosition().asVec().sub(pos).normalize();
            direction = direction.mul(speedBonus);

            // make it relative to the time
            direction = direction.mul(Math.max(tickMs, 1) / 1000.0);
            
            Point posBefore = pos.asVec();

            pos = pos.add(direction);
            pos = pos.add(randomCurve);

            Vec velocity = pos.asVec().sub(posBefore);

            // check if we hit the target
            if (entity.getPosition().distanceSquared(pos) < speedBonus * speedBonus * 0.01) {
                // hit the target
                target.applyAttack(DamageType.IN_WALL, attack);

                // fork the snake
                int childrenRemainingTicks = remainingTicks - 1;
                ThrottledScheduler.use(instance, "snake-weapon-fork-attack", 4, () -> {
                    forkSnake(pos, childrenRemainingTicks);
                    
                    Attack samplingAttack = attack.copy(true);
                    
                    samplingAttack.updateTag(Attack.CRIT_CHANCE, critChance -> critChance * (1.0 - Math.pow(Math.random(), 3.0)));
                    
                    int numCrits = samplingAttack.sampleCrits();
                    for (int i = 0; i < numCrits; i++) {
                        forkSnake(pos, childrenRemainingTicks);
                    }
                    if (numCrits > 0 && attack.getTag(Attack.USER) instanceof Player player) {
                        player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_PLING.key(), Sound.Source.PLAYER, 0.5f, 1.0f));
                        
                        ThreadLocalRandom random = ThreadLocalRandom.current();
                        TextColor color = TextColor.color(
                                random.nextInt(255),
                                random.nextInt(255),
                                random.nextInt(255)
                        );
                        player.sendActionBar(Component.text("Snake fork!", color).decorate(TextDecoration.BOLD));
                    }
                });

                // KILL this snake (with minimal pain)
                remainingTicks = 0;

                // spawn particles
                var entityScale = entity.getAttribute(Attribute.SCALE).getValue();
                ParticlePacket packet = new ParticlePacket(Particle.ANGRY_VILLAGER, pos.add(0, entity.getEyeHeight() * entityScale, 0), Vec.ZERO, 0, 1);
                instance.sendGroupedPacket(packet);
            }
            
            // update pos direction
            pos = pos.withDirection(direction);

            // update rendered snake
            display.move(instance, pos, velocity);

            remainingTicks--;
            msSinceLastHit += tickMs;
        }

        private void forkSnake(Point pos, int remainingTicks) {
            AttackableMob newTarget = (AttackableMob) Utils.fastClosestEntity(instance, pos, entity ->
                    entity instanceof AttackableMob mob &&
                    attack.canTarget(mob) &&
                    !alreadyMarked.contains(entity)
            );

            if (newTarget == null) {
                return;
            }

            // create a new snake that forks from this one
            alreadyMarked.add(newTarget);
            
            // every time we fork, lower the damage
            Attack forkedAttack = attack.copy(true);
            forkedAttack.updateTag(Attack.DAMAGE, damage -> damage * 0.9);
            
            // if the damage is really low, don't fork
            if (forkedAttack.getTag(Attack.DAMAGE) < 0.1) {
                return;
            }
            
            Snake forkedSnake = new Snake(instance, forkedAttack, Pos.fromPoint(pos), newTarget, remainingTicks, alreadyMarked);
            forkedSnake.init();
        }
    }
    
    private static class SnakeDisplay {
        
        private static class DisplayEntity extends Entity {

            public DisplayEntity(@Nullable Entity parent, Vec translation, Vec scale, Vec rotation, String model) {
                this(translation, scale, rotation, model);
                
                parent.eventNode().addListener(EntityTickEvent.class, event -> {
                    Point pos = parent.getPosition();
                    Vec velo = parent.getVelocity();
                    parent.scheduler().scheduleTask(() -> {
                        if (this.isRemoved()) return;
                        
                        this.setInstance(parent.getInstance(), pos);
                        this.setVelocity(velo);

                        double speed = Math.max(velo.length(), 0.1) * 10.0;
                        this.editEntityMeta(AbstractDisplayMeta.class, meta -> {
                            Vec currentScale = meta.getScale();
                            Vec newScale = new Vec(currentScale.x(), currentScale.y(), speed);
                            meta.setScale(newScale);
                            
                            // move forward by half of the scale offset
                            Vec translationOffset = new Vec(0, 0, speed * 0.015);
                            Point newTranslation = meta.getTranslation().add(translationOffset);
                            meta.setTranslation(newTranslation);
                        });
                    }, TaskSchedule.tick(1), TaskSchedule.stop());
                });
                
                parent.eventNode().addListener(RemoveEntityFromInstanceEvent.class, event -> {
                    if (!this.isRemoved()) {
                        this.remove();
                    }
                });
            }

            public DisplayEntity(Vec translation, Vec scale, Vec rotation, String model) {
                super(EntityType.ITEM_DISPLAY);

                editEntityMeta(ItemDisplayMeta.class, meta -> {
                    meta.setItemStack(ItemStack.of(Material.STONE).with(DataComponents.ITEM_MODEL, model));
                    meta.setScale(scale);
                    meta.setTranslation(translation);
                    meta.setPosRotInterpolationDuration(2);
                    meta.setRightRotation(MathUtils.createQuaternionFromEuler(rotation));
                });

                setBoundingBox(new BoundingBox(Vec.ZERO, scale));
                
                setNoGravity(true);
                this.setAerodynamics(new Aerodynamics(0, 0, 0));
            }
        }
        
        private final DisplayEntity head = new DisplayEntity(new Vec(0, 0.5, 0.2), new Vec(0.9), Vec.ZERO, "minecraft:snake_head");

        public SnakeDisplay() {
            int bodySegments = 5;

            Entity parent = head;

            for (int i = 0; i < bodySegments; i++) {
                parent = new DisplayEntity(parent, new Vec(0, 0.3, 0.4), new Vec(1.3), new Vec(90, 0, 0), "minecraft:snake_body");
            }
            new DisplayEntity(parent, new Vec(0, 0.3, 0.4), new Vec(1.3), new Vec(90, 0, 0), "minecraft:snake_tail");
        }
        
        public void move(Instance instance, Pos pos, Vec velocity) {
            
            Point direction = pos.direction();
            
            Pos headPos = pos.add(direction.mul(0.0));
            
            head.setInstance(instance, headPos);
            head.setVelocity(velocity);
        }
        
        public void remove() {
            if (!head.isRemoved()) {
                head.remove();
            }
        }
    }
}
