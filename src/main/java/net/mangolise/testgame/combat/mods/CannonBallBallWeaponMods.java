package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.CannonBallBallWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Consumer;

public sealed interface CannonBallBallWeaponMods extends Mod {
    record SplitCount(int level) implements CannonBallBallWeaponMods {

        public Component name() {
            return Component.text("Split Count").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.HEAVY_CORE, List.of("Cannon Ball Ball: +1 Split"), List.of());
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new CannonBallBallWeapon());
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(CannonBallBallWeapon.SPLIT_COUNT, count -> count + level());
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
}
