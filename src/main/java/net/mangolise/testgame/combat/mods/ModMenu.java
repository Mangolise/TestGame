package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

public class ModMenu {

    public static Tag<Mod.Rarity> BUNDLE_RARITY = Tag.String("testgame.modmenu.rarity").map(Mod.Rarity::valueOf, Mod.Rarity::name);

    public static ItemStack createBundleItem() {
        double randomNum = Math.random();

        if (randomNum <= 0.1) {
            return ItemStack.builder(Material.PURPLE_BUNDLE)
                    .customName(Component.text("Epic Upgrade Box").color(NamedTextColor.LIGHT_PURPLE))
                    .build()
                    .withTag(BUNDLE_RARITY, Mod.Rarity.EPIC);
        } else if (randomNum <= 0.4) {
            return ItemStack.builder(Material.BLUE_BUNDLE)
                    .customName(Component.text("Rare Upgrade Box").color(NamedTextColor.DARK_BLUE))
                    .build()
                    .withTag(BUNDLE_RARITY, Mod.Rarity.RARE);
        }

        return ItemStack.builder(Material.LIGHT_GRAY_BUNDLE)
                .customName(Component.text("Common Upgrade Box").color(NamedTextColor.GRAY))
                .build()
                .withTag(BUNDLE_RARITY, Mod.Rarity.COMMON);
    }

    public static void openModMenu(Player player) {
        Inventory inventory = new Inventory(InventoryType.CHEST_1_ROW, "Mods Menu");
        createRandomUpgrades(inventory);
        player.openInventory(inventory);
    }

    private static void createRandomUpgrades(Inventory inventory) {

    }
}
