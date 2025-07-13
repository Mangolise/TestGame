package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.AdminCommandsFeature;
import net.mangolise.gamesdk.instance.InstanceAnalysis;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.permissions.Permissions;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.commands.GiveModsCommand;
import net.mangolise.testgame.mobs.TestZombie;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class TestGame extends BaseGame<TestGame.Config> {
    protected TestGame(Config config) {
        super(config);
    }

    @Override
    public void setup() {
        super.setup();

//        MangoCombat.enableGlobal(new CombatConfig().withFakeDeath(true).withVoidDeath(true).withVoidLevel(-10));

        DimensionType dimension = DimensionType.builder().ambientLight(15).build();
        RegistryKey<DimensionType> dim = MinecraftServer.getDimensionTypeRegistry().register("test-game-dimension", dimension);

        AttackSystem.register();
        
        MinecraftServer.getCommandManager().register(new GiveModsCommand());

        PolarLoader loader;
        try {
            loader = new PolarLoader(new FileInputStream("world.polar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var blocks = InstanceAnalysis.analyse(loader.world());

        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer(dim, loader);
        instance.setTimeRate(0);
        instance.setTimeSynchronizationTicks(0);

        blocks.forEach((blockStateId, points) -> {
            Block block = Block.fromStateId(blockStateId);
            if (block == null) {
                Log.logger().warn("Unknown block state: " + blockStateId);
                return;
            }
            
            boolean isFullBlock = true;
            for (BlockFace face : BlockFace.values()) {
                if (!block.registry().collisionShape().isFaceFull(face)) {
                    isFullBlock = false;
                    break;
                }
            }
            
            if (isFullBlock) {
                return;
            }
            
            for (var point : points) {
                instance.setBlock(point.blockX(), point.blockY(), point.blockZ(), Block.AIR);
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> {
            var player = e.getPlayer();

            for (int i = 0; i < 128; i++) {
                TestZombie entity = new TestZombie();
                double speedMultiplier = 0.1 + Math.random() * Math.random() * 0.3;

                entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedMultiplier);

                entity.setInstance(instance, new Pos(36.79 + Math.random() * 8.0, 72.74 + Math.random() * 64.0, 20.48 + Math.random() * 8.0));
                entity.setTarget(player);
                entity.getAttribute(Attribute.SCALE).setBaseValue(0.5 * (1.0 / Math.pow(speedMultiplier, 0.4)));
            }
        });

        // Player spawning
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, e -> {
            var player = e.getPlayer();
            Permissions.setPermission(player, "*", true);
            e.setSpawningInstance(instance);

            player.setGameMode(GameMode.SURVIVAL);
            player.setRespawnPoint(new Pos(36.79, 72.74, 20.48));

            player.getAttribute(Attribute.CAMERA_DISTANCE).setBaseValue(10.0);
            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(10000.0);
            
            // TODO: should we do this?
            player.getAttribute(Attribute.SCALE).setBaseValue(1.5);
            player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier("scale-movement-bonus", 1.5, AttributeOperation.ADD_MULTIPLIED_BASE));

            player.getInventory().clear();
            player.getInventory().addItemStack(ItemStack.of(Material.BOW));
            player.getInventory().addItemStack(ItemStack.of(Material.SUNFLOWER));
            player.getInventory().addItemStack(ItemStack.of(Material.BLAZE_ROD));
            player.getInventory().addItemStack(ItemStack.of(Material.STICK));
        });

        Log.logger().info("Started game");
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                // TODO: Remove admin commands feature
                new AdminCommandsFeature()
        );
    }

    public record Config() { }
}
