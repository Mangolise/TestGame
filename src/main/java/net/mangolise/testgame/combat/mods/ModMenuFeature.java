package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.gamesdk.Game;
import net.mangolise.testgame.TestGame;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import net.minestom.server.tag.Tag;

import java.util.Arrays;

public class ModMenuFeature implements Game.Feature<TestGame> {
    private static final Tag<Boolean> IS_MOD_MENU_ITEM = Tag.Boolean("testgame.modmenu.is_mod_menu_item").defaultValue(false);
    private static final ItemStack MOD_MENU_ITEM = ItemStack.builder(Material.PLAYER_HEAD)
            .set(IS_MOD_MENU_ITEM, true)
            .customName(Component.text("Equipped Mods", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
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

        eventNode.addListener(PlayerSpawnEvent.class, e -> {
            if (Arrays.stream(e.getPlayer().getInventory().getItemStacks()).noneMatch(stack -> stack.material().equals(MOD_MENU_ITEM.material()))) {
                PlayerSkin skin = e.getPlayer().getSkin();

                e.getPlayer().getInventory().addItemStack(skin == null ? MOD_MENU_ITEM :
                        MOD_MENU_ITEM.with(DataComponents.PROFILE, new HeadProfile(skin)));
            }
        });

        eventNode.addListener(InventoryPreClickEvent.class, e -> {
            if (e.getClickedItem().getTag(IS_MOD_MENU_ITEM) && e.getClick() instanceof Click.Right) {
                e.setCancelled(true);
                e.getPlayer().openInventory(new ModMenu(e.getPlayer()).getInventory());
            }
        });
    }
}
