package net.mangolise.testgame.mobs;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.weapons.DirectDamageWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class MeleeMob extends HostileEntity {
    public Weapon weapon = new DirectDamageWeapon();

    public MeleeMob(EntityType type) {
        super(type);

        this.addAIGroup(
                List.of(
                        new FollowTargetFixedGoal(this, Duration.ofSeconds(1).plus(Duration.ofMillis((long) (1000.0 * Math.random())))),
                        new MeleeAttackGoal(this, 1.0, Duration.ofSeconds(1))
                ),
                List.of(
                        new AttackTargetSelector(this, entity -> entity instanceof PlayerTeam)
                )
        );
    }

    @Override
    public void doTickUpdate(long time) {
    }

    @Override
    public void attack(@NotNull Entity target, boolean swingHand) {
        super.attack(target, swingHand);

        AttackSystem.instance(target.getInstance()).use(this,weapon, tags -> {
            tags.setTag(Attack.USER, this);
            tags.setTag(Attack.TARGET, target);
            tags.setTag(Attack.DAMAGE, 1.0);
        });
    }
}
