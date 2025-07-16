package net.mangolise.testgame.commands;

import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.testgame.LobbyGame;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;

public class AcceptPartyInviteCommand extends MangoliseCommand {

    public AcceptPartyInviteCommand(LobbyGame lobby) {
        super("acceptpartyinvite");

        addPlayerSyntax((sender, context) ->
                sender.sendMessage("Specify the player you want to accept the party invite from."));

        Argument<String> arg = ArgumentType.String("player");
        addPlayerSyntax(((player, context) -> {
            String partyOwner = context.get(arg);
            lobby.tryJoinParty(player, partyOwner);
        }), arg);
    }

    @Override
    protected String getPermission() {
        return "game.command.acceptpartyinvite";
    }
}
