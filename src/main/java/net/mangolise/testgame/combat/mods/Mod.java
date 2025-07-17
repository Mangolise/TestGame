package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.mangolise.testgame.util.Utils;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.TooltipDisplay;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public sealed interface Mod extends Attack.Node permits CannonBallBallWeaponMods, GenericMods, MaceWeaponMods, SnakeWeaponMods, StaffWeaponMods {

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

    default List<Weapon> getWeaponGrants() {
        return List.of();
    }

    /**
     * The maximum level this mod can be upgraded to
     * @return the maximum level
     */
    default int maxLevel() {
        return 3;
    }

    default Component name() {
        return Component.text(this.getClass().getSimpleName()).color(this.rarity().color());
    }

    default List<Component> description() {
        return item().get(DataComponents.LORE);
    }

    default void onAdd(Entity entity) {}
    default void onRemove(Entity entity) {}

    Rarity rarity();
    ItemStack item();
    int level();

    default ItemStack createItem(Material material, List<String> positives, List<String> negatives) {
        return createItem(material, positives, negatives, List.of());
    }

    default ItemStack createItem(Material material, List<String> positives, List<String> negatives, List<String> info) {
        var builder = ItemStack.builder(material)
                .customName(this.name().decoration(TextDecoration.ITALIC, false).decorate(TextDecoration.BOLD))
                .set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, Set.of(DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.DAMAGE)))
                .amount(1);

        List<Component> lore = new ArrayList<>();
        for (String positive : positives) {
            lore.add(Component.text(positive, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        for (String negative : negatives) {
            lore.add(Component.text(negative, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        for (String infoLine : info) {
            lore.add(Component.text(infoLine, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        builder.lore(lore);
        return builder.build();
    }

    enum Rarity {
        COMMON,
        RARE,
        EPIC;
        
        public TextColor color() {
            return switch (this) {
                case COMMON -> TextColor.color(NamedTextColor.GRAY);
                case RARE -> TextColor.color(TextColor.color(65, 118, 216));
                case EPIC -> TextColor.color(NamedTextColor.LIGHT_PURPLE);
            };
        }
    }

    interface Factory {
        Mod create(int level);
    }

    static List<Mod.Factory> values() {
        List<Class<Mod>> subClasses = Utils.getAllRecordSubclasses(Mod.class);
        List<Mod.Factory> factories = new ArrayList<>();
        
        for (Class<Mod> subClass : subClasses) {
            factories.add(getFactory(subClass));
        }
        return factories;
    }

    static Mod.Factory getFactory(Class<? extends Mod> modClass) {
        try {
            Constructor<? extends Mod> constructor = modClass.getDeclaredConstructor(int.class);
            return (level) -> {
                try {
                    return constructor.newInstance(level);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to create instance of " + modClass.getName(), e);
                }
            };
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create factory for " + modClass.getName(), e);
        }
    }
}
