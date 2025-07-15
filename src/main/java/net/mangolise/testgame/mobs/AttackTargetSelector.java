package net.mangolise.testgame.mobs;

import net.mangolise.testgame.combat.Attack;
import net.mangolise.testgame.util.Utils;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ai.TargetSelector;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class AttackTargetSelector extends TargetSelector {
    private static final long TARGET_UPDATE_INTERVAL = 1000; // update target every second
    
    private final Predicate<AttackableMob> shouldTarget;
    
    public AttackTargetSelector(EntityCreature entityCreature, Predicate<AttackableMob> shouldTarget) {
        super(entityCreature);
        this.shouldTarget = shouldTarget;
    }
    
    private long lastTargetUpdate = 0;

    @Override
    public @Nullable Entity findTarget() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - lastTargetUpdate;
        if (timeSinceLastUpdate < TARGET_UPDATE_INTERVAL) {
            return entityCreature.getTarget();
        }

        // find the closest attackable mob and set it as target
        var attackable = Utils.fastClosestEntity(entityCreature.getInstance(), entityCreature.getPosition(),
                entity -> entity instanceof AttackableMob mob && shouldTarget.test(mob));
        lastTargetUpdate = currentTime + (long) (Math.random() * TARGET_UPDATE_INTERVAL); // randomize a bit to avoid updating every entity at the same time
        entityCreature.setTarget(attackable);
        return attackable;
    }
}
