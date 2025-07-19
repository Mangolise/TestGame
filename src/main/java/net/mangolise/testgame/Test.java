package net.mangolise.testgame;

import net.kyori.adventure.text.Component;
import net.mangolise.gamesdk.permissions.Permissions;
import net.mangolise.gamesdk.tablist.CustomTabList;
import net.mangolise.gamesdk.tablist.TabListEntry;
import net.mangolise.gamesdk.util.ChatUtil;
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
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        CustomTabList tabList = new CustomTabList();
        tabList.setHeader(ChatUtil.toComponent("&6&lJacob's Mod"));
        tabList.setFooter(ChatUtil.toComponent("&7This is a Minestom game jam game.\n&7Please vote 5/5!"));

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

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> {
            Player p = e.getPlayer();
            if (!e.isFirstSpawn()) return;

            tabList.addPlayer(p);
            tabList.update();
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

        LobbyGame lobby = new LobbyGame(new LobbyGame.Config());
        lobby.setup();

        tabList.setEntriesProvider(p -> {
            List<TabListEntry> entries = new ArrayList<>();
            for (Player op : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                Component text;

                TestGame game = lobby.gameByInstance(op.getInstance());
                boolean dead = game != null && game.isDead(op);
                String namePrefix = dead ? "&m" : "";

                if (Permissions.hasPermission(op, "game.admin")) {
                    text = ChatUtil.toComponent("&a" + namePrefix + op.getUsername() + " &7(Creator)");
                } else if (Permissions.hasPermission(op, "game.minestomofficial")) {
                    text = ChatUtil.toComponent("&b" + namePrefix + op.getUsername() + " &7(Minestom)");
                } else {
                    text = ChatUtil.toComponent("&7" + namePrefix + op.getUsername());
                }

                GameMode gameMode = p.getInstance() == op.getInstance() ? op.getGameMode() : GameMode.SPECTATOR;
                if (dead) {
                    gameMode = GameMode.SURVIVAL;  // dead players are always in survival mode (so they display at the top with players)
                }

                entries.add(TabListEntry.text(text).withGameMode(gameMode).withUsername(op.getUsername()).withSkin(op.getSkin()));
            }

            // sort entries such that players not in spectator mode are at the top
            entries.sort((e1, e2) -> {
                boolean e1Spectator = e1.gameMode() == GameMode.SPECTATOR;
                boolean e2Spectator = e2.gameMode() == GameMode.SPECTATOR;

                if (e1Spectator && !e2Spectator) return 1; // e1 is spectator, e2 is not
                if (!e1Spectator && e2Spectator) return -1; // e2 is spectator, e1 is not
                return e1.username().compareToIgnoreCase(e2.username()); // sort by username
            });

            return entries;
        });

        // schedule to update every 2 seconds
        MinecraftServer.getSchedulerManager().scheduleTask(tabList::update, TaskSchedule.seconds(2), TaskSchedule.seconds(2));

        server.start("0.0.0.0", GameSdkUtils.getConfiguredPort());
    }
}
