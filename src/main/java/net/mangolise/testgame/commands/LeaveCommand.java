package net.mangolise.testgame.commands;

import net.mangolise.testgame.LobbyGame;
import net.mangolise.testgame.TestGame;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class LeaveCommand extends Command {

    public LeaveCommand(LobbyGame lobby) {
        super("leave");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            TestGame game = lobby.gameByInstance(player.getInstance());
            lobby.addPlayer(player);

            if (game != null) {
                game.leavePlayer(player);
            }
        });
    }
}
