package net.mangolise.testgame.commands;

import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.testgame.combat.mods.ModMenu;

public class GiveBundleCommand extends MangoliseCommand {
    public GiveBundleCommand() {
        super("givebundle");

        addPlayerSyntax((player, context) -> {
            for (int i = 0; i < 64; i++) {
                player.getInventory().addItemStack(ModMenu.createBundleItem(false));
            }
        });
    }

    @Override
    protected String getPermission() {
        return "game.command.givebundle";
    }
}
