package net.mangolise.testgame.commands;

import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.testgame.LobbyGame;
import net.mangolise.testgame.TestGame;

public class LeaveCommand extends MangoliseCommand {

    public LeaveCommand(LobbyGame lobby) {
        super("leave");

        addPlayerSyntax((player, context) -> {
            TestGame game = lobby.gameByInstance(player.getInstance());
            lobby.addPlayer(player);

            if (game != null) {
                game.leavePlayer(player);
            }
        });
    }

    @Override
    protected String getPermission() {
        return "game.command.leave";
    }
}
