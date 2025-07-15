package net.mangolise.testgame.combat.weapons;

import net.krystilize.pathable.Path;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.testgame.combat.mods.Mod;
import net.mangolise.testgame.util.ThrottledScheduler;
import net.mangolise.testgame.util.Utils;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * A weapon that snakes along the ground, and forks when an enemy is hit.
 * @param level the level of the weapon, which affects its damage, crit chance, and cooldown.
 */
public record SnakeWeapon(int level) implements Weapon {

    public static final Tag<Double> ALIVE_TICKS = Tag.Double("testgame.attack.snake.aliveticks").defaultValue(20.0 * 3.0);
    
    // blocks per second, per second
    public static final Tag<Double> ACCELERATION = Tag.Double("testgame.attack.snake.acceleration").defaultValue(3.0);

    @Override
    public void attack(Attack attack, Consumer<Attack> next) {

        // modifiers only
        attack.setTag(Attack.DAMAGE, 1.0 + level * 0.5);
        attack.setTag(Attack.CRIT_CHANCE, 0.1 + level * 0.1);
        attack.setTag(Attack.COOLDOWN, 0.1 + level * 0.1);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        Set<Entity> alreadyMarked = Collections.newSetFromMap(new WeakHashMap<>());
        
        for (Attack attack : attacks) {
            // next == null means that we perform the attack
            Entity user = attack.getTag(Attack.USER);
            if (user == null) {
                throw new IllegalStateException("Attack called without a user set in the tags.");
            }

            // find the closest damageable entity
            Entity target = Utils.closestEntity(user.getInstance(), user.getPosition(), entity ->
                    entity instanceof AttackableMob &&
                    entity != user &&
                    !(entity instanceof Player) &&
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
    public double priority() {
        return PRIORITY_WEAPON;
    }

    static class Snake {

        private final Instance instance;
        private final Attack attack;
        private final Entity target;
        private final Set<Entity> alreadyMarked;

        private Pos pos;
        private int remainingTicks;

        private int msSinceLastHit = 0;
        private final Vec randomCurve = Vec.ONE.mul(Math.random()).sub(0.5, 0, 0.5).mul(0.1);

        public Snake(Instance instance, Attack attack, Pos pos, Entity target, int remainingTicks, Set<Entity> alreadyMarked) {
            this.instance = instance;
            this.attack = attack;
            this.pos = pos;
            this.target = target;
            this.remainingTicks = remainingTicks;
            this.alreadyMarked = alreadyMarked;
        }

        public void init() {
            MinecraftServer.getGlobalEventHandler().addListener(EventListener.builder(InstanceTickEvent.class)
                    .handler(event -> handleMoveTick(event.getDuration()))
                    .filter(event -> event.getInstance() == instance)
                    .expireWhen(e -> !instance.isRegistered() || remainingTicks <= 0)
                    .build());
        }

        private void handleMoveTick(int tickMs) {
            // if the target is dead, find a new target
            if (target.isRemoved() || (target instanceof LivingEntity living && living.isDead())) {
                forkSnake(pos, remainingTicks);
                
                // KILL this snake
                remainingTicks = 0;
            }

            // move towards the target
            double speedBonus = (msSinceLastHit / 1000.0) * attack.getTag(SnakeWeapon.ACCELERATION);
            Vec direction = target.getPosition().asVec().sub(pos).normalize();
            direction = direction.mul(speedBonus);

            // make it relative to the time
            direction = direction.mul(Math.max(tickMs, 1) / 1000.0);

            pos = pos.add(direction);
            pos = pos.add(randomCurve);

            // check if we hit the target
            if (target.getPosition().distanceSquared(pos) < speedBonus * speedBonus * 0.25) {
                // hit the target
                ((AttackableMob) target).applyAttack(DamageType.IN_WALL, attack);

                // fork the snake
                int childrenRemainingTicks = remainingTicks - 1;
                ThrottledScheduler.use(instance, "snake-weapon-fork-attack", 4, () -> {
                    forkSnake(pos, childrenRemainingTicks);
                    
                    int numCrits = attack.copy(true).sampleCrits();
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

                // KILL this snake
                remainingTicks = 0;

                // spawn particles
                var entityScale = target instanceof LivingEntity living ? living.getAttribute(Attribute.SCALE).getValue() : 1.0;
                ParticlePacket packet = new ParticlePacket(Particle.ANGRY_VILLAGER, pos.add(0, target.getEyeHeight() * entityScale, 0), Vec.ZERO, 0, 1);
                instance.sendGroupedPacket(packet);
            }

            // show particles
            ParticlePacket packet = new ParticlePacket(Particle.COMPOSTER, pos, Vec.ZERO, 0, 1);
            instance.sendGroupedPacket(packet);

            remainingTicks--;
            msSinceLastHit += tickMs;
        }

        private void forkSnake(Point pos, int remainingTicks) {
            var newTarget = Utils.closestEntity(instance, pos, entity ->
                    entity instanceof AttackableMob &&
                            !alreadyMarked.contains(entity) &&
                            !(entity instanceof Player)
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
}
