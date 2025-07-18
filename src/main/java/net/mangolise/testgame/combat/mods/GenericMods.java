package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.MaceWeapon;
import net.mangolise.testgame.mobs.JacobEntity;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.PotionContents;
import net.minestom.server.potion.PotionType;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

sealed public interface GenericMods extends Mod {

    record DoubleAttack(int level) implements GenericMods {

        public Component name() {
            return Component.text("Double Attack").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public int maxLevel() {
            return 0;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.NETHERITE_SWORD, List.of("2x Attacks"), List.of("-50% Damage"));
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            attack.updateTag(Attack.DAMAGE, damage -> damage * 0.5);

            // attack twice
            next.accept(attack);
            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_MULTIPLICATIVE_MODIFIER;
        }
    }

    // TODO: decide whether to keep this, It's really broken having more than 1 or having more than 2 attacks.
//    record TripleAttack(int level) implements GenericMods {
//        @Override
//        public Rarity rarity() {
//            return Rarity.RARE;
//        }
//
//        @Override
//        public ItemStack item() {
//            return ItemStack.builder(Material.DIAMOND_SWORD)
//                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
//                    .lore(
//                            Component.text("3x Attacks", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
//                            Component.text("-0.3 + (0.1 per level)", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
//                    )
//                    .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(List.of()))
//                    .amount(3)
//                    .maxStackSize(3).build();
//        }
//
//        @Override
//        public void attack(Attack attack, Consumer<Attack> next) {
//            attack.updateTag(Attack.DAMAGE, damage -> damage * (0.33 + level * 0.1));
//
//            // attack three times
//            next.accept(attack);
//            next.accept(attack);
//            next.accept(attack);
//        }
//
//        @Override
//        public double priority() {
//            return PRIORITY_WEAPON + 0.2; // this is essentially a weapon
//        }
//    }
//
//    record QuadAttack(int level) implements GenericMods {
//        @Override
//        public Rarity rarity() {
//            return Rarity.EPIC;
//        }
//
//        @Override
//        public ItemStack item() {
//            return ItemStack.builder(Material.NETHERITE_SWORD)
//                    .customName(this.name().decoration(TextDecoration.ITALIC, false))
//                    .lore(
//                            Component.text("4x Attacks", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
//                            Component.text("-0.25 + (0.1 per level)", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
//                    )
//                    .set(DataComponents.ATTRIBUTE_MODIFIERS, new AttributeList(List.of()))
//                    .amount(4)
//                    .maxStackSize(4).build();
//        }
//
//        @Override
//        public void attack(Attack attack, Consumer<Attack> next) {
//            attack.updateTag(Attack.DAMAGE, damage -> damage * (0.25 + level * 0.1));
//
//            // attack four times
//            next.accept(attack);
//            next.accept(attack);
//            next.accept(attack);
//            next.accept(attack);
//        }
//
//        @Override
//        public double priority() {
//            return PRIORITY_WEAPON + 0.3; // this is essentially a weapon
//        }
//    }

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
        public Component name() {
            return Component.text("Lucky Leonard").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.GOLD_INGOT, List.of("+5% Crit Chance"), List.of());
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            attack.updateTag(Attack.CRIT_CHANCE, crit -> crit + (5.0 + level * 5.0));

            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
    
    record QuickHands(int level) implements GenericMods {
        public Component name() {
            return Component.text("Quick Hands").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.RABBIT_FOOT, List.of("+5% Cooldown Reduction"), List.of());
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            double cooldownReduction = 0.05 + level * 0.05;
            attack.updateTag(Attack.COOLDOWN, cooldown -> cooldown * (1.0 - cooldownReduction));

            next.accept(attack);
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record CoinFlip(int level) implements GenericMods {
        public Component name() {
            return Component.text("Coin Flip").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.GOLD_NUGGET,
                    List.of("2.5 + (0.5 per level)x Damage"),
                    List.of("50% - (10% per level) Chance to do Nothing"));
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
        public Component name() {
            return Component.text("Jacob").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.GOAT_SPAWN_EGG,
                    List.of("Jacob joins your side", "    1.5 + (0.5 per level) Damage", "    2 + (1 per level) Seconds to live"),
                    List.of());
        }

        @Override
        public void attack(Attack attack, Consumer<Attack> next) {
            // still do the normal attack
            next.accept(attack);
            
            if (attack.sampleCrits() == 0) {
                return; // no crits, no jacob
            }

            // Every time we crit, spawn a jacob entity with the same weapon
            double damageMultiplier = 1.5 + level * 0.5;
            Attack jacobAttack = attack.copy(true);
            jacobAttack.updateTag(Attack.DAMAGE, damage -> damage * damageMultiplier);
            var user = attack.getTag(Attack.USER);
            
            // TODO: get the user's weapon via a new Attack.WEAPON tag
            JacobEntity jacob = new JacobEntity(new MaceWeapon(), attack, 20 + (level) * 10);
            jacob.setInstance(user.getInstance(), user.getPosition());
        }

        @Override
        public double priority() {
            return PRIORITY_POST_MODIFIER;
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
            mob.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier(getId(), -0.0015 * (level() + 1.0), AttributeOperation.ADD_VALUE));
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
            mob.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(Key.key(getId()));
            mob.setHealth(mob.getHealth() - (float) getHealthAmount());
        }

        @Override
        default double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record CommonIncreaseHealth(int level) implements IncreaseHealth {
        public Component name() {
            return Component.text("Health Increase").color(this.rarity().color());
        }

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
            return createItem(Material.APPLE,
                    List.of("+1.0 Heart"),
                    List.of("+5% Slowness"));
        }
    }

    record RareIncreaseHealth(int level) implements IncreaseHealth {
        public Component name() {
            return Component.text("Rare Health Increase").color(this.rarity().color());
        }

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
            return createItem(Material.GOLDEN_APPLE,
                    List.of("+2.0 Hearts"),
                    List.of("+5% Slowness"));
        }
    }

    record EpicIncreaseHealth(int level) implements IncreaseHealth {
        public Component name() {
            return Component.text("Epic Health Increase").color(this.rarity().color());
        }

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
            return createItem(Material.ENCHANTED_GOLDEN_APPLE,
                    List.of("+4.0 Hearts"),
                    List.of("+5% Slowness"));
        }
    }

    // Base Damage Mods --------------------------------------------------------------------------

    sealed interface IncreaseDamage extends GenericMods permits CommonIncreaseDamage, RareIncreaseDamage, EpicIncreaseDamage {
        @Override
        default void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
            attack.getAndUpdateTag(Attack.DAMAGE, damage -> damage + getDamageAmount());
            attack.getAndUpdateTag(Attack.COOLDOWN, coolDown -> coolDown * getCooldownAmount());
            next.accept(attack);
        }

        double getDamageAmount();
        double getCooldownAmount();

        @Override
        default double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record CommonIncreaseDamage(int level) implements IncreaseDamage {
        public Component name() {
            return Component.text("Damage Increase").color(this.rarity().color());
        }

        @Override
        public double getDamageAmount() {
            return 2 * (level + 1);
        }

        @Override
        public double getCooldownAmount() {
            return 1.0 - (0.05 * (level + 1.0));
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
            return createItem(Material.IRON_INGOT,
                    List.of("+1.0 Damage"),
                    List.of("+5% Weapon Cooldown"));
        }
    }

    record RareIncreaseDamage(int level) implements IncreaseDamage {
        public Component name() {
            return Component.text("Rare Damage Increase").color(this.rarity().color());
        }

        @Override
        public double getDamageAmount() {
            return 4 * (level + 1);
        }

        @Override
        public double getCooldownAmount() {
            return 1.0 - (0.05 * (level + 1.0));
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
            return createItem(Material.DIAMOND,
                    List.of("+2.0 Damage"),
                    List.of("+5% Weapon Cooldown"));
        }
    }

    record EpicIncreaseDamage(int level) implements IncreaseDamage {
        public Component name() {
            return Component.text("Epic Damage Increase").color(this.rarity().color());
        }

        @Override
        public double getDamageAmount() {
            return 8 * (level + 1);
        }

        @Override
        public double getCooldownAmount() {
            return 1.0 - (0.1 * (level + 1.0));
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
            return createItem(Material.NETHERITE_INGOT,
                    List.of("+4.0 Damage"),
                    List.of("+10% Weapon Cooldown"));
        }
    }

    // -------------------------------------------------------------------------------------------

    record GlassCannon(int level) implements GenericMods {
        public Component name() {
            return Component.text("Glass Cannon").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.EPIC;
        }

        @Override
        public int maxLevel() {
            return 0;
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
            return createItem(Material.POTION,
                    List.of("+50% Damage"),
                    List.of("-50% Max Health"))
                    .with(DataComponents.POTION_CONTENTS, new PotionContents(PotionType.STRENGTH));
        }

        @Override
        public double priority() {
            return PRIORITY_MULTIPLICATIVE_MODIFIER;
        }
    }

    record GamblersDice(int level) implements GenericMods {
        public Component name() {
            return Component.text("Gamblers Dice").color(this.rarity().color());
        }

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
            return createItem(Material.RABBIT_HIDE,
                    List.of("20% chance to deal 300% of current damage"),
                    List.of("20% chance to deal 30% of current damage"),
                    List.of("Rolls chances per attack"));
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

    record Agility(int level) implements GenericMods {
        public Component name() {
            return Component.text("Agility").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public int maxLevel() {
            return 6;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.SUGAR,
                    List.of("+5% Speed"),
                    List.of("-1.0 Heart"));
        }

        @Override
        public void onAdd(Entity entity) {
            if (!(entity instanceof LivingEntity mob)) {
                return;
            }

            mob.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier("speed_mod", 0.005, AttributeOperation.ADD_VALUE));
        }

        @Override
        public void onRemove(Entity entity) {
            if (!(entity instanceof LivingEntity mob)) {
                return;
            }

            mob.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(Key.key("speed_mod"));
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }

    record Acrobatics(int level) implements GenericMods {
        public Component name() {
            return Component.text("Acrobatics").color(this.rarity().color());
        }

        @Override
        public Rarity rarity() {
            return Rarity.COMMON;
        }

        @Override
        public ItemStack item() {
            return createItem(Material.GLISTERING_MELON_SLICE,
                    List.of("+5% Jump Boost"),
                    List.of());
        }

        @Override
        public void onAdd(Entity entity) {
            if (!(entity instanceof LivingEntity mob)) {
                return;
            }

            mob.getAttribute(Attribute.JUMP_STRENGTH).addModifier(new AttributeModifier("jump_boost", 0.005, AttributeOperation.ADD_VALUE));
        }

        @Override
        public void onRemove(Entity entity) {
            if (!(entity instanceof LivingEntity mob)) {
                return;
            }

            mob.getAttribute(Attribute.JUMP_STRENGTH).removeModifier(Key.key("jump_boost"));
        }

        @Override
        public double priority() {
            return PRIORITY_ADDITIVE_MODIFIER;
        }
    }
}
