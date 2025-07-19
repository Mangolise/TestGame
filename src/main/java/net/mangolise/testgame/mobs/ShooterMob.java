package net.mangolise.testgame.mobs;

import net.kyori.adventure.sound.Sound;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.weapons.BowWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.goal.RangedAttackGoal;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ShooterMob extends HostileEntity {
    Weapon weapon = new BowWeapon();

    public ShooterMob(EntityType type, Sound hurtSound, Sound deathSound, Sound walkSound) {
        super(type, hurtSound, deathSound, walkSound);

        this.setItemInMainHand(ItemStack.of(Material.BOW));

        RangedAttackGoal rangedGoal = new RangedAttackGoal(this, Duration.ofMillis(1000), 100, 10, true, 2.0, 1.0);
        rangedGoal.setProjectileGenerator((shooter, targetPos, power, spread) -> {
            if (getTarget() instanceof Entity target) {
                Pos from = getPosition().add(0D, getEyeHeight() + getAttributeValue(Attribute.SCALE), 0D);

                AttackSystem.instance(target.getInstance()).use(this, weapon, tags -> {
                    tags.setTag(BowWeapon.AIM_DIRECTION, shoot(from, targetPos, spread));
                    tags.setTag(BowWeapon.VELOCITY, power * 20.0);
                    tags.setTag(Attack.USER, this);
                    tags.setTag(Attack.TARGET, target);
                });

                attack(target);
            }
        });

        this.addAIGroup(
                List.of(
                        rangedGoal
                ),
                List.of(
                        new AttackTargetSelector(this, entity -> entity instanceof PlayerTeam)
                )
        );
    }

    private Vec shoot(@NotNull Point from, @NotNull Point to, double spread) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();
        if (!hasNoGravity()) {
            final double xzLength = Math.sqrt(dx * dx + dz * dz);
            dy += xzLength * 0.20000000298023224D;
        }

        final double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= length;
        dy /= length;
        dz /= length;
        Random random = ThreadLocalRandom.current();
        spread *= 0.007499999832361937D;
        dx += random.nextGaussian() * spread;
        dy += random.nextGaussian() * spread;
        dz += random.nextGaussian() * spread;

        return new Vec(dx, dy, dz);
    }

    @Override
    public void doTickUpdate(long time) {}

    @Override
    public void attack(@NotNull Entity target, boolean swingHand) {
        super.attack(target, swingHand);

        AttackSystem.instance(target.getInstance()).use(this, weapon, tags -> {
            tags.setTag(Attack.USER, this);
            tags.setTag(Attack.TARGET, target);
        });
    }
}
