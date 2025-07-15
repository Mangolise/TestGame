package net.mangolise.testgame.commands;

import net.mangolise.testgame.LobbyGame;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class AcceptPartyInviteCommand extends Command {

    public AcceptPartyInviteCommand(LobbyGame lobby) {
        super("acceptpartyinvite");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            sender.sendMessage("Specify the player you want to accept the party invite from.");
        });

        Argument<String> arg = ArgumentType.String("player");
        addSyntax(((sender, context) -> {
            String partyOwner = context.get(arg);
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return;
            }

            lobby.tryJoinParty(player, partyOwner);
        }), arg);
    }
}
