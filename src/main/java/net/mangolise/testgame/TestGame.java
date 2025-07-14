package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.instance.InstanceAnalysis;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.testgame.combat.AttackSystem;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
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

    public static void CreateRegistryEntries() {
        DimensionType dimension = DimensionType.builder().ambientLight(15).build();
        MinecraftServer.getDimensionTypeRegistry().register("test-game-dimension", dimension);
    }

    @Override
    public void setup() {
        super.setup();

//        MangoCombat.enableGlobal(new CombatConfig().withFakeDeath(true).withVoidDeath(true).withVoidLevel(-10));

        RegistryKey<DimensionType> dim = MinecraftServer.getDimensionTypeRegistry().getKey(Key.key("test-game-dimension"));
        if (dim == null) {
            throw new IllegalStateException("Dimension type 'test-game-dimension' not registered. Call CreateRegistryEntries() first.");
        }

        AttackSystem.register();

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

        // Player spawning
        for (Player player : config.players) {
            player.setInstance(instance);

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlying(true); // TODO: Remove this
            player.setRespawnPoint(new Pos(36.79, 72.74, 20.48));
            player.teleport(new Pos(36.79, 72.74, 20.48));

            player.getAttribute(Attribute.CAMERA_DISTANCE).setBaseValue(10.0);
            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(10000.0);

            // TODO: should we do this?
            player.getAttribute(Attribute.SCALE).setBaseValue(1.5);

            // Setting the base value instead of adding a modifier seems to reduce FOV effects.
            //player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier("scale-movement-bonus", 1.5, AttributeOperation.ADD_MULTIPLIED_BASE));
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1 * 1.5);

            player.getInventory().clear();
            player.getInventory().addItemStack(ItemStack.of(Material.BOW));
            player.getInventory().addItemStack(ItemStack.of(Material.SUNFLOWER));
            player.getInventory().addItemStack(ItemStack.of(Material.BLAZE_ROD));
            player.getInventory().addItemStack(ItemStack.of(Material.STICK));
            player.getInventory().setItemStack(8, ItemStack.of(Material.ZOMBIE_SPAWN_EGG));
        }

        Log.logger().info("Started game");
    }

    @Override
    public List<Feature<?>> features() {
        return List.of();
    }

    public record Config(Player[] players) { }
}
