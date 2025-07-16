package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public record MaceWeapon() implements Weapon {
    public static final Tag<Boolean> IS_LAUNCH_ATTACK = Tag.Boolean("testgame.attack.mace.is_launch_attack");
    public static final Tag<Double> SLAM_RADIUS = Tag.Double("testgame.attack.mace.slam_radius").defaultValue(3.5);
    public static final Tag<Double> SLAM_INTENSITY = Tag.Double("testgame.attack.mace.slam_intensity").defaultValue(20.0);

    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        if (attack.getTag(IS_LAUNCH_ATTACK)) {
            attack.setTag(Attack.DAMAGE, 8.0);
        } else {
            attack.setTag(Attack.DAMAGE, 12.0);
        }

        attack.setTag(Attack.COOLDOWN, 1.5);
        attack.setTag(Attack.CRIT_CHANCE, 0.1);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        for (Attack attack : attacks) {
            LivingEntity user = attack.getTag(Attack.USER);
            if (user == null) {
                throw new IllegalStateException("MaceWeapon attack called without a user set in the tags.");
            }

            Instance instance = user.getInstance();

            if (attack.getTag(IS_LAUNCH_ATTACK)) {
                Collection<Entity> entities = instance.getNearbyEntities(user.getPosition(), attack.getTag(SLAM_RADIUS));

                for (Entity entity : entities) {
                    if (!(entity instanceof AttackableMob mob && attack.canTarget(mob))) {
                        continue;
                    }

                    Entity target = mob.asEntity();

                    Vec newVel = target.getVelocity().withY(target.getVelocity().y() + attack.getTag(SLAM_INTENSITY));
                    target.setVelocity(newVel);
                    mob.applyAttack(DamageType.PLAYER_ATTACK, attack);
                }
            }

            Entity target = attack.getTag(Attack.TARGET);
            if (!(target instanceof AttackableMob mob && attack.canTarget(mob))) {
                continue;
            }

            mob.applyAttack(DamageType.PLAYER_ATTACK, attack);
        }
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.of(Material.MACE).withTag(Weapon.WEAPON_TAG, getId());
    }

    @Override
    public String getId() {
        return "mace";
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }
}
