package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.BREEZE_ROD)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Arc Chance: +10% per level", NamedTextColor.GREEN)
                    )
                    .amount(1)
                    .build();
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            double arcChance = 1.0 + level;
            attack.updateTag(StaffWeapon.ARC_CHANCE, arc -> arc + arcChance);
            next.accept(attack);
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new StaffWeapon());
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record ArcRadius(int level) implements StaffWeaponMods {
        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.NETHER_STAR)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Arc Distance: +1.0 block per level", NamedTextColor.GREEN),
                            Component.text("- Damage: -0.5 damage", NamedTextColor.RED)
                    )
                    .amount(1)
                    .build();
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new StaffWeapon());
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            double arcRadius = 3.0 + level;
            attack.updateTag(StaffWeapon.ARC_RADIUS, arc -> arc + arcRadius);
            attack.updateTag(Attack.DAMAGE, damage -> damage - 0.5);
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
}
