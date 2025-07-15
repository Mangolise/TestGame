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
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.commands.AcceptPartyInviteCommand;
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
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class LobbyGame extends BaseGame<LobbyGame.Config> {
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

    public LobbyGame(Config config) {
        super(config);
    }

    public static void CreateRegistryEntries() {
        DimensionType dimension = DimensionType.builder().ambientLight(15).build();
        MinecraftServer.getDimensionTypeRegistry().register("lobby-dimension", dimension);
    }

    @Override
    public void setup() {
        super.setup();

        MinecraftServer.getCommandManager().register(new AcceptPartyInviteCommand(this));

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
            Player player = e.getPlayer();
            e.setSpawningInstance(world);

            player.setGameMode(GameMode.ADVENTURE);
            player.setRespawnPoint(new Pos(5, 66.5, 6));

            // Reset all attributes to their default values
            for (AttributeInstance attribute : player.getAttributes()) {
                attribute.clearModifiers();
            }
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);  // Set by the game, so we must manually reset it

            player.getInventory().clear();
        });

        world.eventNode().addListener(PlayerSpawnEvent.class, e -> {
            e.getPlayer().sendMessage(ChatUtil.toComponent("&aWelcome!"));

            // TODO: Should this be enabled for production?
//            displayBookToast(e.getPlayer());

            giveRegularItems(e.getPlayer());
        });

        world.eventNode().addListener(InventoryPreClickEvent.class, e -> {
            e.setCancelled(true);
            inventoryItemInteract(e.getPlayer(), e.getClickedItem());
        });

        world.eventNode().addListener(PlayerUseItemEvent.class, e -> {
            inventoryItemInteract(e.getPlayer(), e.getItemStack());
        });

        world.eventNode().addListener(PlayerEntityInteractEvent.class, e -> {
            if (!e.getPlayer().getItemInMainHand().equals(invitePartyMemberItem)) {
                return;
            }

            if (!(e.getTarget() instanceof Player target)) {
                return;
            }

            Set<Player> invitees = e.getPlayer().getTag(PARTY_MEMBER_INVITES_TAG);
            if (!invitees.add(target)) {
                // it was already there
                e.getPlayer().sendMessage(ChatUtil.toComponent("&cYou have already invited &6" + target.getUsername() + "&c to your party."));
                return;
            }

            e.getPlayer().sendMessage(ChatUtil.toComponent("&aYou have invited &6" + target.getUsername() + "&a to your party!"));
            target.sendMessage(ChatUtil.toComponent("&6" + e.getPlayer().getUsername() + "&a has invited you to their party!"));

            Component acceptMessage = Component.text("Click here to accept the invite.")
                    .color(NamedTextColor.GOLD)
                    .hoverEvent(HoverEvent.showText(ChatUtil.toComponent("&aClick this to join their party!")))
                    .clickEvent(ClickEvent.runCommand("/acceptpartyinvite " + e.getPlayer().getUsername()));
            target.sendMessage(acceptMessage);
        });
    }

    private void giveRegularItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItemStack(0, joinQueueItem);
        player.getInventory().setItemStack(1, playSoloItem);
        player.getInventory().setItemStack(2, playWithPartyItem);
        player.getInventory().setItemStack(8, infoItem);
    }

    private void inventoryItemInteract(Player player, ItemStack item) {
        if (item.equals(infoItem)) {
            displayBookToast(player);
        } else if (item.equals(playSoloItem)) {
            player.sendMessage(ChatUtil.toComponent("&aJoining a solo game..."));
            config.startGameMethod.accept(new Player[]{player});
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
            config.startGameMethod.accept(partyMembers.toArray(new Player[0]));
        } else if (item.equals(leavePartyItem)) {
            leaveParty(player);
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
        queue.add(player);
        player.sendMessage(ChatUtil.toComponent("&aYou have joined the queue!"));
        player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP.key(), Sound.Source.MASTER, 1.0f, 1.0f));

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
        config.startGameMethod.accept(players.toArray(new Player[0]));

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

    public record Config(Consumer<Player[]> startGameMethod) { }
}
