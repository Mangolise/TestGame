package net.mangolise.testgame;

import net.hollowcube.polar.PolarLoader;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mangolise.gamesdk.BaseGame;
import net.mangolise.gamesdk.features.AdminCommandsFeature;
import net.mangolise.gamesdk.features.NoCollisionFeature;
import net.mangolise.gamesdk.features.SignFeature;
import net.mangolise.gamesdk.permissions.Permissions;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.gamesdk.util.InventoryMenu;
import net.mangolise.testgame.commands.AcceptPartyInviteCommand;
import net.mangolise.testgame.commands.InviteCommand;
import net.mangolise.testgame.commands.LeaveCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class LobbyGame extends BaseGame<LobbyGame.Config> {
    private static final Pos SPAWN = new Pos(5, 66.5, 6);

    private static final Tag<Set<Player>> PARTY_MEMBERS_TAG = Tag.Transient("lobby.partymembers");
    private static final Tag<Set<Player>> PARTY_MEMBER_INVITES_TAG = Tag.Transient("lobby.partyinvites");
    private static final Tag<Player> JOINED_PARTY_TAG = Tag.Transient("lobby.joinedparty");

    private final ConcurrentLinkedQueue<Player> queue = new ConcurrentLinkedQueue<>();
    private @Nullable Task queueStartTask = null;
    private BossBar queueBossBar = BossBar.bossBar(
            ChatUtil.toComponent("&a&lQueue: &r&70/100"),
            0.7f,
            BossBar.Color.GREEN,
            BossBar.Overlay.NOTCHED_12
    );
    private int queueRemainingTime = 0;
    private Instance world;
    private final List<TestGame> games = new ArrayList<>();
    private static final @NotNull PlayerSkin defaultSkin = Objects.requireNonNull(PlayerSkin.fromUsername("Technoblade"));

    private static final ItemStack infoItem = ItemStack
            .builder(Material.REDSTONE_TORCH)
            .customName(ChatUtil.toComponent("&r&aInformation &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack joinQueueItem = ItemStack
            .builder(Material.WOODEN_SWORD)
            .customName(ChatUtil.toComponent("&r&aJoin Queue &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack playSoloItem = ItemStack
            .builder(Material.STONE_SWORD)
            .customName(ChatUtil.toComponent("&r&aPlay Solo &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack playWithPartyItem = ItemStack
            .builder(Material.IRON_SWORD)
            .customName(ChatUtil.toComponent("&r&aCreate Party &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack spectateItem = ItemStack
            .builder(Material.SPYGLASS)
            .customName(ChatUtil.toComponent("&r&aSpectate Game &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack rejoinItem = ItemStack
            .builder(Material.BLUE_BED)
            .customName(ChatUtil.toComponent("&r&aRejoin Game &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    // Party inv items
    private static final ItemStack startPartyGameItem = ItemStack
            .builder(Material.IRON_SWORD)
            .customName(ChatUtil.toComponent("&r&aStart Game &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack disbandPartyItem = ItemStack
            .builder(Material.BARRIER)
            .customName(ChatUtil.toComponent("&r&aDisband Party &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack leavePartyItem = ItemStack
            .builder(Material.BARRIER)
            .customName(ChatUtil.toComponent("&r&aLeave Party &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack invitePartyMemberItem = ItemStack
            .builder(Material.VILLAGER_SPAWN_EGG)
            .customName(ChatUtil.toComponent("&r&aInvite Party Member &7(Right Click Player)"))
            .hideExtraTooltip()
            .build();

    private static final ItemStack leaveQueueItem = ItemStack
            .builder(Material.BARRIER)
            .customName(ChatUtil.toComponent("&r&aLeave Queue &7(Right Click)"))
            .hideExtraTooltip()
            .build();

    public LobbyGame(Config config) {
        super(config);
    }

    public static void CreateRegistryEntries() {
        DimensionType dimension = DimensionType.builder().ambientLight(15).build();
        MinecraftServer.getDimensionTypeRegistry().register("lobby-dimension", dimension);
    }

    public void addPlayer(Player player) {
        if (player.getInstance() != world) {
            player.setInstance(world, SPAWN);
        }

        player.setGameMode(GameMode.ADVENTURE);
        player.setRespawnPoint(SPAWN);

        // Reset all attributes to their default values
        for (AttributeInstance attribute : player.getAttributes()) {
            attribute.clearModifiers();
        }
        player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);  // Set by the game, so we must manually reset it
        giveRegularItems(player);
    }

    @Override
    public void setup() {
        super.setup();

        MinecraftServer.getCommandManager().register(
                new AcceptPartyInviteCommand(this),
                new LeaveCommand(this),
                new InviteCommand(this)
        );

        RegistryKey<DimensionType> dim = MinecraftServer.getDimensionTypeRegistry().getKey(Key.key("lobby-dimension"));
        if (dim == null) {
            throw new IllegalStateException("Dimension type 'lobby-dimension' not registered. Call CreateRegistryEntries() first.");
        }

        PolarLoader loader;
        try {
            loader = new PolarLoader(new FileInputStream("lobby.polar"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        world = MinecraftServer.getInstanceManager().createInstanceContainer(dim, loader);
        world.setTimeRate(0);
        world.setTimeSynchronizationTicks(0);

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, e -> {
            e.setSpawningInstance(world);
            e.getPlayer().eventNode().addListener(PlayerDisconnectEvent.class, dc -> {
                queue.remove(dc.getPlayer());
                updateQueueBossBar();
            });
        });

        world.eventNode().addListener(PlayerSpawnEvent.class, e -> {
            if (!e.isFirstSpawn()) {
                return;
            }
            addPlayer(e.getPlayer());
            e.getPlayer().teleport(SPAWN);

            if (Permissions.hasPermission(e.getPlayer(), "game.admin")) {
                e.getPlayer().sendMessage(ChatUtil.toComponent("&c&lYou have admin permissions!"));
            } else if (Permissions.hasPermission(e.getPlayer(), "game.minestomofficial")) {
                e.getPlayer().sendMessage(ChatUtil.toComponent("&b&lWelcome to our game! You are a Minestom official!"));
            }
        });

        world.eventNode().addListener(InventoryPreClickEvent.class, e -> {
            e.setCancelled(true);
            inventoryItemInteract(e.getPlayer(), e.getClickedItem());
        });

        world.eventNode().addListener(PlayerUseItemEvent.class, e -> {
            inventoryItemInteract(e.getPlayer(), e.getItemStack());
        });

        world.eventNode().addListener(PlayerEntityInteractEvent.class, e -> {
            if (e.getHand() != PlayerHand.MAIN) return;

            if (!e.getPlayer().getItemInMainHand().equals(invitePartyMemberItem)) {
                return;
            }

            if (!(e.getTarget() instanceof Player target)) {
                return;
            }

            sendPartyInvite(e.getPlayer(), target);
        });
    }

    public void sendPartyInvite(Player player, Player target) {
        Set<Player> invitees = player.getTag(PARTY_MEMBER_INVITES_TAG);
        if (!invitees.add(target)) {
            // it was already there
            player.sendMessage(ChatUtil.toComponent("&cYou have already invited &6" + target.getUsername() + "&c to your party."));
            return;
        }

        player.sendMessage(ChatUtil.toComponent("&aYou have invited &6" + target.getUsername() + "&a to your party!"));
        target.sendMessage(ChatUtil.toComponent("&6" + player.getUsername() + "&a has invited you to their party!"));

        Component acceptMessage = Component.text("Click here to accept the invite.")
                .color(NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(ChatUtil.toComponent("&aClick this to join their party!")))
                .clickEvent(ClickEvent.runCommand("/acceptpartyinvite " + player.getUsername()));
        target.sendMessage(acceptMessage);
    }

    private void startGame(Player[] players) {
        for (Player player : players) {
            player.sendMessage(ChatUtil.toComponent("&aSending you into the game..."));
        }
        TestGame game = new TestGame(new TestGame.Config(players));
        game.setup();
        games.add(game);
        game.setEndCallback(() -> games.remove(game));
        game.setKickFromGameConsumer(player -> {
            addPlayer(player);
            player.sendMessage(ChatUtil.toComponent("&cThe game has ended! You have been returned to the lobby."));
        });
    }

    private void giveRegularItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItemStack(0, joinQueueItem);
        player.getInventory().setItemStack(1, playSoloItem);
        player.getInventory().setItemStack(2, playWithPartyItem);
        player.getInventory().setItemStack(6, rejoinItem);
        player.getInventory().setItemStack(7, spectateItem);
        player.getInventory().setItemStack(8, infoItem);
    }

    private void inventoryItemInteract(Player player, ItemStack item) {
        if (item.equals(infoItem)) {
            displayBookToast(player);
        } else if (item.equals(playSoloItem)) {
            player.sendMessage(ChatUtil.toComponent("&aJoining a solo game..."));
            startGame(new Player[]{player});
        } else if (item.equals(joinQueueItem)) {
            enqueue(player);
        } else if (item.equals(playWithPartyItem)) {
            player.setTag(PARTY_MEMBERS_TAG, new HashSet<>());
            player.setTag(PARTY_MEMBER_INVITES_TAG, new HashSet<>());
            player.sendMessage(ChatUtil.toComponent("&aParty created! Right click the Villager Spawn Egg to invite players!"));
            player.playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP.key(), Sound.Source.MASTER, 1.0f, 1.0f));

            // Give them the party items
            player.getInventory().clear();
            player.getInventory().setItemStack(0, startPartyGameItem);
            player.getInventory().setItemStack(1, invitePartyMemberItem);
            player.getInventory().setItemStack(8, disbandPartyItem);
        } else if (item.equals(disbandPartyItem)) {
            Set<Player> partyMembers = player.getTag(PARTY_MEMBERS_TAG);
            if (partyMembers == null) {
                player.sendMessage(ChatUtil.toComponent("&cYou are not in a party!"));
                return;
            }

            for (Player member : partyMembers) {
                leaveParty(member);
            }

            player.sendMessage(ChatUtil.toComponent("&aParty disbanded!"));
            player.removeTag(PARTY_MEMBERS_TAG);
            giveRegularItems(player);
        } else if (item.equals(startPartyGameItem)) {
            Set<Player> partyMembers = player.getTag(PARTY_MEMBERS_TAG);
            if (partyMembers == null || partyMembers.isEmpty()) {
                player.sendMessage(ChatUtil.toComponent("&cYou are not in a party!"));
                return;
            }

            partyMembers.add(player);
            player.removeTag(PARTY_MEMBERS_TAG);
            for (Player member : partyMembers) {
                member.removeTag(JOINED_PARTY_TAG);
                member.sendMessage(ChatUtil.toComponent("&aParty game starting with " + partyMembers.size() + " members!"));
            }

            // Start the game with the party members
            startGame(partyMembers.toArray(new Player[0]));
        } else if (item.equals(leavePartyItem)) {
            leaveParty(player);
        } else if (item.equals(leaveQueueItem)) {
            if (queue.remove(player)) {
                player.sendMessage(ChatUtil.toComponent("&cYou have left the queue!"));
                player.playSound(Sound.sound(SoundEvent.BLOCK_ANVIL_BREAK.key(), Sound.Source.MASTER, 0.1f, 1.0f));
                giveRegularItems(player);
                player.hideBossBar(queueBossBar);
                updateQueueBossBar();
            } else {
                player.sendMessage(ChatUtil.toComponent("&cYou are not in the queue!"));
            }
        } else if (item.equals(spectateItem)) {
            InventoryMenu menu = new InventoryMenu(InventoryType.CHEST_6_ROW, ChatUtil.toComponent("&a&lOngoing Games"));
            for (TestGame game : games) {
                List<Component> lore = new ArrayList<>();  // will contain the player names
                for (Player p : game.instance().getPlayers()) {
                    lore.add(ChatUtil.toComponent("&r&7" + p.getUsername()));
                }
                PlayerSkin iconSkin = !game.instance().getPlayers().isEmpty() ? game.instance().getPlayers().stream().findFirst().get().getSkin() : null;
                if (iconSkin == null) {
                    iconSkin = defaultSkin;
                }
                ItemStack icon = ItemStack
                        .of(Material.PLAYER_HEAD)
                        .with(DataComponents.PROFILE, new HeadProfile(iconSkin))
                        .withLore(lore)
                        .withCustomName(ChatUtil.toComponent("&r&bGame: " + game.instance().getPlayers().size() + " players"));
                menu.addMenuItem(icon).onLeftClick(e -> game.addSpectator(e.player()));
            }
            player.openInventory(menu.getInventory());
        } else if (item.equals(rejoinItem)) {
            // Check if the player is in a game
            for (TestGame game : games) {
                if (game.players().contains(player.getUuid())) {
                    player.sendMessage(ChatUtil.toComponent("&aRejoining your game..."));
                    game.joinPlayer(player);
                    return;
                }
            }
            player.sendMessage(ChatUtil.toComponent("&cCould not find a game to rejoin! If everyone left, the game likely ended."));
        }
    }

    public void tryJoinParty(Player player, String leader) {
        Player partyLeader = MinecraftServer.getConnectionManager().findOnlinePlayer(leader);
        if (partyLeader == null) {
            player.sendMessage(ChatUtil.toComponent("&cPlayer not found!"));
            return;
        }

        Set<Player> invites = partyLeader.getTag(PARTY_MEMBER_INVITES_TAG);
        if (invites == null || !invites.contains(player)) {
            player.sendMessage(ChatUtil.toComponent("&cYou have not been invited to this party!"));
            return;
        }

        // They were invited, so we can join the party
        Set<Player> partyMembers = partyLeader.getTag(PARTY_MEMBERS_TAG);
        if (!partyMembers.add(player)) {
            // They are already in the party
            player.sendMessage(ChatUtil.toComponent("&cYou are already in this party!"));
            return;
        }
        player.setTag(JOINED_PARTY_TAG, partyLeader);

        for (Player member : partyMembers) {
            member.sendMessage(ChatUtil.toComponent("&a" + player.getUsername() + " has joined the party!"));
        }
        partyLeader.sendMessage(ChatUtil.toComponent("&a" + player.getUsername() + " has joined your party!"));

        player.getInventory().clear();
        player.getInventory().setItemStack(8, leavePartyItem);
    }

    /**
     * Makes the specified player leave the party they are in.
     * @param player The player to leave the party.
     */
    private void leaveParty(Player player) {
        Player partyLeader = player.getTag(JOINED_PARTY_TAG);
        if (partyLeader == null) {
            return;
        }

        // Remove the player from the party members
        Set<Player> partyMembers = partyLeader.getTag(PARTY_MEMBERS_TAG);
        if (partyMembers != null) {
            partyMembers.remove(player);
            player.setTag(PARTY_MEMBERS_TAG, partyMembers);

            player.sendMessage(ChatUtil.toComponent("&cYou have left the party!"));
            for (Player member : partyMembers) {
                member.sendMessage(ChatUtil.toComponent("&c" + player.getUsername() + " has left the party!"));
            }
        }

        player.removeTag(JOINED_PARTY_TAG);
        giveRegularItems(player);
    }

    private void enqueue(Player player) {
        if (queue.contains(player)) {
            player.sendMessage(ChatUtil.toComponent("&cYou are already in the queue!"));
            return;
        }

        queue.add(player);
        player.sendMessage(ChatUtil.toComponent("&aYou have joined the queue!"));
        player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP.key(), Sound.Source.MASTER, 1.0f, 1.0f));

        player.getInventory().clear();
        player.getInventory().setItemStack(8, leaveQueueItem);

        updateQueueBossBar();

        if (queue.size() >= GameConstants.GAME_QUEUE_MIN_PLAYERS) {
            if (queueStartTask != null) {  // already waiting
                if (queue.size() >= GameConstants.GAME_QUEUE_MAX_PLAYERS) {
                    dequeueGame();  // We can start a maxed game immediately
                }
                return;
            }

            startQueueTimer();
        }
    }

    private void updateQueueBossBar() {
        queueAction(p -> p.hideBossBar(queueBossBar));
        queueBossBar = BossBar.bossBar(
                ChatUtil.toComponent("&a&lQueue: &r&7" + queue.size() + "/" + GameConstants.GAME_QUEUE_MAX_PLAYERS),
                (float) queue.size() / GameConstants.GAME_QUEUE_MAX_PLAYERS,
                BossBar.Color.GREEN,
                BossBar.Overlay.NOTCHED_12
        );
        queueAction(p -> p.showBossBar(queueBossBar));
    }

    private void queueAnnounce(Component message) {
        queueAction(p -> p.sendMessage(message));
    }

    // Perform an action on every player in the queue
    private void queueAction(Consumer<Player> action) {
        for (Player player : queue) {
            action.accept(player);
        }
    }

    private void startQueueTimer() {
        queueRemainingTime = GameConstants.GAME_QUEUE_START_TIME;
        queueStartTask = MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            queueRemainingTime--;
            if (queueRemainingTime <= 0) {
                dequeueGame();
            } else {
                queueAction(p -> p.sendActionBar(ChatUtil.toComponent("&aStarting game in " + queueRemainingTime + " seconds...")));
            }
        }, TaskSchedule.seconds(1), TaskSchedule.seconds(1));
    }

    private void dequeueGame() {
        if (queueStartTask != null) {
            queueStartTask.cancel();
            queueStartTask = null;
        }

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < GameConstants.GAME_QUEUE_MAX_PLAYERS; i++) {
            Player p = queue.poll();
            if (p != null && (p.isRemoved() || !p.isOnline())) {
                continue;
            }
            if (p == null) {
                break;
            }
            players.add(p);
            p.hideBossBar(queueBossBar);
        }

        if (players.isEmpty()) {
            return;
        }
        startGame(players.toArray(new Player[0]));

        // Check if we can start another game immediately
        if (players.size() >= GameConstants.GAME_QUEUE_MAX_PLAYERS) {
            dequeueGame();
            return;
        }

        // Check if we should start a queue timer
        if (players.size() >= GameConstants.GAME_QUEUE_MIN_PLAYERS) {
            startQueueTimer();
        } else {
            queueAnnounce(ChatUtil.toComponent("&cNot enough players to start a game! Waiting for more players..."));
        }
    }

    private static void displayBookToast(Player player) {
        Book book = Book.builder()
                .title(Component.text("Weird zombie game"))
                .pages(
                        ChatUtil.toComponent(
                                """
                                &r&b&lWelcome to a game!
                                
                                &r&0This game is a zombie survival game where you build up weapons with upgrades to defend against hordes of zombies.
                                
                                &r&0&oMade by CoPokBl, Calcilore, Krystilize and EclipsedMango""")
                ).build();

        player.openBook(book);
    }

    public @Nullable TestGame gameByInstance(Instance instance) {
        for (TestGame game : games) {
            if (game.instance().equals(instance)) {
                return game;
            }
        }
        return null;
    }

    public @Nullable TestGame gameByPlayer(Player player) {
        return gameByInstance(player.getInstance());
    }

    @Override
    public List<Feature<?>> features() {
        return List.of(
                // TODO: Remove admin commands in production
                new AdminCommandsFeature(),
                new FancyChatFeature(),
                new SignFeature(),
                new NoCollisionFeature()
        );
    }

    public record Config() { }
}
