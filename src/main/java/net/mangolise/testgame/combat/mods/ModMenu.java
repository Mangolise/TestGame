package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;

public class ModMenu {

    public static Tag<Mod.Rarity> BUNDLE_RARITY = Tag.String("testgame.modmenu.rarity").map(Mod.Rarity::valueOf, Mod.Rarity::name);

    public static ItemStack createBundleItem() {
        double randomNum = Math.random();

        if (randomNum <= 0.2) {
            return ItemStack.builder(Material.PURPLE_BUNDLE)
                    .customName(Component.text("Epic Upgrade Box").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.LIGHT_PURPLE))
                    .maxStackSize(64)
                    .build()
                    .withTag(BUNDLE_RARITY, Mod.Rarity.EPIC);
        } else if (randomNum <= 0.5) {
            return ItemStack.builder(Material.BLUE_BUNDLE)
                    .customName(Component.text("Rare Upgrade Box").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_BLUE))
                    .maxStackSize(64)
                    .build()
                    .withTag(BUNDLE_RARITY, Mod.Rarity.RARE);
        }

        return ItemStack.builder(Material.LIGHT_GRAY_BUNDLE)
                .customName(Component.text("Common Upgrade Box").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GRAY))
                .maxStackSize(64)
                .build()
                .withTag(BUNDLE_RARITY, Mod.Rarity.COMMON);
    }

    public static void onItemUseEvent(PlayerUseItemEvent e) {
        if (!e.getItemStack().hasTag(BUNDLE_RARITY)) {
            return;
        }

        openModMenu(e.getPlayer(), e.getItemStack());
    }

    public static void openModMenu(Player player, ItemStack item) {
        Inventory inventory = new Inventory(InventoryType.CHEST_1_ROW, "Mods Menu");
        player.openInventory(inventory);
    }

    private static List<Mod> createRandomUpgrades(Inventory inventory, ItemStack item) {
        List<Mod> mods = new ArrayList<>();

        for (int i = 0; i < Mod.values().size(); i++) {
            if (Mod.values().get(i).create(0).rarity() == item.getTag(BUNDLE_RARITY)) {
                mods.add(Mod.values().get(i).create(0));
            }
        }

        return mods;
    }
}
