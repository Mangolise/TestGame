package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
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
import java.util.Map;

public class ModMenu {

    public static Tag<Mod.Rarity> BUNDLE_RARITY = Tag.String("testgame.modmenu.rarity").map(Mod.Rarity::valueOf, Mod.Rarity::name);

    public static ItemStack createBundleItem() {
        double randomNum = Math.random();

        if (randomNum <= 0.2) {
            var rarity = Mod.Rarity.EPIC;
            return ItemStack.builder(Material.PURPLE_BUNDLE)
                    .customName(Component.text("Epic Upgrade Box").decoration(TextDecoration.ITALIC, false).color(rarity.color()))
                    .maxStackSize(64)
                    .build()
                    .withTag(BUNDLE_RARITY, rarity);
        } else if (randomNum <= 0.5) {
            var rarity = Mod.Rarity.RARE;
            return ItemStack.builder(Material.BLUE_BUNDLE)
                    .customName(Component.text("Rare Upgrade Box").decoration(TextDecoration.ITALIC, false).color(rarity.color()))
                    .maxStackSize(64)
                    .build()
                    .withTag(BUNDLE_RARITY, Mod.Rarity.RARE);
        }

        var rarity = Mod.Rarity.COMMON;
        return ItemStack.builder(Material.LIGHT_GRAY_BUNDLE)
                .customName(Component.text("Common Upgrade Box").decoration(TextDecoration.ITALIC, false).color(rarity.color()))
                .maxStackSize(64)
                .build()
                .withTag(BUNDLE_RARITY, rarity);
    }

    public static void onItemUseEvent(PlayerUseItemEvent e) {
        if (!e.getItemStack().hasTag(BUNDLE_RARITY)) {
            return;
        }

        openModMenu(e.getPlayer(), e.getItemStack());
    }

    public static void openModMenu(Player player, ItemStack item) {
        Inventory inventory = new Inventory(InventoryType.CHEST_1_ROW, "Mods Menu");

        List<Mod> selectedMods = new ArrayList<>();

        while (selectedMods.size() < 3) {
            Mod mod = sampleUpgrade(item);

            if (!selectedMods.contains(mod)) {
                selectedMods.add(mod);
            }
        }

        for (Mod selectedMod : selectedMods) {
            inventory.addItemStack(selectedMod.item());
        }

        player.openInventory(inventory);
    }

    private static final Map<Mod.Rarity, Map<Mod.Rarity, Number>> rarityToRarityDropChance = Map.ofEntries(
            Map.entry(Mod.Rarity.COMMON, Map.of(
                    Mod.Rarity.COMMON, 90,
                    Mod.Rarity.RARE, 10,
                    Mod.Rarity.EPIC, 1
            )),
            Map.entry(Mod.Rarity.RARE, Map.of(
                    Mod.Rarity.COMMON, 10,
                    Mod.Rarity.RARE, 85,
                    Mod.Rarity.EPIC, 5
            )),
            Map.entry(Mod.Rarity.EPIC, Map.of(
                    Mod.Rarity.COMMON, 5,
                    Mod.Rarity.RARE, 15,
                    Mod.Rarity.EPIC, 80
            ))
    );

    private static Mod sampleUpgrade(ItemStack item) {
        Map<Mod.Rarity, Number> rarityChance = rarityToRarityDropChance.get(item.getTag(BUNDLE_RARITY));
        double sampleValue = (Math.random() * rarityChance.values().stream().mapToDouble(Number::doubleValue).sum());

        Mod.Rarity rarity = Mod.Rarity.COMMON;

        for (Map.Entry<Mod.Rarity, Number> entry : rarityChance.entrySet()) {
            if (sampleValue < entry.getValue().doubleValue()) {
                rarity = entry.getKey();
                break;
            }

            sampleValue -= entry.getValue().doubleValue();
        }

        List<Mod> mods = new ArrayList<>();

        for (Mod.Factory value : Mod.values()) {
            if (value.create(0).rarity() == rarity) {
                mods.add(value.create(1));
            }
        }

        double random = Math.random() * mods.size();
        return mods.get((int) random);
    }
}
