package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.weapons.Weapon;
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

    Rarity rarity();
    ItemStack item();
    int level();

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
