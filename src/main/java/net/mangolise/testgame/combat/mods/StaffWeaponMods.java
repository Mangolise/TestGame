package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.StaffWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Consumer;

public sealed interface StaffWeaponMods extends Mod {

    record ArcChance(int level) implements StaffWeaponMods {
        public Component name() {
            return Component.text("Arc Chance").color(this.rarity().color());
        }

        @Override
        public int maxLevel() {
            return 4;
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.BREEZE_ROD,
                    List.of("Staff: +20% Arc Chance"),
                    List.of("Staff: +10% Cooldown"));
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(StaffWeapon.ARC_CHANCE, arc -> arc * (1.20 + (level * 0.20)));
            if (attack.weapon() instanceof StaffWeapon) {
                attack.updateTag(Attack.COOLDOWN, arc -> arc * (1.10 + (level * 0.10)));
            }
            next.accept(attack);
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new StaffWeapon());
        }

        @Override
        public double priority() {
            return PRIORITY_MULTIPLICATIVE_MODIFIER;
        }
    }

    record ArcRadius(int level) implements StaffWeaponMods {
        public Component name() {
            return Component.text("Arc Radius").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.NETHER_STAR,
                    List.of("Staff: +0.5 Block Arc Range"),
                    List.of("Staff: -1.0 Damage"));
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new StaffWeapon());
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(StaffWeapon.ARC_RADIUS, arc -> arc + (0.5 + level * 0.5));
            attack.updateTag(Attack.DAMAGE, damage -> damage - (1.0 + (1.0 * level)));
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
}
