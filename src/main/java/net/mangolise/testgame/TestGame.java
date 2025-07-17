package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.ItemDropFeature;
import net.mangolise.gamesdk.features.ItemPickupFeature;
import net.mangolise.gamesdk.features.NoCollisionFeature;
import net.mangolise.gamesdk.log.Log;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.gamesdk.util.Timer;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.mods.BundleMenu;
import net.mangolise.testgame.combat.mods.ModMenuFeature;
import net.mangolise.testgame.mobs.spawning.CompleteWaveEvent;
import net.mangolise.testgame.mobs.spawning.WaveSystem;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.pathfinding.NavigableEntity;
import net.minestom.server.event.entity.EntityTickEvent;
import net.minestom.server.event.inventory.InventoryCloseEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class TestGame extends BaseGame<TestGame.Config> {
    private static final Pos SPAWN = new Pos(7.5, 0, 8.5);
    private static final PolarLoader worldLoader;
    private final Set<UUID> players = new HashSet<>();  // list of players who belong in this game (allows them to rejoin)
    private final List<Player> deadPlayers = new ArrayList<>();  // they should respawn at the end of the wave

    static {
        try {
            worldLoader = new ReadOnlyPolarLoader(new FileInputStream("game.polar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Instance instance;
    private Runnable endCallback;
    private Consumer<Player> kickFromGameConsumer = ignored -> {};
    private boolean isEnding = false;  // if it's ending then we don't want to end it again

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

        super.setup();  // do this after the instance is set up so that features can access it
        // but before the players are spawned so that the join function can access the features

        // Player spawning
        for (Player player : config.players) {
            joinPlayer(player);
        }

        instance.eventNode().addListener(PlayerUseItemEvent.class, BundleMenu::onItemUseEvent);
        instance.eventNode().addListener(InventoryPreClickEvent.class, BundleMenu::onItemClickEvent);
        instance.eventNode().addListener(InventoryCloseEvent.class, BundleMenu::onInventoryCloseEvent);
        instance.eventNode().addListener(ItemDropEvent.class, e -> {
            if (!e.getItemStack().material().name().endsWith("bundle") || e.getItemStack().getTag(BundleMenu.IS_WEAPON_BUNDLE)) {
                e.setCancelled(true);
            }
        });
        instance.eventNode().addListener(CompleteWaveEvent.class, e -> {
            e.getInstance().sendMessage(Component.text("Wave " + e.getWaveNumber() + " completed!"));
            e.getInstance().playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP.key(), Sound.Source.PLAYER, 1.0f, 1.0f));
            for (Player player : e.getInstance().getPlayers()) {
                player.heal();
            }

            // respawn dead players
            for (Player player : deadPlayers) {
                player.setGameMode(GameMode.ADVENTURE);
                player.teleport(SPAWN);
                player.showTitle(Title.title(ChatUtil.toComponent("&a&lYou respawned!"), ChatUtil.toComponent("&7Get ready for the next wave.")));
            }
            deadPlayers.clear();

            for (Entity entity : e.getInstance().getEntities()) {
                if (entity instanceof ItemEntity item) {
                    // tp to random player
                    Player randomPlayer = e.getInstance().getPlayers().stream()
                            .skip((int) (Math.random() * e.getInstance().getPlayers().size()))
                            .findFirst()
                            .orElse(null);
                    
                    if (randomPlayer != null) {
                        item.teleport(randomPlayer.getPosition().add(0, 1, 0));
                    }
                }
            }
        });
        instance.eventNode().addListener(EntityTickEvent.class, this::tickEntity);
        instance.eventNode().addListener(PlayerDeathEvent.class, e -> {
            e.setChatMessage(ChatUtil.toComponent("&6" + e.getPlayer().getUsername() + " &chas been killed! &7They will respawn at the end of the wave."));
            Player p = e.getPlayer();
            MinecraftServer.getSchedulerManager().scheduleNextTick(p::respawn);
            p.setGameMode(GameMode.SPECTATOR);
            deadPlayers.add(p);
            p.showTitle(Title.title(ChatUtil.toComponent("&c&lYou died!"), ChatUtil.toComponent("&7You will respawn at the end of the wave.")));

            if (instance.getPlayers().stream().noneMatch(pl -> pl.getGameMode() != GameMode.SPECTATOR)) {
                // game lost
                lose();
            }
        });

        // Start the wave system
        WaveSystem.from(instance).start();

        Log.logger().info("Started game");
    }

    public boolean isDead(Player player) {
        return deadPlayers.contains(player);
    }

    // do fancy stuff
    private void lose() {
        if (isEnding) return;

        isEnding = true;
        Log.logger().info("Game lost, ending game");
        instance.sendMessage(ChatUtil.toComponent("&cYou lost the game!"));
        instance.playSound(Sound.sound(SoundEvent.ENTITY_ENDER_DRAGON_DEATH.key(), Sound.Source.PLAYER, 0.1f, 1.0f));
        instance.showTitle(Title.title(ChatUtil.toComponent("&c&lYou lost!"), ChatUtil.toComponent("&7Better luck next time.")));

        // Blow up the map
        Timer.countDown(100, 2, i -> {
            Vec randomPos = new Vec(
                    SPAWN.x() + (Math.random() - 0.5) * 50,
                    SPAWN.y() + (Math.random() - 0.5) * 20 + 20,
                    SPAWN.z() + (Math.random() - 0.5) * 50
            );
            Entity fb = new Entity(EntityType.FIREBALL);
            fb.setInstance(instance, randomPos);
            fb.setVelocity(new Vec(0, 0, 0).add(Math.random() - 0.5, Math.random() - 1, Math.random() - 0.5).mul(10));
            instance.playSound(Sound.sound(SoundEvent.ENTITY_GHAST_SHOOT.key(), Sound.Source.HOSTILE, 0.4f, 1.0f), randomPos);
            fb.eventNode().addListener(EntityTickEvent.class, e -> {
                if (fb.getVelocity().x() != 0 || fb.getVelocity().z() != 0) return;

                // it stopped moving (aka hit something), so explode
                fb.getInstance().playSound(Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE.key(), Sound.Source.PLAYER, 1.0f, 1.0f), fb.getPosition());

                // destroy every block in a 5 block radius
                for (int x = -5; x <= 5; x++) {
                    for (int y = -5; y <= 5; y++) {
                        for (int z = -5; z <= 5; z++) {
                            if (x * x + y * y + z * z > 25) continue; // only destroy blocks in a sphere
                            instance.setBlock(fb.getPosition().add(x, y, z), Block.AIR);
                        }
                    }
                }

                // particles
                ParticlePacket packet = new ParticlePacket(Particle.EXPLOSION, fb.getPosition(), Vec.ZERO, 0, 3);
                instance.sendGroupedPacket(packet);

                fb.remove();
            });
        }).thenAccept(ignored -> {
            end();
        });
    }

    private record NavigationInfo(Point pos, Point goal, int ticks) {}
    private static final Tag<NavigationInfo> NAVIGATION_INFO = Tag.Transient("testgame.navigation_info");

    private void tickEntity(@NotNull EntityTickEvent entityTickEvent) {
        Entity entity = entityTickEvent.getEntity();
        if (!(entity instanceof NavigableEntity navigable)) {
            return;
        }
        
        var goalPos = navigable.getNavigator().getGoalPosition();
        if (goalPos == null || entity.isRemoved()) {
            // If the entity has no goal or is removed, we don't need to do anything
            return;
        }

        // prevent navigating entities from being stuck
        NavigationInfo info = entity.getTag(NAVIGATION_INFO);

        if (info == null) {
            entity.setTag(NAVIGATION_INFO, new NavigationInfo(entity.getPosition(), goalPos, 0));
            return;
        }

        if (info.ticks() > 60) {
            // apply random movement to prevent being stuck
            double scalar = 16;
            entity.setVelocity(new Vec(scalar * (Math.random() - 0.5), scalar * Math.random(), scalar * (Math.random() - 0.5)));
            // reset the navigation info
            entity.setTag(NAVIGATION_INFO, new NavigationInfo(entity.getPosition(), goalPos, 0));
            return;
        }

        if (!goalPos.sameBlock(info.goal())) {
            // reset the navigation info if the goal has changed
            entity.setTag(NAVIGATION_INFO, new NavigationInfo(entity.getPosition(), goalPos, 0));
            return;
        }

        // increment the ticks
        entity.setTag(NAVIGATION_INFO, new NavigationInfo(info.pos(), info.goal(), info.ticks() + 1));
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
        player.setRespawnPoint(SPAWN);

        player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).setBaseValue(32);

        // Give the default weapon bundle
        player.getInventory().clear();
        player.getInventory().addItemStack(BundleMenu.createBundleItem(true));
        feature(ModMenuFeature.class).giveItem(player);

        player.eventNode().addListener(PlayerDisconnectEvent.class, e -> {
            leavePlayer(e.getPlayer());
        });

        players.add(player.getUuid());
    }

    public void leavePlayer(Player player) {
        Log.logger().info("Player {} left a game", player.getUsername());

        if (isEnding) return;  // if the game is ending, we don't want to do anything
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
                new ItemPickupFeature(),
                new ItemDropFeature(),
                new ModMenuFeature(),
                new BossBarFeature()
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
