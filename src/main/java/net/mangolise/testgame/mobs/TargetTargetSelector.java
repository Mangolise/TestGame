package net.mangolise.testgame.mobs;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.ai.TargetSelector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TargetTargetSelector extends TargetSelector {

    public TargetTargetSelector(@NotNull EntityCreature entityCreature) {
        super(entityCreature);
    }

    @Override
    public @Nullable Entity findTarget() {
        Entity target = entityCreature.getTarget();
        return (target == null || target.isRemoved()) ? null : target;
    }
}
