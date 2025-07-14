package net.mangolise.testgame.mobs;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.util.Throttler;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;

public abstract class HostileEntity extends EntityCreature implements AttackableMob {
    public HostileEntity(@NotNull EntityType entityType) {
        super(entityType);
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
    public EntityCreature asEntity() {
        return this;
    }
    
    public abstract void doTickUpdate(long time);

    public final void tick(long time) {
        // skip the ticks when we are not in an instance
        if (this.instance == null) {
            super.tick(time);
            return;
        }
        if (!this.isDead() && Throttler.shouldThrottle(this.instance, "testgame.hostileentity.tick", 20)) {
            return;
        }

        Throttler.useTime(this.instance, "testgame.hostileentity.tick", 20, () -> {
            super.tick(time);

            var instance = this.getInstance();

            if (instance == null) {
                return;
            }

            Vec movementFinisher = new Vec(1, 0.1, 1);
            Vec movementVec = Vec.ZERO;

            var entities = instance.getNearbyEntities(this.position, 3);
            for (Entity entity : entities) {
                if (!(entity instanceof HostileEntity other)) {
                    continue;
                }

                if (other == this) {
                    continue;
                }

                // when this entity is close to the other entity, we apply a force to move away from it
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

            this.doTickUpdate(time);
        });
    }
}
