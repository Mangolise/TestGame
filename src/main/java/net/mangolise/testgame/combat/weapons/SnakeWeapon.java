package net.mangolise.testgame.combat.weapons;

import net.kyori.adventure.text.Component;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.audience.Audiences;
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
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * A weapon that snakes along the ground, and forks when an enemy is hit.
 * @param level the level of the weapon, which affects its damage, crit chance, and cooldown.
 */
public record SnakeWeapon(int level) implements Attack.Node {

    public static final Tag<Double> SPEED = Tag.Double("testgame.attack.snake.speed").defaultValue(1.0);
    public static final Tag<Double> ALIVE_TICKS = Tag.Double("testgame.attack.snake.aliveticks").defaultValue(20.0 * 3.0);

    @Override
    public void attack(Attack attack, Consumer<Attack> next) {

        if (next != null) {
            // modifiers only
            attack.setTag(Attack.DAMAGE, 1.0 + level * 0.5);
            attack.setTag(Attack.CRIT_CHANCE, 0.1 + level * 0.1);
            attack.setTag(Attack.COOLDOWN, 0.1 + level * 0.1);

            next.accept(attack);
            return;
        }

        // next == null means that we perform the attack
        Entity user = attack.getTag(Attack.USER);
        if (user == null) {
            throw new IllegalStateException("Attack called without a user set in the tags.");
        }
        
        // find the closest damageable entity
        Entity target = closestAttackableEntity(user);
        
        if (target == null) {
            // no target found, nothing to do
            return;
        }

        // spawn snake
        Set<Entity> alreadyHit = Collections.newSetFromMap(new WeakHashMap<>());
        Snake snake = new Snake(user.getInstance(), attack, user.getPosition(), target, attack.getTag(SnakeWeapon.ALIVE_TICKS).intValue(), alreadyHit);
        snake.init();
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }
    
    private static @Nullable Entity closestAttackableEntity(Entity user) {
        double closestDistance = Double.MAX_VALUE;
        Entity closestEntity = null;
        
        for (Entity entity : user.getInstance().getEntities()) {
            if (!(entity instanceof AttackableMob) || entity == user) {
                continue; // skip non-attackable mobs or the user
            }
            
            double distance = user.getPosition().distanceSquared(entity.getPosition());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestEntity = entity;
            }
        }
        
        return closestEntity;
    }
}

class Snake {
    
    private final Instance instance;
    private final Attack attack;
    private final Entity target;
    private final Set<Entity> alreadyMarked;
    
    private Pos pos;
    private int remainingTicks;
    
    private int msSinceLastHit = 0;
    
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
        double speedBonus = msSinceLastHit * 0.01;
        
        // move towards the target
        Vec direction = target.getPosition().asVec().sub(pos).normalize().mul(attack.getTag(SnakeWeapon.SPEED));
        direction = direction.mul(1.0 + speedBonus);
        
        // make it relative to the time
        direction = direction.mul(Math.max(tickMs, 1) / 1000.0);
        
        pos = pos.add(direction);
        
        // check if we hit the target
        if (target.getPosition().distanceSquared(pos) < 0.5 * 0.5) {
            // hit the target
            ((AttackableMob) target).applyAttack(DamageType.IN_WALL, attack);
            
            // fork the snake
            forkSnake();
            forkSnake();
            
            // KILL this snake
            remainingTicks = 0;
            
            // spawn particles
            var entityScale = target instanceof LivingEntity living ? living.getAttribute(Attribute.SCALE).getBaseValue() : 1.0;
            ParticlePacket packet = new ParticlePacket(Particle.ANGRY_VILLAGER, pos.add(0, target.getEyeHeight() * entityScale, 0), Vec.ZERO, 0, 1);
            instance.sendGroupedPacket(packet);
        }
        
        // show particles
        ParticlePacket packet = new ParticlePacket(Particle.COMPOSTER, pos, Vec.ZERO, 0, 1);
        instance.sendGroupedPacket(packet);

        remainingTicks--;
        msSinceLastHit += tickMs;
    }

    private void forkSnake() {
        // find the closest attackable entity again
        Entity newTarget = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : instance.getEntities()) {
            if (!(entity instanceof AttackableMob) || alreadyMarked.contains(entity) || entity instanceof Player) {
                continue;
            }
            
            double distance = pos.distanceSquared(entity.getPosition());
            if (distance < closestDistance) {
                closestDistance = distance;
                newTarget = entity;
            }
        }
        
        if (newTarget == null) {
            return;
        }
        
        // create a new snake that forks from this one
        alreadyMarked.add(newTarget);
        Snake forkedSnake = new Snake(instance, attack, pos, newTarget, remainingTicks, alreadyMarked);
        forkedSnake.init();
    }
}
