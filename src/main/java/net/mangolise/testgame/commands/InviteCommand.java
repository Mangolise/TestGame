package net.mangolise.testgame.commands;

import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.LobbyGame;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.List;

public class InviteCommand extends MangoliseCommand {

    public InviteCommand(LobbyGame lobby) {
        super("invite");

        addPlayerSyntax((player, context) -> {
            List<Player> targets = getPlayers(context, player, "player");
            if (targets.isEmpty()) {
                player.sendMessage("You must specify a player to invite.");
                return;
            }

            if (lobby.gameByPlayer(player) != null) {
                player.sendMessage(ChatUtil.toComponent("&cYou cannot invite players while in a game."));
                return;
            }

            boolean didAnInvite = false;
            for (Player target : targets) {
                if (target == player) {
                    continue;
                }

                if (lobby.gameByPlayer(target) != null) {
                    continue;
                }

                lobby.sendPartyInvite(player, target);
                didAnInvite = true;
            }

            if (!didAnInvite) {
                player.sendMessage(ChatUtil.toComponent("&cNo players were invited. They might be in a game already."));
            }
        }, ArgumentType.Entity("player").onlyPlayers(true));
    }

    @Override
    protected String getPermission() {
        return "game.command.invite";
    }
}
