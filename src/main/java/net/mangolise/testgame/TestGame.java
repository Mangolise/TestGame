package net.mangolise.testgame;

import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.AdminCommandsFeature;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.permissions.Permissions;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.util.List;

public class TestGame extends BaseGame<TestGame.Config> {
    protected TestGame(Config config) {
        super(config);
    }

    @Override
    public void setup() {
        super.setup();

        DimensionType dimension = DimensionType.builder().ambientLight(15).build();
        RegistryKey<DimensionType> dim = MinecraftServer.getDimensionTypeRegistry().register("test-game-dimension", dimension);

        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(dim);
        instance.setTimeRate(0);
        instance.setTimeSynchronizationTicks(0);

        for (int i = -5; i < 5; i++) {
            for (int j = -5; j < 5; j++) {
                instance.setBlock(i, 0, j, Block.AMETHYST_BLOCK);
            }
        }

        // Player spawning
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            Permissions.setPermission(e.getPlayer(), "*", true);
            e.setSpawningInstance(instance);

            e.getPlayer().setGameMode(GameMode.SURVIVAL);
            e.getPlayer().setRespawnPoint(new Pos(0.5, 1, 0.5, 0, 0));
        });

        Log.logger().info("Started game");
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                new AdminCommandsFeature()
        );
    }

    public record Config() { }
}
