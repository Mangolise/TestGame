package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.tag.Tag;

import java.util.List;
import java.util.function.Consumer;

public record SwordWeapon(int level) implements Weapon {
    public static final Tag<Player> SWORD_USER = Tag.Transient("testgame.attack.sword.user");

    @Override
    public void attack(Attack attack, Consumer<Attack> next) {
        // Sword has a base damage of 8.0, and increases by 0.5 per level
        attack.setTag(Attack.DAMAGE, 8.0 + level * 0.5);

        attack.setTag(Attack.COOLDOWN, 0.5);

        // Sword has a base crit change of 0.5, and increases by 0.1 per level
        attack.setTag(Attack.CRIT_CHANCE, 0.5 + level * 0.1);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        for (Attack attack : attacks) {
            Player user = attack.getTag(SWORD_USER);
            if (user == null) {
                throw new IllegalStateException("SwordWeapon attack called without a user set in the tags.");
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
