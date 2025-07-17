package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.gamesdk.Game;
import net.mangolise.gamesdk.util.InventoryMenu;
import net.mangolise.testgame.TestGame;
import net.mangolise.testgame.combat.AttackSystem;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModMenuFeature implements Game.Feature<TestGame> {
    private static final Tag<Boolean> IS_MOD_MENU_ITEM = Tag.Boolean("testgame.modmenu.is_mod_menu_item").defaultValue(false);
    private static final ItemStack MOD_MENU_ITEM = ItemStack.builder(Material.PLAYER_HEAD)
            .set(IS_MOD_MENU_ITEM, true)
            .customName(Component.text("Equipped Mods", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
            .lore(Component.text("Right click to open", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            .build();

    @Override
    public void setup(Context<TestGame> context) {
        EventNode<InstanceEvent> eventNode = context.game().instance().eventNode();
        eventNode.addListener(PlayerUseItemEvent.class, e -> {
            if (!e.getItemStack().getTag(IS_MOD_MENU_ITEM)) {
                return;
            }

            e.setCancelled(true);
            e.getPlayer().openInventory(new ModMenu(e.getPlayer()).getInventory());
        });

        eventNode.addListener(InventoryPreClickEvent.class, e -> {
            if (e.getClickedItem().getTag(IS_MOD_MENU_ITEM) && e.getClick() instanceof Click.Right) {
                e.setCancelled(true);
                e.getPlayer().openInventory(new ModMenu(e.getPlayer()).getInventory());
            }
        });
    }

    public void giveItem(Player player) {
        PlayerSkin skin = player.getSkin();
        ItemStack item = skin == null ? MOD_MENU_ITEM : MOD_MENU_ITEM.with(DataComponents.PROFILE, new HeadProfile(skin));

        if (player.getInventory().getItemStack(8).isAir()) {
            player.getInventory().setItemStack(8, item);
        } else {
            player.getInventory().addItemStack(item);
        }
    }

    private static class ModMenu extends InventoryMenu {
        public ModMenu(Player player) {
            super(InventoryType.CHEST_3_ROW, Component.text("Current Mods"));

            AttackSystem attackSystem = AttackSystem.instance(player.getInstance());

            attackSystem.getModifiers(player).forEach((modClass, mod) -> {
                ItemStack item = mod.item();
                int level = mod.level() + 1;
                int maxLevel = mod.maxLevel() + 1;

                List<Component> lore = new ArrayList<>(Objects.requireNonNull(item.get(DataComponents.LORE)));
                lore.add(Component.text(""));
                lore.add(Component.text(String.format("Level: %d/%d", level, maxLevel), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, true));

                addMenuItem(item
                        .withLore(lore)
                        .with(DataComponents.MAX_DAMAGE, maxLevel * 1000 + 1)
                        .with(DataComponents.DAMAGE, (maxLevel - level) * 1000 + 1)
                );
            });
        }
    }
}
