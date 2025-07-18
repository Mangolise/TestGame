package net.mangolise.testgame.combat.weapons;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.gamesdk.util.ChatUtil;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.events.ProjectileCollideAnyEvent;
import net.mangolise.testgame.events.ProjectileCollideEntityEvent;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.projectiles.VanillaProjectile;
import net.mangolise.testgame.util.ThrottledScheduler;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public record CannonBallBallWeapon() implements Weapon {

    public static final Tag<Integer> SPLIT_COUNT = Tag.Integer("testgame.attack.cannonballball.split_count").defaultValue(1);

    @Override
    public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
        attack.setTag(Attack.DAMAGE, 12.0);
        attack.setTag(Attack.CRIT_CHANCE, 0.5);
        attack.setTag(Attack.COOLDOWN, 1.0);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        for (Attack attack : attacks) {
            LivingEntity user = attack.getTag(Attack.USER);
            if (user == null) {
                throw new IllegalStateException("CannonBallBall attack called without a user set in the tags.");
            }

            double playerScale = user.getAttribute(Attribute.SCALE).getValue();
            Pos position = user.getPosition().withPitch(0).add(0, user.getEyeHeight() * playerScale, 0);
            Vec velocity = user.getPosition().direction().mul(24);
            
            double inaccuracy = attacks.size() - 1.0; // more attacks, more inaccuracy
            velocity = velocity.add((Math.random() - 0.5) * inaccuracy, (Math.random() - 0.5) * inaccuracy, (Math.random() - 0.5) * inaccuracy);

            int splitCount = attack.getTag(SPLIT_COUNT);
            createCannonBall(user, user.getInstance(), attack, position, velocity, Vec.ONE, splitCount, 6);
        }
    }

    @Override
    public ItemStack.Builder generateItem() {
        return ItemStack.builder(Material.HEAVY_CORE)
                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .customName(ChatUtil.toComponent("&r&6&lCannon Ball Ball"))
                .lore(
                        ChatUtil.toComponent("&7A heavy ball that splits into smaller balls on impact."),
                        ChatUtil.toComponent("&7It deals large area of effect damage in a chain reaction."),
                        ChatUtil.toComponent("&7Crits cause it to split even more.")
                )
                .set(DataComponents.ITEM_MODEL, "minecraft:cannon");
    }

    @Override
    public String getId() {
        return "cannon_ball";
    }

    private void createCannonBall(LivingEntity user, Instance instance, Attack attack, Pos position, Vec velocity, Vec scale, int splitCount, double childCount) {
        VanillaProjectile cannonBall = new VanillaProjectile(user, EntityType.ITEM_DISPLAY);
        cannonBall.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItemStack(ItemStack.of(Material.SMOOTH_BASALT).with(DataComponents.ITEM_MODEL, "minecraft:cannon_ball"));
            meta.setScale(scale);
            meta.setTranslation(new Vec(0, 0, 0));
            meta.setPosRotInterpolationDuration(1);
        });

        cannonBall.setBoundingBox(new BoundingBox(Vec.ZERO, scale));

        cannonBall.setInstance(instance, position);
        cannonBall.setVelocity(velocity);

        final Attack finalAttack = attack;
        cannonBall.eventNode().addListener(EventListener.builder(ProjectileCollideAnyEvent.class)
                .handler(event -> onCannonBallCollide(event, user, cannonBall, finalAttack, splitCount, childCount))
                .expireWhen(ignored -> cannonBall.isRemoved())
                .build());
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }

    private void onCannonBallCollide(ProjectileCollideAnyEvent event, LivingEntity user, VanillaProjectile cannonBall, Attack attack, int splitCount, double childCount) {
        double scale = ((AbstractDisplayMeta) cannonBall.getEntityMeta()).getScale().x();

        if (event instanceof ProjectileCollideEntityEvent eEvent) {
            if (!(eEvent.getTarget() instanceof AttackableMob target && attack.canTarget(target))) {
                event.setCancelled(true);
                return;
            }

            Attack attackCopy = attack.copy(false);
            attackCopy.updateTag(Attack.DAMAGE, damage -> damage * scale);
            target.applyAttack(DamageType.FALLING_ANVIL, attackCopy);
        }

        if (splitCount <= 0) {
            return;
        }

        Instance instance = cannonBall.getInstance();

        // explosion
        Attack explosionAttack = attack.copy(false);
        explosionAttack.updateTag(Attack.DAMAGE, damage -> damage * scale * 0.5);

        for (Entity entity : instance.getNearbyEntities(cannonBall.getPosition(), 3 * scale)) {
            if (entity instanceof AttackableMob mob && explosionAttack.canTarget(mob)) {
                mob.applyAttack(DamageType.PLAYER_EXPLOSION, explosionAttack);
            }
        }

        // split into children
        final double CHILD_SCALE_MOD = Math.pow(childCount, 1.0 / 3.0); // is this math right, idk
        Attack attackCopy = attack;

        for (int i = 0; i < (int) childCount; i++) {
            double rotation = i * Math.TAU / childCount;
            Pos position = cannonBall.getPosition().withYaw((float) rotation);
            Vec velocity = new Vec(6, 12, 0).rotateAroundY(rotation);

            instance.playSound(Sound.sound(Key.key("minecraft:entity.generic.explode"), Sound.Source.PLAYER, 0.1f, 2.5f + (float) Math.random() * 0.5f), position);

            Attack childAttack = attackCopy.copy(true);
            attackCopy = childAttack;

            int consumeCount = IntStream.range(0, attackCopy.sampleCrits()).anyMatch(j -> Math.random() < scale * 0.75) ? 0 : 1;

            ThrottledScheduler.use(instance, "cannonball-weapon-ball-attack", 4, () -> {
                createCannonBall(user, instance, childAttack, position, velocity, new Vec(scale / CHILD_SCALE_MOD), splitCount - consumeCount, childCount / (Math.random() + 1.0));
            });
        }
    }
}
