package net.mangolise.testgame.mobs;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.util.ThrottledScheduler;
import net.mangolise.testgame.util.Throttler;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.ai.goal.FollowTargetGoal;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.registry.RegistryKey;

import java.time.Duration;
import java.util.List;

public class TestZombie extends HostileEntity {
    public TestZombie() {
        super(EntityType.ZOMBIE);

        this.addAIGroup(
                List.of(
                        new FollowTargetGoal(this, Duration.ofSeconds(1).plus(Duration.ofMillis((long) (1000.0 * Math.random())))),
                        new MeleeAttackGoal(this, 1.0, Duration.ofSeconds(1))
                ),
                List.of()
        );
    }

    @Override
    public void doTickUpdate(long time) {
    }
}
