package net.mangolise.testgame.combat.mods;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.gamesdk.util.InventoryMenu;
import net.mangolise.testgame.combat.AttackSystem;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModMenu extends InventoryMenu {
    public ModMenu(Player player) {
        super(InventoryType.CHEST_3_ROW, Component.text("Current Mods"));

        AttackSystem attackSystem = AttackSystem.instance(player.getInstance());

        attackSystem.getModifiers(player).forEach((modClass, mod) -> {
            ItemStack item = mod.item();
            int level = mod.level() + 1;
            int maxLevel = mod.maxLevel() + 1;

            List<Component> lore = new ArrayList<>(Objects.requireNonNull(item.get(DataComponents.LORE)));
            lore.add(Component.text(String.format("Level: %d/%d", level, maxLevel), NamedTextColor.WHITE));

            addMenuItem(item
                    .withLore(lore)
                    .with(DataComponents.MAX_DAMAGE, maxLevel * 1000 + 1)
                    .with(DataComponents.DAMAGE, (maxLevel - level) * 1000 + 1)
            );
        });
    }
}
