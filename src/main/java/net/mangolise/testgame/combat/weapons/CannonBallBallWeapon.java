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
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Consumer;

public record CannonBallBallWeapon() implements Weapon {

    public static final Tag<Integer> SPLIT_COUNT = Tag.Integer("testgame.attack.cannonballball.split_count").defaultValue(1);

    @Override
    public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
        attack.setTag(Attack.DAMAGE, 8.0);
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

            createCannonBall(user, user.getInstance(), attack, position, velocity, Vec.ONE, attack.getTag(SPLIT_COUNT));
        }
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.of(Material.HEAVY_CORE)
                .withTag(Weapon.WEAPON_TAG, getId())
                .with(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .withCustomName(ChatUtil.toComponent("&r&6&lCannon Ball Ball"))
                .withLore(
                        ChatUtil.toComponent("&7A heavy ball that splits into smaller balls on impact."),
                        ChatUtil.toComponent("&7It deals large area of effect damage in a chain reaction."));
    }

    @Override
    public String getId() {
        return "cannon_ball";
    }

    private void createCannonBall(LivingEntity user, Instance instance, Attack attack, Pos position, Vec velocity, Vec scale, int splitCount) {
        VanillaProjectile cannonBall = new VanillaProjectile(user, EntityType.BLOCK_DISPLAY);
        cannonBall.editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(Block.SMOOTH_BASALT);
            meta.setScale(scale);
            meta.setPosRotInterpolationDuration(1);
            meta.setTranslation(new Vec(-0.5));
        });

        cannonBall.setBoundingBox(new BoundingBox(Vec.ZERO, scale));

        cannonBall.setInstance(instance, position);
        cannonBall.setVelocity(velocity);

        final Attack finalAttack = attack;
        cannonBall.eventNode().addListener(EventListener.builder(ProjectileCollideAnyEvent.class)
                .handler(event -> onCannonBallCollide(event, user, cannonBall, finalAttack, splitCount))
                .expireWhen(ignored -> cannonBall.isRemoved())
                .build());
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }

    private void onCannonBallCollide(ProjectileCollideAnyEvent event, LivingEntity user, VanillaProjectile cannonBall, Attack attack, int splitCount) {
        double scale = ((BlockDisplayMeta)cannonBall.getEntityMeta()).getScale().x();

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
        final int CHILD_COUNT = 6;
        final double CHILD_SCALE_MOD = Math.pow(CHILD_COUNT, 1.0 / 3.0); // is this math right, idk
        for (int i = 0; i < CHILD_COUNT; i++) {
            double rotation = i * Math.TAU / CHILD_COUNT;
            Pos position = cannonBall.getPosition().withYaw((float) rotation);
            Vec velocity = new Vec(6, 12, 0).rotateAroundY(rotation);

            instance.playSound(Sound.sound(Key.key("minecraft:entity.generic.explode"), Sound.Source.PLAYER, 0.1f, 2.5f + (float) Math.random() * 0.5f), position);

            ThrottledScheduler.use(instance, "cannonball-weapon-ball-attack", 4, () -> {
                createCannonBall(user, instance, attack, position, velocity, new Vec(scale / CHILD_SCALE_MOD), splitCount - 1);
            });
        }
    }
}
