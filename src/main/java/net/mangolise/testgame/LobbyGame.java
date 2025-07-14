package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.AdminCommandsFeature;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class LobbyGame extends BaseGame<LobbyGame.Config> {
    private Instance world;

    private static final ItemStack infoItem = ItemStack
            .builder(Material.REDSTONE_TORCH)
            .customName(Component.text("Information").color(TextColor.color(218, 89, 80)).decoration(TextDecoration.ITALIC, false))
            .hideExtraTooltip()
            .build();

    public LobbyGame(Config config) {
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

            displayBookToast(e.getPlayer());

            e.getPlayer().getInventory().setItemStack(8, infoItem);  // Last hotbar slot
        });

        world.eventNode().addListener(InventoryPreClickEvent.class, e -> {
            e.setCancelled(true);
            inventoryItemInteract(e.getPlayer(), e.getClickedItem());
        });

        world.eventNode().addListener(PlayerUseItemEvent.class, e -> {
            inventoryItemInteract(e.getPlayer(), e.getItemStack());
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

    private static void inventoryItemInteract(Player player, ItemStack item) {
        if (item.equals(infoItem)) {
            displayBookToast(player);
        }
    }

    private static void displayBookToast(Player player) {
        Book book = Book.builder()
                .title(Component.text("Weird zombie game"))
                .pages(
                        Component.text("Welcome to a game!").color(TextColor.color(80, 182, 218)).decorate(TextDecoration.BOLD),
                        Component.text("This game is a zombie survival game where you build up weapons with upgrades to defend against hordes of zombies."),
                        Component.text("Made by CoPokBl, Calcilore, Krystilize and EclipsedMango")
                                .decorate(TextDecoration.ITALIC)
                ).build();

        player.openBook(book);
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
