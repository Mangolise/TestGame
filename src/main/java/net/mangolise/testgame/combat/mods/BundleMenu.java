package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jspecify.annotations.Nullable;

import java.util.*;

// TODO: maybe possibly make the messages have a hover for item tooltips and a click that opens ModMenu
public class BundleMenu {
    public static final Tag<Mod.Rarity> BUNDLE_RARITY = Tag.String("testgame.bundlemenu.rarity").map(Mod.Rarity::valueOf, Mod.Rarity::name);
    public static final Tag<Boolean> IS_BARRIER = Tag.Boolean("testgame.bundlemenu.is_barrier").defaultValue(false);
    public static final Tag<Boolean> IS_WEAPON_BUNDLE = Tag.Boolean("testgame.bundlemenu.is_weapon_bundle").defaultValue(false);
    public static final Tag<Mod> ITEM_MOD = Tag.Component("testgame.bundlemenu.item_mod").map(c -> Mod.values().stream().filter(a -> a.create(0).name().equals(c)).findAny().get().create(0), Mod::name);
    public static final Tag<Boolean> BUNDLE_INVENTORY = Tag.Boolean("testgame.bundlemenu.is_bundle_inventory").defaultValue(false);
    public static final Tag<OpenBundleInfo> OPEN_BUNDLE_MENU = Tag.Transient("testgame.bundlemenu.inv_rarity");

    public static final Map<Mod.Rarity, Tag<Inventory>> LAST_MOD_SELECTION = Map.of(
            Mod.Rarity.COMMON, Tag.Transient("testgame.bundlemenu.last_mod_selection.common"),
            Mod.Rarity.RARE  , Tag.Transient("testgame.bundlemenu.last_mod_selection.rare"),
            Mod.Rarity.EPIC  , Tag.Transient("testgame.bundlemenu.last_mod_selection.epic")
    );

    public static final Tag<Inventory> LAST_MOD_SELECTION_WEAPON = Tag.Transient("testgame.bundlemenu.last_mod_selection.weapon");

    public record OpenBundleInfo(Mod.Rarity rarity, boolean isWeaponBundle, PlayerHand hand) { }

    public static ItemStack createBundleItem(boolean isWeaponBundle) {
        if (isWeaponBundle) {
            var rarity = Mod.Rarity.EPIC;
            return ItemStack.builder(Material.PURPLE_BUNDLE)
                    .customName(Component.text("Epic Weapon Box").decoration(TextDecoration.ITALIC, false).color(rarity.color()))
                    .maxStackSize(64)
                    .build()
                    .withTag(IS_WEAPON_BUNDLE, true)
                    .withTag(BUNDLE_RARITY, rarity);
        }

        double randomNum = Math.random();

        if (randomNum <= 0.1) {
            var rarity = Mod.Rarity.EPIC;
            return ItemStack.builder(Material.PURPLE_BUNDLE)
                    .customName(Component.text("Epic Upgrade Box").decoration(TextDecoration.ITALIC, false).color(rarity.color()))
                    .maxStackSize(64)
                    .build()
                    .withTag(IS_WEAPON_BUNDLE, false)
                    .withTag(BUNDLE_RARITY, rarity);
        } else if (randomNum <= 0.3) {
            var rarity = Mod.Rarity.RARE;
            return ItemStack.builder(Material.BLUE_BUNDLE)
                    .customName(Component.text("Rare Upgrade Box").decoration(TextDecoration.ITALIC, false).color(rarity.color()))
                    .maxStackSize(64)
                    .build()
                    .withTag(IS_WEAPON_BUNDLE, false)
                    .withTag(BUNDLE_RARITY, rarity);
        }

        var rarity = Mod.Rarity.COMMON;
        return ItemStack.builder(Material.LIGHT_GRAY_BUNDLE)
                .customName(Component.text("Common Upgrade Box").decoration(TextDecoration.ITALIC, false).color(rarity.color()))
                .maxStackSize(64)
                .build()
                .withTag(IS_WEAPON_BUNDLE, false)
                .withTag(BUNDLE_RARITY, rarity);
    }

    public static void onItemUseEvent(PlayerUseItemEvent e) {
        if (!e.getItemStack().hasTag(BUNDLE_RARITY)) {
            return;
        }

        openBundleMenu(e.getPlayer(), e.getItemStack(), e.getHand());
    }

    private static void clearLastModSelectionAndConsumeItem(Player player, OpenBundleInfo rarity) {
        if (rarity.isWeaponBundle()) {
            player.removeTag(LAST_MOD_SELECTION_WEAPON);
        } else {
            player.removeTag(LAST_MOD_SELECTION.get(rarity.rarity()));
        }

        player.setItemInHand(rarity.hand(), player.getItemInHand(rarity.hand()).consume(1));
    }

    public static void onItemClickEvent(InventoryPreClickEvent e) {
        AbstractInventory inv = e.getPlayer().getOpenInventory();
        if (inv == null || !inv.getTag(BUNDLE_INVENTORY)) {
            return;
        }

        e.setCancelled(true);

        Player player = e.getPlayer();

        if (e.getClickedItem().getTag(IS_BARRIER)) {
            clearLastModSelectionAndConsumeItem(player, player.getTag(OPEN_BUNDLE_MENU));
            player.closeInventory();
            return;
        }

        Mod mod = e.getClickedItem().getTag(ITEM_MOD);
        if (mod == null) {
            return;
        }

        clearLastModSelectionAndConsumeItem(player, player.getTag(OPEN_BUNDLE_MENU));

        AttackSystem attackSystem = AttackSystem.instance(player.getInstance());

        Map<Class<? extends Mod>, Mod> modifiers = attackSystem.getModifiers(player);

        if (modifiers.containsKey(mod.getClass())) {
            attackSystem.upgradeMod(player, mod.getClass(), m -> m.level() + 1);
            player.sendMessage(Component.text()
                    .append(Component.text("You upgraded: "))
                    .append(mod.name())
                    .decoration(TextDecoration.ITALIC, false)
            );
        } else {
            attackSystem.add(player, mod);
            player.sendMessage(Component.text()
                    .append(Component.text("You unlocked: "))
                    .append(mod.name())
                    .decoration(TextDecoration.ITALIC, false)
            );

            if (mod.rarity() == Mod.Rarity.EPIC) {
                player.sendNotification(new Notification(Component.text("You unlocked: ").append(mod.name()), FrameType.CHALLENGE, mod.item()));
            } else {
                player.sendNotification(new Notification(Component.text("You unlocked: ").append(mod.name()), FrameType.GOAL, mod.item()));
            }

            for (Weapon weaponToGrant : mod.getWeaponGrants()) {
                boolean playerHasWeapon = false;

                for (ItemStack inventoryStack : player.getInventory().getItemStacks()) {
                    if (isWeapon(inventoryStack, weaponToGrant)) {
                        playerHasWeapon = true;
                        break;
                    }
                }

                if (!playerHasWeapon) {
                    player.getInventory().addItemStack(weaponToGrant.getItem());
                }
            }
        }

        player.closeInventory();
    }

    private static boolean isWeapon(ItemStack item, Weapon weapon) {
        return item.hasTag(Weapon.WEAPON_TAG) && item.getTag(Weapon.WEAPON_TAG).equals(weapon.getId());
    }

    public static void openBundleMenu(Player player, ItemStack item, PlayerHand hand) {
        Inventory inventory;
        if (item.getTag(IS_WEAPON_BUNDLE)) {
            inventory = player.getTag(LAST_MOD_SELECTION_WEAPON);
            player.setTag(OPEN_BUNDLE_MENU, new OpenBundleInfo(Mod.Rarity.RARE, true, hand));
        } else {
            Mod.Rarity rarity = item.getTag(BUNDLE_RARITY);
            inventory = player.getTag(LAST_MOD_SELECTION.get(rarity));
            player.setTag(OPEN_BUNDLE_MENU, new OpenBundleInfo(rarity, false, hand));
        }

        if (inventory == null) {
            inventory = generateBundleMenu(player, item, hand);
        }

        player.openInventory(inventory);
    }

    public static Inventory generateBundleMenu(Player player, ItemStack item, PlayerHand hand) {
        Inventory inventory = new Inventory(InventoryType.CHEST_1_ROW, "Mods Menu");
        inventory.setTag(BUNDLE_INVENTORY, true);

        List<Mod> selectedMods = new ArrayList<>();

        while (selectedMods.size() < 3) {
            if (item.getTag(IS_WEAPON_BUNDLE)) {
                Mod weaponMod = sampleWeaponUpgrade();
                if (weaponMod == null) {
                    break;
                }

                if (!selectedMods.contains(weaponMod)) {
                    selectedMods.add(weaponMod);
                }

                player.setTag(LAST_MOD_SELECTION_WEAPON, inventory);
            } else {
                Mod mod = sampleUpgrade(item, player, selectedMods);
                if (mod == null) {
                    break;
                }

                selectedMods.add(mod);
                player.setTag(LAST_MOD_SELECTION.get(item.getTag(BUNDLE_RARITY)), inventory);
            }
        }

        for (int i = 0; i < selectedMods.size(); i++) {
            Mod selectedMod = selectedMods.get(i);
            inventory.setItemStack(i * 2 + 2, selectedMod.item().withTag(ITEM_MOD, selectedMod));
        }

        if (!item.getTag(IS_WEAPON_BUNDLE)) {
            inventory.setItemStack(8, ItemStack.builder(Material.BARRIER)
                    .customName(Component.text("No Upgrade").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                    .lore(Component.text("Select this if you want no upgrades.").color(NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false))
                    .build().withTag(IS_BARRIER, true)
            );
        }

        return inventory;
    }

    private static Mod sampleWeaponUpgrade() {
        List<Mod> mods = new ArrayList<>();

        for (Mod.Factory value : Mod.values()) {
            if (value.create(0).getWeaponGrants().isEmpty()) {
                continue;
            }

            mods.add(value.create(0));
        }

        double randomValue = Math.random() * mods.size();
        return mods.get((int) randomValue);
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

    private static @Nullable Mod sampleUpgrade(ItemStack bundle, Player player, List<Mod> selectedMods) {
        Map<Mod.Rarity, Number> rarityChance = new HashMap<>(rarityToRarityDropChance.get(bundle.getTag(BUNDLE_RARITY)));
        while (!rarityChance.isEmpty()) {
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

            Map<Class<? extends Mod>, Mod> modifiers = AttackSystem.instance(player.getInstance()).getModifiers(player);

            for (Mod.Factory value : Mod.values()) {
                Mod mod = value.create(1);

                if (mod.rarity() == rarity && !selectedMods.contains(mod)) {
                    if (modifiers.containsKey(mod.getClass()) && modifiers.get(mod.getClass()).level() >= mod.maxLevel()) {
                        continue;
                    }

                    mods.add(mod);
                }
            }

            if (!mods.isEmpty()) {
                double random = Math.random() * mods.size();
                return mods.get((int) random);
            }

            rarityChance.remove(rarity);
        }

        return null;
    }
}
