package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.HEAVY_CORE)
                    .customName(name())
                    .lore(
                            Component.text("CannonBallBall: ", NamedTextColor.GRAY).append(Component.text("+1 Split", NamedTextColor.GREEN))
                    )
                    .hideExtraTooltip()
                    .build();
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
