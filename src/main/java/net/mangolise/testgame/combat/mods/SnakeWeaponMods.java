package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.SnakeWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Consumer;

public sealed interface SnakeWeaponMods extends Mod {
    record Acceleration(int level) implements SnakeWeaponMods {
        public Component name() {
            return Component.text("Acceleration").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.IRON_BOOTS,
                    List.of("Snake: +1.0 Acceleration"), // TODO: fix description?, behaviour should not change
                    List.of());
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new SnakeWeapon());
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
