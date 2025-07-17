package net.mangolise.testgame.mobs;

import net.mangolise.testgame.util.Utils;
import net.minestom.server.entity.*;
import net.minestom.server.entity.ai.TargetSelector;
import net.minestom.server.event.entity.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class AttackTargetSelector extends TargetSelector {
    private static final long TARGET_UPDATE_INTERVAL = 1000; // update target every second
    
    private final Predicate<AttackableMob> shouldTarget;
    
    public AttackTargetSelector(EntityCreature entityCreature, Predicate<AttackableMob> shouldTarget) {
        super(entityCreature);
        this.shouldTarget = shouldTarget;
        entityCreature.eventNode().addListener(EntityTickEvent.class, event -> this.tick());
    }
    
    private long lastTargetUpdate = 0;

    @Override
    public @Nullable Entity findTarget() {
        // find the closest attackable mob
        return Utils.fastClosestEntity(entityCreature.getInstance(), entityCreature.getPosition(), this::shouldTarget);
    }

    private void tick() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = currentTime - lastTargetUpdate;
        if (timeSinceLastUpdate < TARGET_UPDATE_INTERVAL) {
            return;
        }

        // find the closest attackable mob and set it as target
        var attackable = Utils.fastClosestEntity(entityCreature.getInstance(), entityCreature.getPosition(), this::shouldTarget);
        lastTargetUpdate = currentTime + (long) (Math.random() * TARGET_UPDATE_INTERVAL + (TARGET_UPDATE_INTERVAL * 0.5)); // randomize a bit to avoid updating every entity at the same time
        entityCreature.setTarget(attackable);
    }
    
    private boolean shouldTarget(Entity entity) {
        if (!(entity instanceof AttackableMob mob)) return false;
        if (entity.isRemoved()) return false;
        if (!shouldTarget.test(mob)) return false;
        if (entity instanceof LivingEntity living && living.isDead()) return false;
        if (entity instanceof Player player && player.getGameMode() != GameMode.ADVENTURE) return false;
        return true;
    }
}
