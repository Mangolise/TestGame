package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.NoCollisionFeature;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.mods.ModMenu;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class TestGame extends BaseGame<TestGame.Config> {
    private static final Pos SPAWN = new Pos(7.5, 0, 8.5);

    private Instance instance;

    protected TestGame(Config config) {
        super(config);
    }

    public static void CreateRegistryEntries() {
        DimensionType dimension = DimensionType.builder().build();
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
            loader = new PolarLoader(new FileInputStream("game.polar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        instance = MinecraftServer.getInstanceManager().createInstanceContainer(dim, loader);
        instance.setTimeRate(0);
        instance.setTimeSynchronizationTicks(0);

        // Player spawning
        for (Player player : config.players) {
            joinPlayer(player);
        }

        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, ModMenu::onItemUseEvent);

        Log.logger().info("Started game");
    }

    public void joinPlayer(Player player) {
        // This is needed for the ONE_GAME option
        if (player.getInstance() != instance) player.setInstance(instance, SPAWN);

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlying(true); // TODO: Remove this
        player.setRespawnPoint(SPAWN);

        // TODO: should we do this?
        // Setting the base value instead of adding a modifier seems to reduce FOV effects.
        //player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(new AttributeModifier("scale-movement-bonus", 1.5, AttributeOperation.ADD_MULTIPLIED_BASE));
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1 * 1.5);
        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(10000.0);

        player.getInventory().clear();
        player.getInventory().addItemStack(ItemStack.of(Material.BOW));
        player.getInventory().addItemStack(ItemStack.of(Material.SUNFLOWER));
        player.getInventory().addItemStack(ItemStack.of(Material.BLAZE_ROD));
        player.getInventory().addItemStack(ItemStack.of(Material.STICK));
        player.getInventory().setItemStack(7, ItemStack.of(Material.CHICKEN_SPAWN_EGG));
        player.getInventory().setItemStack(8, ItemStack.of(Material.ZOMBIE_SPAWN_EGG));
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                new NoCollisionFeature()
        );
    }

    public Instance getInstance() {
        return instance;
    }

    public record Config(Player[] players) { }
}
