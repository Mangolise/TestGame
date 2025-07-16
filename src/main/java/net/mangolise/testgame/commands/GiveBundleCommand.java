package net.mangolise.testgame.commands;

import net.mangolise.testgame.combat.mods.ModMenu;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class GiveBundleCommand extends Command {
    public GiveBundleCommand() {
        super("givebundle");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            for (int i = 0; i < 64; i++) {
                player.getInventory().addItemStack(ModMenu.createBundleItem(false));
            }
        });
    }
}
