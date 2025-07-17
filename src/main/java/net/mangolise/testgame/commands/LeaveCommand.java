package net.mangolise.testgame.commands;

import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.LobbyGame;
import net.mangolise.testgame.TestGame;

public class LeaveCommand extends MangoliseCommand {

    public LeaveCommand(LobbyGame lobby) {
        super("leave");

        addPlayerSyntax((player, context) -> {
            TestGame game = lobby.gameByInstance(player.getInstance());
            lobby.addPlayer(player);

            if (game != null) {
                game.instance().sendMessage(ChatUtil.toComponent("&7[&aGame&7] &7[&c-&7] &7" + player.getUsername()));
                game.leavePlayer(player);
            }
        });
    }

    @Override
    protected String getPermission() {
        return "game.command.leave";
    }
}
