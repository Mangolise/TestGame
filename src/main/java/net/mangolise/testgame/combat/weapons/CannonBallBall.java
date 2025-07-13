package net.mangolise.testgame.combat.weapons;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.events.ProjectileCollideAnyEvent;
import net.mangolise.testgame.events.ProjectileCollideEntityEvent;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.projectiles.VanillaProjectile;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.*;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Consumer;

public record CannonBallBall(int level) implements Attack.Node {
    @Override
    public void attack(Attack attack, @UnknownNullability Consumer<Attack> next) {
        if (next != null) {
            attack.setTag(Attack.DAMAGE, 8.0 + level * 2.0);
            attack.setTag(Attack.CRIT_CHANCE, 0.5 + level * 0.1);

            next.accept(attack);
            return;
        }

        // next == null means that we perform the attack
        Player user = (Player) attack.getTag(Attack.USER);
        if (user == null) {
            throw new IllegalStateException("BowWeapon attack called without a user set in the tags.");
        }

        // spawn single arrow
        var instance = user.getInstance();

        var cannonBall = new VanillaProjectile(user, EntityType.BLOCK_DISPLAY);
        setMeta(cannonBall, Vec.ONE);

        var playerScale = user.getAttribute(Attribute.SCALE).getValue();
        cannonBall.setInstance(instance, user.getPosition().withPitch(0).add(0, user.getEyeHeight() * playerScale, 0));

        cannonBall.setVelocity(user.getPosition().direction().mul(24));

        cannonBall.eventNode().addListener(EventListener.builder(ProjectileCollideAnyEvent.class)
                .handler(event -> onCannonBallCollide(event, user, cannonBall, attack, level))
                .expireCount(1)
                .expireWhen(ignored -> cannonBall.isRemoved())
                .build());
    }

    private void setMeta(VanillaProjectile projectile, Vec scale) {
        projectile.editEntityMeta(BlockDisplayMeta.class, meta -> {
            meta.setBlockState(Block.SMOOTH_BASALT);
            meta.setScale(scale);
            meta.setPosRotInterpolationDuration(1);
            meta.setTranslation(new Vec(-0.5));
        });
    }

    @Override
    public double priority() {
        return PRIORITY_WEAPON;
    }

    private void onCannonBallCollide(ProjectileCollideAnyEvent event, Player user, VanillaProjectile cannonBall, Attack attack, int splitCount) {
        user.sendMessage("hi");
        if (event instanceof ProjectileCollideEntityEvent eEvent) {
            if (!(eEvent.getTarget() instanceof AttackableMob target)) {
                event.setCancelled(true);
                return;
            }

            target.applyAttack(DamageType.FALLING_ANVIL, attack);
        }

        Instance instance = user.getInstance();

        if (splitCount <= 0) {
            return;
        }

        cannonBall.remove();

        final int CHILD_COUNT = 6;
        for (int i = 0; i < CHILD_COUNT; i++) {
            var child = new VanillaProjectile(user, EntityType.BLOCK_DISPLAY);
            setMeta(child, new Vec(0.25));

            child.setVelocity(new Vec(6, 12, 0).rotateAroundY(i * Math.TAU / CHILD_COUNT));
            child.setInstance(instance, cannonBall.getPosition());

            child.eventNode().addListener(EventListener.builder(ProjectileCollideAnyEvent.class)
                    .handler(e -> onCannonBallCollide(e, user, child, attack, splitCount - 1))
                    .expireCount(1)
                    .expireWhen(ignored -> child.isRemoved())
                    .build());
        }
    }
}
