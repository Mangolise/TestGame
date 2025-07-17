package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.MinecraftServer;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: maybe possibly make the messages have a hover for item tooltips and a click that opens ModMenu
public class BundleMenu {
    public static Tag<Mod.Rarity> BUNDLE_RARITY = Tag.String("testgame.modmenu.rarity").map(Mod.Rarity::valueOf, Mod.Rarity::name);
    public static Tag<Boolean> BUNDLE_INVENTORY = Tag.Boolean("testgame.modmenu.bundle_inventory").defaultValue(false);
    public static Tag<Boolean> IS_BARRIER = Tag.Boolean("testgame.modmenu.is_barrier").defaultValue(false);
    public static Tag<Boolean> IS_WEAPON_BUNDLE = Tag.Boolean("testgame.modmenu.is_weapon_bundle").defaultValue(false);
    public static Tag<Mod> ITEM_MOD = Tag.Component("testgame.modmenu.item_mod").map(c -> Mod.values().stream().filter(a -> a.create(0).name().equals(c)).findAny().get().create(0), Mod::name);

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

        openBundleMenu(e.getPlayer(), e.getItemStack());
        e.getPlayer().setItemInHand(e.getHand(), e.getItemStack().consume(1));
    }

    public static void onInventoryCloseEvent(InventoryCloseEvent e) {
        if (!e.getInventory().getTag(BUNDLE_INVENTORY)) {
            return;
        }

        MinecraftServer.getSchedulerManager().scheduleEndOfTick(() -> {
            e.getPlayer().openInventory((Inventory) e.getInventory());
        });
    }

    public static void onItemClickEvent(InventoryPreClickEvent e) {
        AbstractInventory inv = e.getPlayer().getOpenInventory();
        if (inv == null || !inv.getTag(BUNDLE_INVENTORY)) {
            return;
        }

        if (e.getInventory() != inv && !(e.getClick() instanceof Click.RightShift || e.getClick() instanceof Click.LeftShift)) {
            return;
        }

        e.setCancelled(true);
        Player player = e.getPlayer();

        if (e.getClickedItem().getTag(IS_BARRIER)) {
            inv.removeTag(BUNDLE_INVENTORY);
            e.getPlayer().closeInventory();
        }

        Mod mod = e.getClickedItem().getTag(ITEM_MOD);
        if (mod == null) {
            return;
        }

        AttackSystem attackSystem = AttackSystem.instance(player.getInstance());

        Map<Class<? extends Mod>, Mod> modifiers = attackSystem.getModifiers(player);

        if (modifiers.containsKey(mod.getClass())) {
            attackSystem.upgradeMod(e.getPlayer(), mod.getClass(), m -> m.level() + 1);
            player.sendMessage(Component.text()
                    .append(Component.text("You upgraded: "))
                    .append(mod.name())
                    .decoration(TextDecoration.ITALIC, false)
            );
        } else {
            attackSystem.add(e.getPlayer(), mod);
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

        inv.removeTag(BUNDLE_INVENTORY);
        e.getPlayer().closeInventory();
    }

    private static boolean isWeapon(ItemStack item, Weapon weapon) {
        return item.hasTag(Weapon.WEAPON_TAG) && item.getTag(Weapon.WEAPON_TAG).equals(weapon.getId());
    }

    public static void openBundleMenu(Player player, ItemStack item) {
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
            } else {
                Mod mod = sampleUpgrade(item, player, selectedMods);
                if (mod == null) {
                    break;
                }

                selectedMods.add(mod);
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

        player.openInventory(inventory);
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
