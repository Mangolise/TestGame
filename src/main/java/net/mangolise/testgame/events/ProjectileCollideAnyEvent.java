package net.mangolise.testgame.events;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.EntityInstanceEvent;
import net.minestom.server.event.trait.RecursiveEvent;
import org.jetbrains.annotations.NotNull;

public abstract class ProjectileCollideAnyEvent implements EntityInstanceEvent, CancellableEvent, RecursiveEvent {
    private final @NotNull Entity projectile;
    private final @NotNull Pos position;
    private boolean cancelled;

    public ProjectileCollideAnyEvent(@NotNull Entity projectile, @NotNull Pos position) {
        this.projectile = projectile;
        this.position = position;
    }

    @Override
    public @NotNull Entity getEntity() {
        return projectile;
    }

    public @NotNull Pos getCollisionPosition() {
        return position;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}