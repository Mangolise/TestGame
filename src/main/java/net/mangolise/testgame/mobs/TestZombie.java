package net.mangolise.testgame.mobs;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.goal.FollowTargetGoal;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;

import java.time.Duration;
import java.util.List;

public class TestZombie extends HostileEntity {
    public TestZombie() {
        super(EntityType.ZOMBIE);

        this.addAIGroup(
                List.of(
                        new MeleeAttackGoal(this, 1.0, Duration.ofSeconds(1)),
                        new FollowTargetGoal(this, Duration.ofSeconds(1).plus(Duration.ofMillis((long) (1000.0 * Math.random()))))
                ),
                List.of(
                        // TODO: after yummy food on toast (jam) Make a guthib issue to make this not needed
                        new TargetTargetSelector(this)
                )
        );
    }

    @Override
    public void doTickUpdate(long time) {
    }
}
