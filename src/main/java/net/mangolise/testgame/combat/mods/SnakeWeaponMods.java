package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.SnakeWeapon;
import net.mangolise.testgame.combat.weapons.StaffWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Consumer;

public sealed interface SnakeWeaponMods extends Mod {
    record Acceleration(int level) implements SnakeWeaponMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.IRON_BOOTS)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Multiplies the acceleration of your snakes", NamedTextColor.GREEN),
                            Component.text("    Acceleration Multiplication: 1.0 + (1.0 per level)", NamedTextColor.GREEN)
                    )
                    .amount(1)
                    .build();
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new SnakeWeapon(1));
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
