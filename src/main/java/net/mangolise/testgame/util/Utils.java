package net.mangolise.testgame.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class Utils {
    
    // TODO: Optimize this method if we have time
    public static @Nullable Entity closestEntity(Instance instance, Point pos, Predicate<Entity> filter) {
        List<Entity> filtered = new ArrayList<>();

        for (Entity entity : instance.getEntities()) {
            if (!entity.isRemoved() && filter.test(entity)) {
                filtered.add(entity);
            }
        }

        if (filtered.isEmpty()) {
            return null; // no entities found
        }

        filtered.sort(Comparator.comparingDouble(entity -> entity.getPosition().distanceSquared(pos)));
        return filtered.getFirst();
    }
}
