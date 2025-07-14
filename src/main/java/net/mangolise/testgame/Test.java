package net.mangolise.testgame;

import net.mangolise.gamesdk.permissions.Permissions;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.gamesdk.util.PerformanceTracker;
import net.mangolise.testgame.commands.GiveBundleCommand;
import net.mangolise.testgame.commands.GiveModsCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.velocity.VelocityProxy;

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

        // give every permission to every player
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> {
            Permissions.setPermission(e.getPlayer(), "*", true);
        });

        TestGame.CreateRegistryEntries();
        LobbyGame.CreateRegistryEntries();

        // TODO: How should we handle this?
        MinecraftServer.getCommandManager().register(
                new GiveModsCommand(),
                new GiveBundleCommand()
        );

        boolean oneGame = System.getenv("ONE_GAME") != null && System.getenv("ONE_GAME").equalsIgnoreCase("true");
        if (oneGame) {  // Start one game and join all players to it
            TestGame game = new TestGame(new TestGame.Config(new Player[0]));
            game.setup();

            MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> e.setSpawningInstance(game.getInstance()));
            MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> game.joinPlayer(e.getPlayer()));
        } else {  // Regular prod setup with lobby
            LobbyGame lobby = new LobbyGame(new LobbyGame.Config(ps -> {
                TestGame game = new TestGame(new TestGame.Config(ps));
                game.setup();
            }));
            lobby.setup();
        }

        server.start("0.0.0.0", GameSdkUtils.getConfiguredPort());
    }
}
