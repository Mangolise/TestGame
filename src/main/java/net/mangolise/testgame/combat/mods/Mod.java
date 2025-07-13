package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.BowWeapon;
import net.mangolise.testgame.combat.weapons.StaffWeapon;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

public sealed interface Mod extends Attack.Node {

    /**
     * The scaling factor for the experience needed to upgrade this mod
     * It's calculated as:
     * <pre>
     *     upgradeExpBase * max(1, pow(level, upgradeExpScaling))
     * </pre>
     */
    default double upgradeExpScaling() {
        return 1.5;
    }

    /**
     * The base experience needed to upgrade this mod
     * It's calculated as:
     * <pre>
     *     upgradeExpBase * max(1, pow(level, upgradeExpScaling))
     * </pre>
     */
    default double upgradeExpBase() {
        return 512.0;
    }

    /**
     * The maximum level this mod can be upgraded to
     * @return the maximum level
     */
    default int maxLevel() {
        return 3;
    }
    
    record DoubleAttack(int level) implements Mod {

        @Override
        public Component description() {
            return Component.text()
                    .append(Component.text("+ Attacks twice", NamedTextColor.GREEN))
                    .append(Component.text("- Damage multiplier: 0.5 + (0.1 per level)", NamedTextColor.RED))
                    .build();
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            attack.updateTag(Attack.DAMAGE, damage -> damage * (0.5 + level * 0.1));
            
            // attack twice
            next.accept(attack);
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_WEAPON + 0.1; // this is essentially a weapon
        }
    }
    
    record TripleAttack(int level) implements Mod {

        @Override
        public Component description() {
            return Component.text()
                    .append(Component.text("+ Attacks three times", NamedTextColor.GREEN))
                    .append(Component.text("- Damage multiplier: 0.33 + (0.1 per level)", NamedTextColor.RED))
                    .build();
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            attack.updateTag(Attack.DAMAGE, damage -> damage * (0.33 + level * 0.1));
            
            // attack three times
            next.accept(attack);
            next.accept(attack);
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_WEAPON + 0.2; // this is essentially a weapon
        }
    }
    
    record QuadAttack(int level) implements Mod {

        @Override
        public Component description() {
            return Component.text()
                    .append(Component.text("+ Attacks four times", NamedTextColor.GREEN))
                    .append(Component.text("- Damage multiplier: 0.25 + (0.1 per level)", NamedTextColor.RED))
                    .build();
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            attack.updateTag(Attack.DAMAGE, damage -> damage * (0.25 + level * 0.1));

            // attack four times
            next.accept(attack);
            next.accept(attack);
            next.accept(attack);
            next.accept(attack);
        }
        
        @Override
        public double priority() {
            return PRIORITY_WEAPON + 0.3; // this is essentially a weapon
        }
    }
    
    record BowVelocity(int level) implements Mod {

        @Override
        public Component description() {
            return Component.text()
                    .append(Component.text("+ Multiplies bow velocity", NamedTextColor.GREEN))
                    .append(Component.text("    Velocity multiplier: 2.0 + (1.0 per level)", NamedTextColor.RED))
                    .build();
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            double velocity = 2.0 + (1.0 * level);
            attack.updateTag(BowWeapon.VELOCITY, velo -> velo * velocity);
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_STAT_MODIFIER;
        }
    }

    record StaffVelocity(int level) implements Mod {

        @Override
        public Component description() {
            return Component.text()
                    .append(Component.text("+ Multiplies spell velocity", NamedTextColor.GREEN))
                    .append(Component.text("    Velocity multiplier: 2.0 + (1.0 per level)", NamedTextColor.RED))
                    .build();
        }

        @Override
        public void attack(Attack tags, @UnknownNullability Consumer<Attack> next) {
            double velocity = 2.0 + (1.0 * level);
            tags.updateTag(StaffWeapon.VELOCITY, velo -> velo * velocity);
            next.accept(tags);
        }

        @Override
        public double priority() {
            return PRIORITY_STAT_MODIFIER;
        }
    }

    record StaffExplosionSize(int level) implements Mod {

        @Override
        public Component description() {
            return Component.text()
                    .append(Component.text("+ Adds to the Staffs Explosion Size", NamedTextColor.GREEN))
                    .append(Component.text("    Explosion Addition: 1.0 + (1.0 per level)", NamedTextColor.RED))
                    .build();
        }

        @Override
        public void attack(Attack tags, @UnknownNullability Consumer<Attack> next) {
            double explosionSize = 1.0 + level;
            tags.updateTag(StaffWeapon.EXPLOSION_SIZE, exploSize -> exploSize + explosionSize);
            next.accept(tags);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
    
    record CritToDamage(int level) implements Mod {

        @Override
        public Component description() {
            return Component.text()
                    .append(Component.text("+ Converts crit chance into damage", NamedTextColor.GREEN))
                    .append(Component.text("    Crit chance: 0.5 + (0.1 per level)", NamedTextColor.RED))
                    .build();
        }
    
        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            double critChance = attack.getTag(Attack.CRIT_CHANCE);
            double critDamage = critChance * (0.5 + level * 0.1);
            attack.updateTag(Attack.DAMAGE, damage -> damage + critDamage);
            attack.setTag(Attack.CRIT_CHANCE, 0.0); // remove crit chance

            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_STAT_MODIFIER;
        }
    }

    default Component name() {
        return Component.text(this.getClass().getSimpleName());
    }

    Component description();
}
