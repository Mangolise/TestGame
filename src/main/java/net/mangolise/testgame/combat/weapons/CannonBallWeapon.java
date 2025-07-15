package net.mangolise.testgame.combat.weapons;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.events.ProjectileCollideAnyEvent;
import net.mangolise.testgame.events.ProjectileCollideEntityEvent;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.projectiles.VanillaProjectile;
import net.mangolise.testgame.util.ThrottledScheduler;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.function.Consumer;

public record CannonBallWeapon(int level) implements Weapon {
    @Override
    public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
        attack.setTag(Attack.DAMAGE, 8.0 + level * 2.0);
        attack.setTag(Attack.CRIT_CHANCE, 0.5 + level * 0.1);
        attack.setTag(Attack.COOLDOWN, 1.0 - level * 0.2);

        next.accept(attack);
    }

    @Override
    public void doWeaponAttack(List<Attack> attacks) {
        for (Attack attack : attacks) {
            LivingEntity user = (LivingEntity) attack.getTag(Attack.USER);
            if (user == null) {
                throw new IllegalStateException("CannonBallBall attack called without a user set in the tags.");
            }

            double playerScale = user.getAttribute(Attribute.SCALE).getValue();
            Pos position = user.getPosition().withPitch(0).add(0, user.getEyeHeight() * playerScale, 0);
            Vec velocity = user.getPosition().direction().mul(24);
            
            double inaccuracy = attacks.size() - 1.0; // more attacks, more inaccuracy
            velocity = velocity.add((Math.random() - 0.5) * inaccuracy, (Math.random() - 0.5) * inaccuracy, (Math.random() - 0.5) * inaccuracy);

            createCannonBall(user, user.getInstance(), attack, position, velocity, Vec.ONE, level);
        }
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

        attack = attack.copy(false);
        attack.updateTag(Attack.DAMAGE, damage -> damage * scale.x());

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
        if (event instanceof ProjectileCollideEntityEvent eEvent) {
            if (!(eEvent.getTarget() instanceof AttackableMob target)) {
                event.setCancelled(true);
                return;
            }

            target.applyAttack(DamageType.FALLING_ANVIL, attack);
        }

        if (splitCount <= 0) {
            return;
        }

        Instance instance = cannonBall.getInstance();

        // explosion
        double scale = ((BlockDisplayMeta)cannonBall.getEntityMeta()).getScale().x();
        Attack explosionAttack = attack.copy(false);
        explosionAttack.updateTag(Attack.DAMAGE, damage -> damage * 0.5);

        for (Entity entity : instance.getNearbyEntities(cannonBall.getPosition(), 3 * scale)) {
            if (entity instanceof AttackableMob mob) {
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
