package net.mangolise.testgame.mobs;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.util.ThrottledScheduler;
import net.mangolise.testgame.util.Throttler;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.goal.FollowTargetGoal;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.registry.RegistryKey;

import java.time.Duration;
import java.util.List;

public class TestZombie extends EntityCreature implements AttackableMob {
    public TestZombie() {
        super(EntityType.ZOMBIE);

        this.addAIGroup(
                List.of(
                        new FollowTargetGoal(this, Duration.ofSeconds(1).plus(Duration.ofMillis((long) (1000.0 * Math.random())))),
                        new MeleeAttackGoal(this, 1.0, Duration.ofSeconds(1))
                ),
                List.of()
        );
    }

    @Override
    public void applyAttack(RegistryKey<DamageType> type, Attack attack) {
        // Apply the attack to the zombie, e.g., reduce health
        var user = attack.getTag(Attack.USER);
        double damage = attack.getTag(Attack.DAMAGE);

        // convert crit into additional damage
        double critMultiplier = 1.0 + attack.sampleCrits();
        
        // when the crit multiplier is above 1.9, ping a ding sound to the user if they are a player
        if (critMultiplier > 1.9 && user instanceof Player player) {
            player.playSound(Sound.sound(Key.key("minecraft:entity.experience_orb.pickup"), Sound.Source.HOSTILE, 0.01f, 1f));
        }
        
        this.damage(new Damage(type, null, user, null, (float) (damage * critMultiplier)));
    }

    @Override
    public Entity asEntity() {
        return this;
    }

    @Override
    public void tick(long time) {

        if (!this.isDead() && Throttler.shouldThrottle(this.instance, "testgame.zombie.tick", 10)) {
            if (Math.random() > 0.99) { // randomly tick, despite being throttled
                return;
            }
        }
        
        Throttler.useTime(this.instance, "testgame.zombie.tick", 10, () -> {
            super.tick(time);

            var instance = this.getInstance();

            if (instance == null) {
                return;
            }

            Vec movementFinisher = new Vec(1, 0.1, 1);
            Vec movementVec = Vec.ZERO;
            for (Entity entity : instance.getEntities()) {
                if (!(entity instanceof TestZombie other)) {
                    continue;
                }

                if (other == this) {
                    continue;
                }

                // when the zombie is close to another zombie, move this zombie away
                if (other.getPosition().distance(this.getPosition()) > 3) {
                    continue;
                }
                Vec thisPosition = Vec.fromPoint(this.getPosition());
                Vec otherPosition = Vec.fromPoint(other.getPosition());
                Vec difference = thisPosition.sub(otherPosition);
                Vec direction = difference.normalize();

                // amplify the force based on the distance
                double distance = difference.length();
                if (distance < 0.2) {
                    distance = 0.2;
                }

                double force = 1.0 / (distance * distance);
                
                // increase the force based on the size of the other zombie
                double otherScale = other.getAttribute(Attribute.SCALE).getValue();
                force *= (otherScale * otherScale);

                movementVec = movementVec.add(direction.mul(force));
            }

            // apply the movement vector to the zombie
            if (!movementVec.equals(Vec.ZERO)) {
                this.setVelocity(this.getVelocity().add(movementVec.mul(movementFinisher)));
            }
        });
    }
}
