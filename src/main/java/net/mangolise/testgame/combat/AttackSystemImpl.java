package net.mangolise.testgame.combat;

import net.mangolise.testgame.combat.mods.Mod;
import net.mangolise.testgame.combat.weapons.*;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.mobs.TestZombie;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.Material;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class AttackSystemImpl implements AttackSystem {
    
    private final Map<Entity, Map<Class<? extends Mod>, Mod>> modifierNodes = Collections.synchronizedMap(new WeakHashMap<>());

    public void use(Entity entity, Weapon weapon) {
        use(entity, weapon, tags -> {});
    }

    public void use(Entity entity, Weapon weapon, Consumer<Attack> tags) {
        Map<Class<? extends Mod>, Mod> nodes = modifierNodes.computeIfAbsent(entity, k -> new HashMap<>());
        List<Attack.Node> unsorted = new ArrayList<>(nodes.values());
        List<Attack.Node> sorted = sort(unsorted);

//        System.out.printf("Went from %s mods to %s sorted nodes.%n", unsorted, sorted);
        
        Attack initialAttack = new Attack(ThreadLocalRandom.current().nextInt());
        tags.accept(initialAttack);
        
        List<Attack> attacks = new ArrayList<>();

        weapon.attack(initialAttack, next -> attacks.add(next.copy(false)));
        
        for (Attack.Node node : sorted) {
            var attacksCopy = List.copyOf(attacks);
            attacks.clear();
            for (Attack attack : attacksCopy) {
                node.attack(attack, next -> attacks.add(next.copy(false)));
            }
        }
        
        // finish the attack processing
        weapon.doWeaponAttack(List.copyOf(attacks));
    }

    @Override
    public boolean add(Entity entity, Mod mod) {
        Map<Class<? extends Mod>, Mod> nodes = modifierNodes.computeIfAbsent(entity, k -> new HashMap<>());
        Class<? extends Mod> modClass = mod.getClass();
        
        if (nodes.containsKey(modClass)) {
            return false; // Entity already has this mod
        }
        
        nodes.put(modClass, mod);
        return true; // Mod added successfully
    }

    private static List<Attack.Node> sort(List<Attack.Node> unsorted) {
        List<Attack.Node> sorted = new ArrayList<>(unsorted);
        sorted.sort(Comparator.comparingDouble(Attack.Node::priority).reversed());
        return sorted;
    }
    
    public void init() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent.class, e -> {
            var player = e.getPlayer();

            if (e.getHand() == PlayerHand.OFF) {
                return; // ignore off-hand interactions
            }
            var itemUseHand = player.getItemUseHand() == null ? PlayerHand.MAIN : player.getItemUseHand();
            var item = player.getItemInHand(itemUseHand);

            onSwing(player, item.material());
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, e -> {
            var player = e.getPlayer();
            onSwing(player, e.getItemStack().material());
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerEntityInteractEvent.class, e -> {
            Player player = e.getPlayer();

            // staff
            if (player.getItemInHand(e.getHand()).material() == Material.BLAZE_ROD) {
                StaffWeapon staffWeapon = new StaffWeapon(1);
                AttackSystem.INSTANCE.use(player, staffWeapon, tags -> {
                    tags.setTag(Attack.USER, player);
                    tags.setTag(StaffWeapon.STAFF_USER, player);
                    if (e.getTarget() instanceof AttackableMob target) {
                        tags.setTag(StaffWeapon.HIT_ENTITY, target);
                    }
                });
            }
        });
    }

    private void onSwing(Player player, Material material) {
        // bow
        if (material == Material.BOW) {
            BowWeapon bowWeapon = new BowWeapon(1);
            AttackSystem.INSTANCE.use(player, bowWeapon, tags -> {
                tags.setTag(Attack.USER, player);
                tags.setTag(BowWeapon.BOW_USER, player);
            });
        } else if (material == Material.SUNFLOWER) { // CannonBallBall
            CannonBallWeapon weapon = new CannonBallWeapon(2);
            AttackSystem.INSTANCE.use(player, weapon, tags -> tags.setTag(Attack.USER, player));
        } else if (material == Material.STICK) { // staff
            SnakeWeapon snakeWeapon = new SnakeWeapon(1);
            AttackSystem.INSTANCE.use(player, snakeWeapon, tags -> {
                tags.setTag(Attack.USER, player);
                tags.setTag(StaffWeapon.STAFF_USER, player);
            });
        } else if (material == Material.ZOMBIE_SPAWN_EGG) { // spawn zombies
            for (int i = 0; i < 128; i++) {
                TestZombie entity = new TestZombie();
                double speedMultiplier = 0.1 + Math.random() * Math.random() * 0.3;

                entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedMultiplier);

                entity.setInstance(player.getInstance(), player.getPosition().add(Math.random() * 8.0, Math.random() * 64.0, Math.random() * 8.0));
                entity.setTarget(player);
                entity.getAttribute(Attribute.SCALE).setBaseValue(0.5 * (1.0 / Math.pow(speedMultiplier, 0.4)));
            }
        }
    }
}
