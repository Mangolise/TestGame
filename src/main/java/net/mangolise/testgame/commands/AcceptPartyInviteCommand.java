package net.mangolise.testgame.commands;

import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.testgame.LobbyGame;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.List;

public class AcceptPartyInviteCommand extends MangoliseCommand {

    public AcceptPartyInviteCommand(LobbyGame lobby) {
        super("acceptpartyinvite");

        addPlayerSyntax((sender, context) ->
                sender.sendMessage("Specify the player you want to accept the party invite from."));

        addPlayerSyntax(((player, context) -> {
            List<Player> selections = getPlayers(context, player, "player");
            if (selections.size() != 1) {
                player.sendMessage("You must specify exactly one player to accept the party invite from.");
                return;
            }
            lobby.tryJoinParty(player, selections.getFirst().getUsername());
        }), ArgumentType.Entity("player").onlyPlayers(true));
    }

    @Override
    protected String getPermission() {
        return "game.command.acceptpartyinvite";
    }
}
