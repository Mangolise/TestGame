package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.testgame.combat.Attack;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

sealed public interface GenericMods extends Mod {

    record DoubleAttack(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.IRON_SWORD)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Attacks twice", NamedTextColor.GREEN),
                            Component.text("- Damage multiplier: 0.5 + (0.1 per level)", NamedTextColor.RED)
                    )
                    .amount(2)
                    .maxStackSize(2).build();
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

    record TripleAttack(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.DIAMOND_SWORD)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Attacks three times", NamedTextColor.GREEN),
                            Component.text("- Damage multiplier: 0.33 + (0.1 per level)", NamedTextColor.RED)
                    )
                    .amount(3)
                    .maxStackSize(3).build();
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

    record QuadAttack(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.NETHERITE_SWORD)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Attacks four times", NamedTextColor.GREEN),
                            Component.text("- Damage multiplier: 0.25 + (0.1 per level)", NamedTextColor.RED)
                    )
                    .amount(4)
                    .maxStackSize(4).build();
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

    record CritToDamage(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.AMETHYST_SHARD)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Converts crit chance into damage", NamedTextColor.GREEN),
                            Component.text("    Crit chance: 0.5 + (0.1 per level)", NamedTextColor.GREEN)
                    )
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
            return PRIORITY_POST_MODIFIER;
        }
    }
    
    record LuckyLeonard(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.GOLD_INGOT)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Adds to crit chance", NamedTextColor.GREEN),
                            Component.text("    Added crit chance: 20% + (5% per level)", NamedTextColor.GREEN)
                    )
                    .build();
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            attack.updateTag(Attack.CRIT_CHANCE, crit -> crit + (0.2 + level * 0.05));

            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
    
    record QuickHands(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.LEATHER_HORSE_ARMOR)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Reduces cooldown", NamedTextColor.GREEN),
                            Component.text("    Cooldown reduction: 30% + (10% per level)", NamedTextColor.GREEN)
                    )
                    .build();
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            double cooldownReduction = 0.3 + level * 0.1;
            attack.updateTag(Attack.COOLDOWN, cooldown -> cooldown * (1.0 - cooldownReduction));

            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record CoinFlip(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.GOLD_NUGGET)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Deals multiple times more damage", NamedTextColor.GREEN),
                            Component.text("    Damage multiplier: 2.5 + (0.5 per level)", NamedTextColor.GREEN),
                            Component.text("- Sometimes does nothing", NamedTextColor.RED),
                            Component.text("    Chance: 50% - (10% per level)", NamedTextColor.RED)
                    )
                    .build();
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            double damageMultiplier = 2.5 + level * 0.5;
            double chance = 0.5 - level * 0.1;

            double testValue = ThreadLocalRandom.current().nextDouble();
            
            if (testValue < chance) {
                if (attack.getTag(Attack.USER) instanceof Player player) {
                    // play sound
                    player.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_BREAK.key(), Sound.Source.PLAYER, 0.5f, 1.0f));
                }
            } else {
                // apply damage multiplier
                attack.updateTag(Attack.DAMAGE, damage -> damage * damageMultiplier);
                next.accept(attack);
            }
        }

        @Override
        public double priority() {
            return PRIORITY_STAT_MODIFIER;
        }
    }
}
