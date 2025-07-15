package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public record MaceWeapon(int level) implements Weapon {
    public static final Tag<Player> MACE_USER = Tag.Transient("testgame.attack.mace.user");
    public static final Tag<Boolean> IS_LAUNCH_ATTACK = Tag.Transient("testgame.attack.mace.is_launch_attack");
    public static final Tag<Double> SLAM_RADIUS = Tag.Double("testgame.attack.mace.is_launch_attack").defaultValue(4.0);

    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        // Mace has a base damage of 12.0, and increases by 0.5 per level
        if (attack.getTag(IS_LAUNCH_ATTACK)) {
            attack.setTag(Attack.DAMAGE, 8.0 + level * 0.5);
        } else {
            attack.setTag(Attack.DAMAGE, 12.0 + level * 0.5);
        }

        attack.setTag(Attack.COOLDOWN, 1.5);

        // Mace has a base crit change of 0.1, and increases by 0.1 per level
        attack.setTag(Attack.CRIT_CHANCE, 0.1 + level * 0.1);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        for (Attack attack : attacks) {
            // next == null means that we perform the attack
            Player user = attack.getTag(MACE_USER);
            if (user == null) {
                throw new IllegalStateException("MaceWeapon attack called without a user set in the tags.");
            }

            Instance instance = user.getInstance();

            if (attack.getTag(IS_LAUNCH_ATTACK)) {
                Collection<Entity> entities = instance.getNearbyEntities(user.getPosition(), 5);

                for (Entity entity : entities) {
                    if (!(entity instanceof AttackableMob mob)) {
                        continue;
                    }

                    Entity target = mob.asEntity();

                    Vec newVel = target.getVelocity().withY(target.getVelocity().y() + 20);
                    target.setVelocity(newVel);
                    mob.applyAttack(DamageType.PLAYER_ATTACK, attack);
                }
            }

            Entity target = attack.getTag(Attack.TARGET);
            if (!(target instanceof AttackableMob mob)) {
                continue;
            }

            mob.applyAttack(DamageType.PLAYER_ATTACK, attack);
        }
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }
}
