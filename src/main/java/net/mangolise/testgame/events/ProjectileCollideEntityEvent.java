package net.mangolise.testgame.events;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class ProjectileCollideEntityEvent extends ProjectileCollideWithAnyEvent {
    private final @NotNull Entity target;

    public ProjectileCollideEntityEvent(@NotNull Entity projectile, @NotNull Pos position, @NotNull Entity target) {
        super(projectile, position);
        this.target = target;
    }

    public @NotNull Entity getTarget() {
        return target;
    }
}
