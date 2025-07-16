package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.ItemPickupFeature;
import net.mangolise.gamesdk.features.NoCollisionFeature;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.mods.ModMenu;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class TestGame extends BaseGame<TestGame.Config> {
    private static final Pos SPAWN = new Pos(7.5, 0, 8.5);
    private static final PolarLoader worldLoader;
    private final Set<UUID> players = new HashSet<>();

    static {
        try {
            worldLoader = new PolarLoader(new FileInputStream("game.polar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Instance instance;
    private Runnable endCallback;
    private Consumer<Player> kickFromGameConsumer = ignored -> {};

    protected TestGame(Config config, Runnable endCallback) {
        super(config);
        this.endCallback = endCallback;
    }

    protected TestGame(Config config) {
        this(config, () -> {});
    }

    public static void CreateRegistryEntries() {
        DimensionType dimension = DimensionType.builder().build();
        MinecraftServer.getDimensionTypeRegistry().register("test-game-dimension", dimension);
    }

    // TODO: Unregister the instance when everyone leaves
    @Override
    public void setup() {
        RegistryKey<DimensionType> dim = MinecraftServer.getDimensionTypeRegistry().getKey(Key.key("test-game-dimension"));
        if (dim == null) {
            throw new IllegalStateException("Dimension type 'test-game-dimension' not registered. Call CreateRegistryEntries() first.");
        }

        instance = MinecraftServer.getInstanceManager().createInstanceContainer(dim, worldLoader);
        instance.setTimeRate(0);
        instance.setTimeSynchronizationTicks(0);

        AttackSystem.register(instance);

        // Player spawning
        for (Player player : config.players) {
            joinPlayer(player);
        }

        instance.eventNode().addListener(PlayerUseItemEvent.class, ModMenu::onItemUseEvent);
        instance.eventNode().addListener(InventoryPreClickEvent.class, ModMenu::onItemClickEvent);
        instance.eventNode().addListener(InventoryCloseEvent.class, ModMenu::onInventoryCloseEvent);
        instance.eventNode().addListener(ItemDropEvent.class, e -> {
            e.setCancelled(true);
        });

        super.setup();  // do this after the instance is set up so that features can access it
        Log.logger().info("Started game");
    }

    public void end() {
        Log.logger().info("Ending game");
        if (endCallback != null) {
            endCallback.run();
        }

        // kick spectators
        for (Player player : instance.getPlayers()) {
            kickFromGameConsumer.accept(player);
        }

        if (instance != null && instance.isRegistered()) {
            MinecraftServer.getInstanceManager().unregisterInstance(instance);
        } else {
            Log.logger().warn("Instance was not registered or already closed");
        }
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

        // TODO: this should be changed, only on weapons like the staff would this be big
        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(10000.0);

        player.getInventory().clear();

        // TODO: remove this and make sure each weapon has a mod.
        player.getInventory().addItemStack(ModMenu.createBundleItem(true));
        player.getInventory().setItemStack(6, ItemStack.of(Material.SKELETON_SPAWN_EGG));
        player.getInventory().setItemStack(7, ItemStack.of(Material.CHICKEN_SPAWN_EGG));
        player.getInventory().setItemStack(8, ItemStack.of(Material.ZOMBIE_SPAWN_EGG));

        player.eventNode().addListener(PlayerDisconnectEvent.class, e -> {
            leavePlayer(e.getPlayer());
        });

        players.add(player.getUuid());
    }

    public void leavePlayer(Player player) {
        Log.logger().info("Player {} left a game", player.getUsername());
        if (instance.getPlayers().stream().filter(p -> p != player).noneMatch(p -> p.getGameMode() != GameMode.SPECTATOR) && GameConstants.END_EMPTY_GAMES) {
            Log.logger().info("No players left, ending game");
            end();
        }
    }

    public void addSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlying(true);
        player.setRespawnPoint(SPAWN);
        player.setInstance(instance, SPAWN);
        player.sendMessage(ChatUtil.toComponent("&cYou are now a spectator! You can fly around freely."));
        player.sendMessage(ChatUtil.toComponent("&cTo leave type &6/leave&c."));
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                new NoCollisionFeature(),
                new FindTheButtonFeature(),
                new ItemPickupFeature()
        );
    }

    public Instance instance() {
        return instance;
    }

    public static PolarLoader worldLoader() {
        return worldLoader;
    }

    public void setEndCallback(Runnable endCallback) {
        this.endCallback = endCallback;
    }

    public void setKickFromGameConsumer(Consumer<Player> kickFromGameConsumer) {
        this.kickFromGameConsumer = kickFromGameConsumer;
    }

    public Set<UUID> players() {
        return Collections.unmodifiableSet(players);
    }

    public record Config(Player[] players) { }
}
