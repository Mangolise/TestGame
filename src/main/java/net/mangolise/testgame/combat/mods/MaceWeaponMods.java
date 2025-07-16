package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                    .customName(this.name())
                    .lore(
                            Component.text("+ Slam Radius: +1 block per level", NamedTextColor.GREEN),
                            Component.text("- Damage: -0.5 damage", NamedTextColor.RED)
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
                    .customName(this.name())
                    .lore(
                            Component.text("+ Slam Intensity: +5 velocity per level", NamedTextColor.GREEN),
                            Component.text("+ Damage: +1.5 damage", NamedTextColor.GREEN),
                            Component.text("- Cooldown: +0.5 second increase", NamedTextColor.RED)
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
