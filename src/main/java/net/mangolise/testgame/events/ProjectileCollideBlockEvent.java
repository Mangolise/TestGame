package net.mangolise.testgame.events;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class ProjectileCollideBlockEvent extends ProjectileCollideWithAnyEvent {
    private final @NotNull Block block;

    public ProjectileCollideBlockEvent(@NotNull Entity projectile, @NotNull Pos position, @NotNull Block block) {
        super(projectile, position);
        this.block = block;
    }

    public @NotNull Block getBlock() {
        return block;
    }
}
