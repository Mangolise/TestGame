package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.SnakeWeapon;
import net.mangolise.testgame.mobs.JacobEntity;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.AttributeList;
import net.minestom.server.item.component.PotionContents;
import net.minestom.server.item.component.TooltipDisplay;
import net.minestom.server.potion.PotionType;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.Set;
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("2x Attacks", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("-0.5 + (0.1 per level) damage", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                    )
                    .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(List.of()))
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("3x Attacks", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("-0.3 + (0.1 per level)", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                    )
                    .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(List.of()))
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("4x Attacks", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("-0.25 + (0.1 per level)", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                    )
                    .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(List.of()))
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

    // TODO: decide whether to keep this, It's often detrimental and is confusing to explain.
//    record CritToDamage(int level) implements GenericMods {
//        @Override
//        public Rarity rarity() {
//            return Rarity.COMMON;
//        }
//
//        @Override
//        public ItemStack item() {
//            return ItemStack.builder(Material.AMETHYST_SHARD)
//                    .customName(this.name())
//                    .lore(
//                            Component.text("+ Converts crit chance into damage", NamedTextColor.GREEN),
//                            Component.text("    Crit chance: 0.5 + (0.1 per level)", NamedTextColor.GREEN)
//                    )
//                    .build();
//        }
//
//        @Override
//        public void attack(Attack attack, Consumer<Attack> next) {
//            double critChance = attack.getTag(Attack.CRIT_CHANCE);
//            double critDamage = critChance * (0.5 + level * 0.1);
//            attack.updateTag(Attack.DAMAGE, damage -> damage + critDamage);
//            attack.setTag(Attack.CRIT_CHANCE, 0.0); // remove crit chance
//
//            next.accept(attack);
//        }
//
//        @Override
//        public double priority() {
//            return PRIORITY_POST_MODIFIER;
//        }
//    }
    
    record LuckyLeonard(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.GOLD_INGOT)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+20% + (5% per level) Crit chance", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("30% + (10% per level) Cooldown Reduction", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("2.5 + (0.5 per level)x Damage", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("50% - (10% per level) Chance to do Nothing", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("Jacob joins your side", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("    1.5 + (0.5 per level) Damage", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
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
        public int maxLevel() {
            return 6;
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+1.0 Heart", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
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
        public int maxLevel() {
            return 4;
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
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+2.0 Hearts", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
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
            return 8 * (level + 1);
        }

        @Override
        public int maxLevel() {
            return 2;
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.ENCHANTED_GOLDEN_APPLE)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+4.0 Hearts", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
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
        public int maxLevel() {
            return 6;
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.IRON_INGOT)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+1.0 Damage", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
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
        public int maxLevel() {
            return 4;
        }

        @Override
        public Rarity rarity() {
            return Rarity.RARE;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.DIAMOND)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+2.0 Damage", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                    )
                    .build();
        }
    }

    record EpicIncreaseDamage(int level) implements IncreaseDamage {
        @Override
        public double getDamageAmount() {
            return 8 * (level + 1);
        }

        @Override
        public int maxLevel() {
            return 2;
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.NETHERITE_INGOT)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+4.0 Damage", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                    )
                    .build();
        }
    }

    // -------------------------------------------------------------------------------------------

    record GlassCannon(int level) implements GenericMods {
        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public int maxLevel() {
            return 100;
        }

        @Override
        public void onAdd(Entity entity) {
            if (!(entity instanceof LivingEntity mob)) {
                return;
            }

            mob.getAttribute(Attribute.MAX_HEALTH).addModifier(new AttributeModifier("glass_cannon_mod",
                    (0.5 / (level + 1.0)) - 1.0, AttributeOperation.ADD_MULTIPLIED_TOTAL)
            );
        }

        @Override
        public void onRemove(Entity entity) {
            if (!(entity instanceof LivingEntity mob)) {
                return;
            }

            mob.getAttribute(Attribute.MAX_HEALTH).removeModifier(Key.key("glass_cannon_mod"));
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.updateTag(Attack.DAMAGE, damage -> damage * (1.5 * (level + 1.0)));
            next.accept(attack);
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.POTION)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("+50% damage", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("-50% max health", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                    )
                    .set(DataComponents.POTION_CONTENTS, new PotionContents(PotionType.STRENGTH))
                    .hideExtraTooltip()
                    .build();
        }

        @Override
        public double priority() {
            return PRIORITY_MULTIPLICATIVE_MODIFIER;
        }
    }

    record GamblersDice(int level) implements GenericMods {
        @Override
        public int maxLevel() {
            return 1;
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return ItemStack.builder(Material.RABBIT_HIDE)
                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
                    .lore(
                            Component.text("20% chance to deal 300% of current damage", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                            Component.text("20% chance to deal 30% of current damage", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                            Component.text("Rolls chances per attack", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    )
                    .hideExtraTooltip()
                    .build();
        }

        @Override
        public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            double random = Math.random();

            if (random <= 0.2) {
                attack.updateTag(Attack.DAMAGE, damage -> damage * 3);
            } else if (random <= 0.4) {
                attack.updateTag(Attack.DAMAGE, damage -> damage * 0.3);
            }

            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_MULTIPLICATIVE_MODIFIER;
        }
    }
}
