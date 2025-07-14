package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.AdminCommandsFeature;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class LobbyGame extends BaseGame<LobbyGame.Config> {
    private Instance world;

    protected LobbyGame(Config config) {
        super(config);
    }

    public static void CreateRegistryEntries() {
        DimensionType dimension = DimensionType.builder().ambientLight(15).build();
        MinecraftServer.getDimensionTypeRegistry().register("lobby-dimension", dimension);
    }

    @Override
    public void setup() {
        RegistryKey<DimensionType> dim = MinecraftServer.getDimensionTypeRegistry().getKey(Key.key("test-game-dimension"));
        if (dim == null) {
            throw new IllegalStateException("Dimension type 'test-game-dimension' not registered. Call CreateRegistryEntries() first.");
        }

        PolarLoader loader;
        try {
            loader = new PolarLoader(new FileInputStream("world.polar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        world = MinecraftServer.getInstanceManager().createInstanceContainer(dim, loader);
        world.setTimeRate(0);
        world.setTimeSynchronizationTicks(0);

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> {
            Player player = e.getPlayer();
            e.setSpawningInstance(world);

            player.setGameMode(GameMode.ADVENTURE);
            player.setRespawnPoint(new Pos(36.79, 72.74, 20.48));

            // Reset all attributes to their default values
            for (AttributeInstance attribute : player.getAttributes()) {
                attribute.clearModifiers();
            }
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);  // Set by the game, so we must manually reset it

            player.getInventory().clear();
        });

        world.eventNode().addListener(PlayerSpawnEvent.class, e -> {
            e.getPlayer().sendMessage("Welcome to the Lobby!");
            e.getPlayer().sendMessage("Type 'potato' in chat to start the game!");
        });

        world.eventNode().addListener(PlayerChatEvent.class, e -> {
            Player player = e.getPlayer();
            String message = e.getRawMessage();

            if (message.equalsIgnoreCase("potato")) {
                player.sendMessage("Starting the game...");
                config.startGameMethod.accept(new Player[]{player});
                e.setCancelled(true);
            } else {
                player.sendMessage("Type 'potato' to start the game!");
            }
        });
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                // TODO: Remove admin commands in production
                new AdminCommandsFeature()
        );
    }

    public record Config(Consumer<Player[]> startGameMethod) { }
}
