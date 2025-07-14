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
