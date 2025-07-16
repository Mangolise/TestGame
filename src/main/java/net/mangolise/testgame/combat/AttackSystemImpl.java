package net.mangolise.testgame.combat;

import net.mangolise.testgame.combat.mods.Mod;
import net.mangolise.testgame.combat.weapons.*;
import net.mangolise.testgame.mobs.ShooterMob;
import net.mangolise.testgame.mobs.spawning.SpawnSystem;
import net.mangolise.testgame.mobs.MeleeJockeyMob;
import net.mangolise.testgame.mobs.MeleeMob;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.monster.zombie.DrownedMeta;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

public final class AttackSystemImpl implements AttackSystem {

    private final Map<Entity, Map<Class<? extends Mod>, Mod>> modifierNodes = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Tag<Set<Weapon>> COOLDOWNS = Tag.Transient("testgame.attacksystem.weapon_cooldowns");
    private final Instance instance;

    public AttackSystemImpl(Instance instance) {
        this.instance = instance;
    }

    public Map<Class<? extends Mod>, Mod> getModifiers(Entity entity) {
        return Collections.unmodifiableMap(modifierNodes.computeIfAbsent(entity, k -> new HashMap<>()));
    }

    public void upgradeMod(Entity entity, Class<? extends Mod> modClass, Function<Mod, Integer> levelSupplier) {
        Map<Class<? extends Mod>, Mod> modifiers = modifierNodes.computeIfAbsent(entity, k -> new HashMap<>());
        Mod mod = modifiers.get(modClass);

        if (mod.level() >= mod.maxLevel()) {
            throw new IllegalArgumentException("Mod is already max level.");
        }

        mod.onRemove(entity);

        Mod newMod = Mod.getFactory(modClass).create(levelSupplier.apply(mod));
        modifiers.put(modClass, newMod);
        newMod.onAdd(entity);
    }

    public void use(Entity entity, Weapon weapon) {
        use(entity, weapon, tags -> {});
    }

    public void use(Entity entity, Weapon weapon, Consumer<Attack> tags) {
        // entity needs a cooldown, we cant use defaults because defaults do not set the value
        if (!entity.hasTag(COOLDOWNS)) {
            entity.setTag(COOLDOWNS, new HashSet<>());
        }

        Set<Weapon> cooldowns = entity.getTag(COOLDOWNS);
        if (cooldowns.contains(weapon)) {
            return;
        }

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

        // get cooldown, we use the largest cooldown from all of the attacks
        if (attacks.isEmpty()) {
            entity.getTag(COOLDOWNS).remove(weapon);
            return;
        }

        double cooldown = attacks.stream()
                .map(attack -> attack.getTag(Attack.COOLDOWN))
                .max(Double::compareTo).get();

        cooldowns.add(weapon);
        MinecraftServer.getSchedulerManager().scheduleTask(() -> {
            cooldowns.remove(weapon);
            if (cooldowns.isEmpty()) {
                entity.removeTag(COOLDOWNS);
            }
        }, TaskSchedule.duration(Duration.ofNanos((long)(cooldown * 1e+9f))), TaskSchedule.stop());
    }

    @Override
    public boolean add(Entity entity, Mod mod) {
        Map<Class<? extends Mod>, Mod> nodes = modifierNodes.computeIfAbsent(entity, k -> new HashMap<>());
        Class<? extends Mod> modClass = mod.getClass();
        
        if (nodes.containsKey(modClass)) {
            return false; // Entity already has this mod
        }

        nodes.put(modClass, mod);
        mod.onAdd(entity);
        return true; // Mod added successfully
    }

    private static List<Attack.Node> sort(List<Attack.Node> unsorted) {
        List<Attack.Node> sorted = new ArrayList<>(unsorted);
        sorted.sort(Comparator.comparingDouble(Attack.Node::priority).reversed());
        return sorted;
    }

    public boolean isWeapon(ItemStack item, Weapon weapon) {
        return item.hasTag(Weapon.WEAPON_TAG) && item.getTag(Weapon.WEAPON_TAG).equals(weapon.getId());
    }
    
    public void init() {
        instance.eventNode().addListener(PlayerHandAnimationEvent.class, e -> {
            var player = e.getPlayer();

            if (e.getHand() == PlayerHand.OFF) {
                return; // ignore off-hand interactions
            }
            var itemUseHand = player.getItemUseHand() == null ? PlayerHand.MAIN : player.getItemUseHand();
            var item = player.getItemInHand(itemUseHand);

            onSwing(player, item);
        });

        instance.eventNode().addListener(PlayerUseItemEvent.class, e -> {
            var player = e.getPlayer();
            onSwing(player, e.getItemStack());

            if (isWeapon(player.getItemInHand(e.getHand()), Weapon.weapon(MaceWeapon.class))) { // mace
                MaceWeapon maceWeapon = new MaceWeapon();
                AttackSystem.instance(player.getInstance()).use(player, maceWeapon, tags -> {
                    tags.setTag(Attack.USER, player);
                    tags.setTag(MaceWeapon.IS_LAUNCH_ATTACK, true);
                });
            }
        });

        instance.eventNode().addListener(EntityAttackEvent.class, e -> {
            if (!(e.getEntity() instanceof Player player)) {
                return;
            }

            if (isWeapon(player.getItemInHand(PlayerHand.MAIN), Weapon.weapon(MaceWeapon.class))) { // mace
                MaceWeapon maceWeapon = new MaceWeapon();
                AttackSystem.instance(player.getInstance()).use(player, maceWeapon, tags -> {
                    tags.setTag(Attack.USER, player);
                    tags.setTag(Attack.TARGET, e.getTarget());
                    tags.setTag(MaceWeapon.IS_LAUNCH_ATTACK, false);
                });
            }
        });

        instance.eventNode().addListener(PlayerEntityInteractEvent.class, e -> {
            Player player = e.getPlayer();

            // staff
            if (isWeapon(player.getItemInHand(e.getHand()), Weapon.weapon(StaffWeapon.class))) {
                StaffWeapon staffWeapon = new StaffWeapon();
                AttackSystem.instance(instance).use(player, staffWeapon, tags -> {
                    tags.setTag(Attack.USER, player);
                    tags.setTag(Attack.TARGET, e.getTarget());
                });
            }
        });
    }

    private void onSwing(Player player, ItemStack item) {
        // bow
        if (isWeapon(item, Weapon.weapon(BowWeapon.class))) {
            BowWeapon bowWeapon = new BowWeapon();
            AttackSystem.instance(player.getInstance()).use(player, bowWeapon, tags -> {
                tags.setTag(Attack.USER, player);
            });
        } else if (isWeapon(item, Weapon.weapon(CannonBallBallWeapon.class))) { // CannonBallBall
            CannonBallBallWeapon weapon = new CannonBallBallWeapon();
            AttackSystem.instance(player.getInstance()).use(player, weapon, tags -> tags.setTag(Attack.USER, player));
        } else if (isWeapon(item, Weapon.weapon(SnakeWeapon.class))) { // staff
            SnakeWeapon snakeWeapon = new SnakeWeapon();
            AttackSystem.instance(player.getInstance()).use(player, snakeWeapon, tags -> {
                tags.setTag(Attack.USER, player);
            });
        } else if (item.material() == Material.ZOMBIE_SPAWN_EGG) { // spawn zombies
            for (int i = 0; i < 128; i++) {
                MeleeMob entity = new MeleeMob(EntityType.HUSK);
                double speedMultiplier = 0.1 + Math.random() * Math.random() * 0.4;
                double scale = Math.pow(1.0 / (0.8 * (speedMultiplier + 1.0)), 1.0 / 0.3);

                entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedMultiplier);

                entity.setTarget(player);
                entity.getAttribute(Attribute.SCALE).setBaseValue(scale);
                SpawnSystem.spawn(player.getInstance(), entity);
            }
        } else if (item.material() == Material.CHICKEN_SPAWN_EGG) {
            for (int i = 0; i < 128; i++) {
                MeleeJockeyMob entity = new MeleeJockeyMob(EntityType.CHICKEN, EntityType.DROWNED);
                entity.getRider().editEntityMeta(DrownedMeta.class, m -> m.setBaby(true));

                double statScaling = 1.5;

                double speedMultiplier = 0.1 + Math.random() * Math.random() * 0.4;
                double scale = Math.pow(1.0 / (0.8 * (speedMultiplier + 1.0)), 1.0 / 0.3);

                speedMultiplier *= statScaling;
                scale *= statScaling;

                entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedMultiplier);

                entity.setTarget(player);
                entity.getAttribute(Attribute.SCALE).setBaseValue(scale);
                SpawnSystem.spawn(player.getInstance(), entity);
            }
        } else if (item.material() == Material.SKELETON_SPAWN_EGG) {
            for (int i = 0; i < 1; i++) {
                ShooterMob entity = new ShooterMob(EntityType.SKELETON);

                double statScaling = 1.5;

                double speedMultiplier = 0.1 + Math.random() * Math.random() * 0.4;
                double scale = Math.pow(1.0 / (0.8 * (speedMultiplier + 1.0)), 1.0 / 0.3);

                speedMultiplier *= statScaling;
                scale *= statScaling;

                entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speedMultiplier);

                entity.setTarget(player);
                entity.getAttribute(Attribute.SCALE).setBaseValue(scale);
                SpawnSystem.spawn(player.getInstance(), entity);
            }
        }
    }
}
