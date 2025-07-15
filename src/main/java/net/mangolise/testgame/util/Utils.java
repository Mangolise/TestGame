package net.mangolise.testgame.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.EntityTracker;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Utils {

    private static Entity findClosestEntity(List<Entity> entities, Point pos) {
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : entities) {
            double distance = entity.getPosition().distanceSquared(pos);
            if (distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }

        return closest;
    }
    
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

        return findClosestEntity(filtered, pos);
    }

    /**
     * Optimised method to find the closest entity to a position.
     * THIS METHOD MAY NOT BE FULLY ACCURATE, but it is faster than the full search.
     * <p>
     * If accuracy is required, use {@link #closestEntity(Instance, Point, Predicate)} instead.
     * @param instance the instance to search in
     * @param pos the position to search from
     * @param filter a filter to apply to the entities
     * @return the closest entity that matches the filter, or null if no entity is found
     */
    public static @Nullable Entity fastClosestEntity(Instance instance, Point pos, Predicate<Entity> filter) {
        int dist = 1;
        int maxDist = 3;  // Tuned for max performance for this game
        while (true) {
            List<Entity> filtered = new ArrayList<>();

            instance.getEntityTracker().nearbyEntitiesByChunkRange(pos, dist, EntityTracker.Target.ENTITIES, entity -> {
                if (!entity.isRemoved() && filter.test(entity)) {
                    filtered.add(entity);
                }
            });

            if (filtered.isEmpty()) {
                dist++;

                if (dist > maxDist) {
                    return closestEntity(instance, pos, filter);  // no entities found within max distance
                }
                continue;
            }

            if (filtered.size() == 1) {
                return filtered.getFirst();  // only one entity found, return it
            }

            return findClosestEntity(filtered, pos);
        }
    }
    
    public static String getFullSimpleClassName(Class<?> clazz) {
        return clazz.getCanonicalName().substring(clazz.getPackageName().length() + 1);
    }
    public static <T> List<Class<T>> getAllRecordSubclasses(Class<? extends T> sealedInterface) {
        List<Class<T>> subclasses = new ArrayList<>();

        if (!sealedInterface.isInterface() || !sealedInterface.isSealed()) {
            throw new IllegalArgumentException("The provided class must be a sealed interface.");
        }

        for (Class<?> permittedSubclass : sealedInterface.getPermittedSubclasses()) {
            if (permittedSubclass.isInterface()) {
                if (permittedSubclass.isSealed()) {
                    subclasses.addAll(getAllRecordSubclasses((Class<T>) permittedSubclass));
                } else {
                    // is non-sealed interface, which means we can only ignore it.
                    continue;
                }
            }
            
            if (permittedSubclass.isRecord()) {
                subclasses.add((Class<T>) permittedSubclass);
            }
            
            // else, we ignore it as it is not a record
        }
        
        return subclasses;
    }
}
