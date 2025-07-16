package net.mangolise.testgame.mobs;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.combat.AttackSystem;
import net.mangolise.testgame.combat.weapons.MaceWeapon;
import net.mangolise.testgame.combat.weapons.Weapon;
import net.mangolise.testgame.util.Utils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.ai.goal.FollowTargetGoal;
import net.minestom.server.entity.ai.goal.MeleeAttackGoal;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

public class JacobEntity extends HostileEntity implements PlayerTeam {
    
    private final Weapon weapon;
    private final Attack attack;
    private final int ticksToLive;
    
    public JacobEntity(Weapon weapon, Attack attack, int ticksToLive) {
        super(EntityType.GOAT);
        this.weapon = weapon;
        this.attack = attack;
        this.ticksToLive = ticksToLive;

        this.getAttribute(Attribute.MAX_HEALTH).setBaseValue(5.0);

        this.addAIGroup(
                List.of(
                        new MeleeAttackGoal(this, 3.0, Duration.ofMillis(250)),
                        new FollowTargetGoal(this, Duration.ofSeconds(1).plus(Duration.ofMillis((long) (1000.0 * Math.random()))))
                ),
                List.of(
                        // TODO: after yummy food on toast (jam) Make a guthib issue to make this not needed
                        new AttackTargetSelector(this, attack::canTarget)
                )
        );
    }

    @Override
    public void doTickUpdate(long time) {
        if (this.isDead()) {
            return;
        }
        
        // Shrink a little
        double scale = 1.0 - (this.ticksToLive - this.getAliveTicks()) / (double) this.ticksToLive;
        this.getAttribute(Attribute.SCALE).addModifier(new AttributeModifier("old_age", -scale, AttributeOperation.ADD_VALUE));

        if (this.ticksToLive <= this.getAliveTicks()) {
            // If the entity has reached it's time to live, DIE
            this.remove();
        }
    }

    @Override
    public void attack(@NotNull Entity target, boolean swingHand) {
        AttackSystem.instance(this.instance).use(this, weapon, tags -> {
            tags.setTag(Attack.USER, this);
            tags.setTag(Attack.TARGET, target);
            tags.setTag(MaceWeapon.IS_LAUNCH_ATTACK, true);
        });
    }
}
