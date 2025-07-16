package net.mangolise.testgame;

import net.mangolise.gamesdk.permissions.Permissions;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.gamesdk.util.PerformanceTracker;
import net.mangolise.testgame.commands.BenchmarkTestCommand;
import net.mangolise.testgame.commands.GiveAllWeaponsCommand;
import net.mangolise.testgame.commands.GiveBundleCommand;
import net.mangolise.testgame.commands.GiveModsCommand;
import net.mangolise.testgame.mobs.TestPlayer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.velocity.VelocityProxy;

import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        // Remove dumb Minestom limitations that are enabled by default.
        System.setProperty("minestom.packet-queue-size", "10000000");
        System.setProperty("minestom.packet-per-tick", "10000000");

        MinecraftServer server = MinecraftServer.init();
        PerformanceTracker.start();

        if (GameSdkUtils.useBungeeCord()) {
            BungeeCordProxy.enable();
        }

        // This is required for the final submission.
        String secret = System.getenv("VELOCITY_SECRET");
        if (secret != null) {
            VelocityProxy.enable(secret);
        }

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> {
            if (Arrays.stream(GameConstants.CREATORS).anyMatch(n -> n.equalsIgnoreCase(e.getPlayer().getUsername()))) {
                Permissions.setPermission(e.getPlayer(), "*", true);
            } else if (Arrays.stream(GameConstants.MINESTOM_OFFICIALS).anyMatch(n -> n.equalsIgnoreCase(e.getPlayer().getUsername()))) {
                Permissions.setPermission(e.getPlayer(), "game.minestomofficial", true);
                Permissions.setPermission(e.getPlayer(), "mangolise.command.tps", true);
            }

            Permissions.setPermission(e.getPlayer(), "game.command.leave", true);
            Permissions.setPermission(e.getPlayer(), "game.command.acceptpartyinvite", true);

            e.getPlayer().updateViewableRule(p -> e.getPlayer().getGameMode() != GameMode.SPECTATOR);  // hide spectators
        });

        MinecraftServer.getConnectionManager().setPlayerProvider(TestPlayer::new);

        TestGame.CreateRegistryEntries();
        LobbyGame.CreateRegistryEntries();

        MinecraftServer.getCommandManager().register(
                new GiveModsCommand(),
                new GiveBundleCommand(),
                new BenchmarkTestCommand(),
                new GiveAllWeaponsCommand()
        );

        boolean oneGame = System.getenv("ONE_GAME") != null && System.getenv("ONE_GAME").equalsIgnoreCase("true");
        if (oneGame) {  // Start one game and join all players to it
            TestGame game = new TestGame(new TestGame.Config(new Player[0]));
            game.setup();

            MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> e.setSpawningInstance(game.instance()));
            MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> game.joinPlayer(e.getPlayer()));
        } else {  // Regular prod setup with lobby
            LobbyGame lobby = new LobbyGame(new LobbyGame.Config());
            lobby.setup();
        }

        server.start("0.0.0.0", GameSdkUtils.getConfiguredPort());
    }
}
