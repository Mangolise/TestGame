package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.SnakeWeapon;
import net.mangolise.testgame.mobs.JacobEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.UnknownNullability;

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
            return ItemStack.builder(Material.RABBIT_FOOT)
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

    record Jacob(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.GOAT_SPAWN_EGG)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Jacob joins your side", NamedTextColor.GREEN),
                            Component.text("    Damage multiplier: 1.5 + (0.5 per level)", NamedTextColor.GREEN)
                    )
                    .build();
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            // still do the normal attack
            next.accept(attack);
            
            // Every time we attack, spawn a jacob entity with the same weapon
            double damageMultiplier = 1.5 + level * 0.5;
            Attack jacobAttack = attack.copy(true);
            jacobAttack.updateTag(Attack.DAMAGE, damage -> damage * damageMultiplier);
            var user = attack.getTag(Attack.USER);
            
            // TODO: get the user's weapon via a new Attack.WEAPON tag
            JacobEntity jacob = new JacobEntity(new SnakeWeapon(), attack);
            jacob.setInstance(user.getInstance(), user.getPosition());
        }

        @Override
        public double priority() {
            return PRIORITY_STAT_MODIFIER;
        }
    }

    // Health Mods --------------------------------------------------------------------------

    sealed interface IncreaseHealth extends GenericMods permits CommonIncreaseHealth, RareIncreaseHealth, EpicIncreaseHealth {
        @Override
        default void onAdd(Entity entity) {
            if (!(entity instanceof LivingEntity mob)) {
                return;
            }

            mob.getAttribute(Attribute.MAX_HEALTH).addModifier(new AttributeModifier(getId(), getHealthAmount(), AttributeOperation.ADD_VALUE));
            mob.setHealth(mob.getHealth() + (float) getHealthAmount());
        }

        String getId();
        double getHealthAmount();

        @Override
        default void onRemove(Entity entity) {
            if (!(entity instanceof LivingEntity mob)) {
                return;
            }

            mob.getAttribute(Attribute.MAX_HEALTH).removeModifier(Key.key(getId()));
            mob.setHealth(mob.getHealth() - (float) getHealthAmount());
        }

        @Override
        default double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record CommonIncreaseHealth(int level) implements IncreaseHealth {
        @Override
        public String getId() {
            return "common_max_health_mod";
        }

        @Override
        public double getHealthAmount() {
            return 2 * (level + 1);
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.APPLE)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Increase Health: +1.0 heart per level", NamedTextColor.GREEN)
                    )
                    .build();
        }
    }

    record RareIncreaseHealth(int level) implements IncreaseHealth {
        @Override
        public String getId() {
            return "rare_max_health_mod";
        }

        @Override
        public double getHealthAmount() {
            return 4 * (level + 1);
        }

        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.GOLDEN_APPLE)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Increase Health: +2.0 hearts per level", NamedTextColor.GREEN)
                    )
                    .build();
        }
    }

    record EpicIncreaseHealth(int level) implements IncreaseHealth {
        @Override
        public String getId() {
            return "epic_max_health_mod";
        }

        @Override
        public double getHealthAmount() {
            return 6 * (level + 1);
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.ENCHANTED_GOLDEN_APPLE)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Increase Health: +3.0 heart per level", NamedTextColor.GREEN)
                    )
                    .build();
        }
    }

    // Base Damage Mods --------------------------------------------------------------------------

    sealed interface IncreaseDamage extends GenericMods permits CommonIncreaseDamage, RareIncreaseDamage, EpicIncreaseDamage {
        @Override
        default void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.getAndUpdateTag(Attack.DAMAGE, damage -> damage + getDamageAmount());
            next.accept(attack);
        }

        double getDamageAmount();

        @Override
        default double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record CommonIncreaseDamage(int level) implements IncreaseDamage {
        @Override
        public double getDamageAmount() {
            return 2 * (level + 1);
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.IRON_INGOT)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Increase Damage: +1.0 damage per level", NamedTextColor.GREEN)
                    )
                    .build();
        }
    }

    record RareIncreaseDamage(int level) implements IncreaseDamage {
        @Override
        public double getDamageAmount() {
            return 4 * (level + 1);
        }

        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.DIAMOND)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Increase Damage: +2.0 damage per level", NamedTextColor.GREEN)
                    )
                    .build();
        }
    }

    record EpicIncreaseDamage(int level) implements IncreaseDamage {
        @Override
        public double getDamageAmount() {
            return 6 * (level + 1);
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.NETHERITE_INGOT)
                    .customName(this.name())
                    .lore(
                            Component.text("+ Increase Damage: +3.0 damage per level", NamedTextColor.GREEN)
                    )
                    .build();
        }
    }
}
