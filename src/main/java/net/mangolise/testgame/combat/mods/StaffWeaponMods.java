package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        public int maxLevel() {
            return 4;
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.BREEZE_ROD)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("Staff: +5% Arc Chance", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("Staff: +5% Cooldown", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                    )
                    .amount(1)
                    .build();
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(StaffWeapon.ARC_CHANCE, arc -> arc * (0.05 + (level * 0.05)));
            if (attack.weapon() instanceof StaffWeapon) {
                attack.updateTag(Attack.COOLDOWN, arc -> arc * (1.05 + (level * 0.05)));
            }
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("Staff: +0.5 Block Arc Range", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("Staff: -1.0 Damage", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
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
            attack.updateTag(StaffWeapon.ARC_RADIUS, arc -> arc + (1.0 + (0.5 + level * 0.5)));
            attack.updateTag(Attack.DAMAGE, damage -> damage - (1.0 + (1.0 * level)));
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
}
