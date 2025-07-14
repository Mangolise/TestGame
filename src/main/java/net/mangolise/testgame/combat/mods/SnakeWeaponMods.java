package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.SnakeWeapon;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

public sealed interface SnakeWeaponMods extends Mod {
    record Acceleration(int level) implements SnakeWeaponMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.END_ROD)
                    .lore(
                            Component.text("+ Multiplies the acceleration of your snakes", NamedTextColor.GREEN),
                            Component.text("    Acceleration Multiplication: 1.0 + (1.0 per level)", NamedTextColor.RED)
                    )
                    .amount(1)
                    .build();
        }

        @Override
        public void attack(Attack tags, @UnknownNullability Consumer<Attack> next) {
            tags.updateTag(SnakeWeapon.ACCELERATION, acceleration -> acceleration * (1.0 + (1.0 * level)));
            next.accept(tags);
        }

        @Override
        public double priority() {
            return PRIORITY_MULTIPLICATIVE_MODIFIER;
        }
    }
}
