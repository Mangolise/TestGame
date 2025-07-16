package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.MaceWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Consumer;

public sealed interface MaceWeaponMods extends Mod {

    record SlamRadius(int level) implements MaceWeaponMods {
        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.MACE)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+1 Block Slam Range", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("-0.5 Damage", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                    )
                    .amount(1)
                    .build();
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            double slamRadius = 3.5 + level;
            attack.updateTag(MaceWeapon.SLAM_RADIUS, slamRad -> slamRad + slamRadius);
            attack.updateTag(Attack.DAMAGE, damage -> damage - 0.5);
            next.accept(attack);
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new MaceWeapon());
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record SlamIntensity(int level) implements MaceWeaponMods {
        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.FIRE_CHARGE)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+5 Slam Velocity", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("+1.5 Damage", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("+0.5 Cooldown", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                    )
                    .amount(1)
                    .build();
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new MaceWeapon());
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(MaceWeapon.SLAM_INTENSITY, slamIntensity -> slamIntensity + 5);
            attack.updateTag(Attack.DAMAGE, damage -> damage + 1.5);
            attack.updateTag(Attack.COOLDOWN, cooldown -> cooldown + 0.5);
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
}
