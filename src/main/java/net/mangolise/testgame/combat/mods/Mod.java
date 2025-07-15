package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.util.Utils;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public sealed interface Mod extends Attack.Node permits GenericMods, SnakeWeaponMods, StaffWeaponMods {

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

    default Component name() {
        return Component.text(this.getClass().getSimpleName()).color(this.rarity().color());
    }

    default List<Component> description() {
        return item().get(DataComponents.LORE);
    }

    Rarity rarity();
    ItemStack item();

    enum Rarity {
        COMMON,
        RARE,
        EPIC;
        
        public TextColor color() {
            return switch (this) {
                case COMMON -> TextColor.color(NamedTextColor.GRAY);
                case RARE -> TextColor.color(NamedTextColor.DARK_BLUE);
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
            try {
                Constructor<Mod> constructor = subClass.getDeclaredConstructor(int.class);
                factories.add((level) -> {
                    try {
                        return constructor.newInstance(level);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException("Failed to create instance of " + subClass.getName(), e);
                    }
                });
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to create factory for " + subClass.getName(), e);
            }
        }
        
        return factories;
    }
}
