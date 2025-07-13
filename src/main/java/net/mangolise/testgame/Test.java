package net.mangolise.testgame;

import net.mangolise.combat.CombatConfig;
import net.mangolise.combat.MangoCombat;
import net.mangolise.gamesdk.permissions.Permissions;
import net.mangolise.gamesdk.util.GameSdkUtils;
import net.mangolise.gamesdk.util.PerformanceTracker;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.extras.bungee.BungeeCordProxy;

public class Test {
    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        PerformanceTracker.start();

        if (GameSdkUtils.useBungeeCord()) {
            BungeeCordProxy.enable();
        }

        // give every permission to every player
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> {
            Permissions.setPermission(e.getPlayer(), "*", true);
        });
        
        TestGame.Config config = new TestGame.Config();
        TestGame game = new TestGame(config);
        game.setup();
        server.start("0.0.0.0", GameSdkUtils.getConfiguredPort());
    }
}
