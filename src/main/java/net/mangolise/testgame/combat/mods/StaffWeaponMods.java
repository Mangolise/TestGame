package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.StaffWeapon;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

public sealed interface StaffWeaponMods extends Mod {

    record ArcChance(int level) implements StaffWeaponMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.END_ROD)
                    .lore(
                            Component.text("+ Adds to the chance for an arc to happen", NamedTextColor.GREEN),
                            Component.text("    Arc chance Addition: 1.0 + (0.1 per level)", NamedTextColor.RED)
                    )
                    .amount(1)
                    .build();
        }

        @Override
        public void attack(Attack tags, @UnknownNullability Consumer<Attack> next) {
            double arcChance = 1.0 + level;
            tags.updateTag(StaffWeapon.ARC_CHANCE, arc -> arc + arcChance);
            next.accept(tags);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
}
