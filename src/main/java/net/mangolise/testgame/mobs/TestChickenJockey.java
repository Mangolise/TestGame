package net.mangolise.testgame.mobs;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.weapons.CannonBallWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.goal.FollowTargetGoal;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TestChickenJockey extends HostileEntity {
    
    private final TestZombie rider;
    
    public TestChickenJockey() {
        super(EntityType.CHICKEN);

        this.addAIGroup(
                List.of(
                        new FollowTargetGoal(this, Duration.ofSeconds(1).plus(Duration.ofMillis((long) (1000.0 * Math.random())))),
                        new MeleeAttackGoal(this, 1.0, Duration.ofSeconds(1))
                ),
                List.of(
                        new TargetTargetSelector(this)
                )
        );
        
        // Create the rider entity (a zombie)
        this.rider = new TestZombie();
    }

    @Override
    public void setTarget(@Nullable Entity target) {
        super.setTarget(target);
        rider.setTarget(target);
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        return super.setInstance(instance, spawnPosition).thenAccept(ignored -> {
            rider.setInstance(instance, spawnPosition).thenAccept(ignored2 -> {
                // after the rider is set in the instance, attach it to the chicken
                this.addPassenger(rider);
            });
        });
    }

    @Override
    public void doTickUpdate(long time) {
    }

    Weapon weapon = new CannonBallWeapon(1);

    @Override
    public void attack(@NotNull Entity target, boolean swingHand) {
        super.attack(target, swingHand);

        AttackSystem.instance(target.getInstance()).use(this, weapon, tags -> {
            tags.setTag(Attack.USER, this);
            tags.setTag(Attack.TARGET, target);
        });
    }
}
