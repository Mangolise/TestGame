package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
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
        public Component name() {
            return Component.text("Slam Radius").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.MACE, List.of("Mace: +1 Block Slam Range"), List.of("Mace: -1.0 Damage"));
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(MaceWeapon.SLAM_RADIUS, slamRad -> slamRad + (1.0 + level));
            attack.updateTag(Attack.DAMAGE, damage -> damage - (1.0 + level));
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
        public Component name() {
            return Component.text("Slam Intensity").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.FIRE_CHARGE,
                    List.of("Mace: +5 Slam Velocity", "Mace: +1.0 Damage"),
                    List.of("Mace: +0.5s Cooldown"));
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new MaceWeapon());
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(MaceWeapon.SLAM_INTENSITY, slamIntensity -> slamIntensity + (5.0 + 5.0 * level));
            attack.updateTag(Attack.DAMAGE, damage -> damage + (1.0 + 1.0 * level));
            if (attack.weapon() instanceof MaceWeapon) {
                attack.updateTag(Attack.COOLDOWN, cooldown -> cooldown + (0.5 + 0.5 * level));
            }

            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record SwingRadius(int level) implements MaceWeaponMods {
        public Component name() {
            return Component.text("Swing Radius").color(this.rarity().color());
        }

        @Override
        public int maxLevel() {
            return 6;
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.FIREWORK_STAR,
                    List.of("Mace: +0.5 Blocks Swing Radius"),
                    List.of("Mace: -0.5 Damage"));
        }

        @Override
        public List<Weapon> getWeaponGrants() {
            return List.of(new MaceWeapon());
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(MaceWeapon.SWING_RADIUS, slamRad -> slamRad + (0.5 + 0.5 * level));
            attack.updateTag(Attack.DAMAGE, damage -> damage - (0.5 + 0.5 * level));
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
}
