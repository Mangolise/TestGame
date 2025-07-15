package net.mangolise.testgame.commands;

import net.mangolise.testgame.LobbyGame;
import net.minestom.server.command.builder.Command;

public class LeaveCommand extends Command {

    public LeaveCommand(LobbyGame lobby) {
        super("leave");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof net.minestom.server.entity.Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            lobby.addPlayer(player);
        });
    }
}
