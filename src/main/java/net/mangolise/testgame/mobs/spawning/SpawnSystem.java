package net.mangolise.testgame.mobs.spawning;

import net.kyori.adventure.text.Component;
import net.minestom.server.adventure.audience.Audiences;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SpawnSystem {
    
    private static long msBetweenSpawns = 50; // cooldown between spawns in milliseconds
    
    private static final Set<SpawnInfo> spawns = Set.of(
            ///execute in minecraft:test-game-dimension run tp @s 15.25 -9.60 -8.11 804.15 32.55
            ///execute in minecraft:test-game-dimension run tp @s 15.25 -1.71 -8.11 754.50 19.65
            new SpawnInfo(new Vec(15, -9, -8), new Vec(15, -1, -8), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s 20.95 -9.24 -28.46 1079.25 18.75
            ///execute in minecraft:test-game-dimension run tp @s 20.95 -1.74 -28.46 1083.00 29.10
            new SpawnInfo(new Vec(20, -9, -28), new Vec(20, -1, -28), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s 46.31 -9.25 -6.29 1252.50 3.60
            ///execute in minecraft:test-game-dimension run tp @s 46.31 -1.76 -6.29 1262.10 5.70
            new SpawnInfo(new Vec(47, 4, 5), new Vec(46, -1, -6), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s 45.21 2.38 22.92 1260.60 22.35
            ///execute in minecraft:test-game-dimension run tp @s 45.54 2.38 20.67 1267.80 18.15
            new SpawnInfo(new Vec(45, 2, 22), new Vec(45, 2, 20), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s 22.96 -11.85 26.70 1180.35 16.80
            ///execute in minecraft:test-game-dimension run tp @s 22.96 -2.49 26.70 1185.75 23.85
            new SpawnInfo(new Vec(22, -11, 26), new Vec(22, -2, 26), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s 11.61 0.26 36.32 1351.80 20.25
            ///execute in minecraft:test-game-dimension run tp @s 14.03 0.26 36.41 1352.25 21.90
            new SpawnInfo(new Vec(11, 0, 36), new Vec(14, 0, 36), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s -3.55 7.35 42.43 1262.25 52.95
            ///execute in minecraft:test-game-dimension run tp @s -3.55 16.12 42.43 1252.20 84.90
            new SpawnInfo(new Vec(-9, 3, 42), new Vec(-9, 3, 42), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s -27.02 -0.07 16.02 1351.35 14.40
            ///execute in minecraft:test-game-dimension run tp @s -19.45 -0.07 16.19 1351.65 16.35
            new SpawnInfo(new Vec(-27, 0, 16), new Vec(-19, 0, 16), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s -5.55 3.65 28.83 1127.85 39.30
            ///execute in minecraft:test-game-dimension run tp @s -5.36 11.73 28.31 1185.45 77.70
            new SpawnInfo(new Vec(-5, 3, 28), new Vec(-5, 11, 28), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s -27.40 0.28 -9.39 1192.80 52.05
            ///execute in minecraft:test-game-dimension run tp @s -28.46 0.28 -10.45 1221.75 43.20
            new SpawnInfo(new Vec(-27, 0, -9), new Vec(-28, 0, -10), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s -15.95 -5.10 3.98 1292.25 12.60
            ///execute in minecraft:test-game-dimension run tp @s -15.95 -1.04 3.98 1302.00 49.05
            new SpawnInfo(new Vec(-15, -5, 3), new Vec(-15, -1, 3), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s -5.65 -1.38 -33.55 1413.15 18.90
            ///execute in minecraft:test-game-dimension run tp @s -5.65 2 -33.55 1423.50 27.60
            new SpawnInfo(new Vec(-5, -1, -33), new Vec(-5, 2, -33), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s 7.53 5.36 21.64 900.15 89.85
            ///execute in minecraft:test-game-dimension run tp @s 7.53 11.43 21.64 899.70 89.55
            new SpawnInfo(new Vec(7, 5, 21), new Vec(7, 11, 21), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s 7.33 6.02 -4.61 627.90 41.25
            ///execute in minecraft:test-game-dimension run tp @s 8.28 6.02 -4.69 633.60 89.55
            new SpawnInfo(new Vec(7, 6, -4), new Vec(8, 6, -4), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s 20.29 4.67 7.51 623.39 90.00
            ///execute in minecraft:test-game-dimension run tp @s 20.29 7.37 7.51 450.44 89.70
            new SpawnInfo(new Vec(20, 4, 7), new Vec(20, 7, 7), 0.5),
            
            ///execute in minecraft:test-game-dimension run tp @s -5.69 7.70 8.29 358.35 50.10
            ///execute in minecraft:test-game-dimension run tp @s -5.59 7.70 9.31 364.35 56.55
            new SpawnInfo(new Vec(-5, 7, 8), new Vec(-5, 7, 9), 0.5)
    );
    
    private static final Tag<Long> NEXT_SPAWN_TIME_MS = Tag.Long("testgame.next_spawn_time").defaultValue(0L);

    /**
     * Finds a spawn point, and spawns the entity within the given instance.
     *
     * @param instance the instance where the entity should be spawned
     * @param entity   the entity to spawn
     * @return a CompletableFuture that completes when the entity is spawned
     */
    public static CompletableFuture<Void> spawn(Instance instance, Entity entity) {
        long nextSpawnTimeMs = instance.getTag(NEXT_SPAWN_TIME_MS);
        
        long currentTimeMs = System.currentTimeMillis();
        
        if (currentTimeMs < nextSpawnTimeMs) {
            // if the next spawn time is in the future, schedule the spawn for later
            long delay = nextSpawnTimeMs - currentTimeMs;
            CompletableFuture<Void> complete = new CompletableFuture<>();
            instance.scheduler().scheduleTask(() -> {
                doSpawn(instance, entity).thenAccept(complete::complete);
            }, TaskSchedule.millis(delay), TaskSchedule.stop());
            
            // update the next spawn time
            instance.setTag(NEXT_SPAWN_TIME_MS, currentTimeMs + delay + msBetweenSpawns);
            return complete;
        }
        
        // otherwise, spawn the entity
        // but update the next spawn time to be 50ms in the future first
        instance.setTag(NEXT_SPAWN_TIME_MS, currentTimeMs + msBetweenSpawns);
        return doSpawn(instance, entity);
    }
    
    private static CompletableFuture<Void> doSpawn(Instance instance, Entity entity) {
        // choose the spawn point furthest away from a player
        var players = instance.getPlayers();

        SpawnInfo bestSpawn = null;
        double bestDistance = Double.NEGATIVE_INFINITY;

        for (SpawnInfo spawn : spawns) {
            Point spawnPoint = spawn.getSpawnPoint();
            double distance = players.stream()
                    .mapToDouble(player -> player.getPosition().distance(spawnPoint))
                    .min()
                    .orElse(Double.POSITIVE_INFINITY);

            if ((distance * Math.random()) > bestDistance) {
                bestDistance = distance;
                bestSpawn = spawn;
            }
        }

        if (bestSpawn == null) {
            throw new IllegalStateException("No spawn point found for entity " + entity.getUuid());
        }

        // teleport the entity to the spawn point
        // TODO: handle the secondsToSpawn logic - make them come OUT OF THE GROUND!
        Point spawnPoint = bestSpawn.getSpawnPoint();
        
        
        // add tiny bit of randomness to the spawn point
        spawnPoint = spawnPoint.add(Math.random() * 0.1, Math.random() * 0.1, Math.random() * 0.1);
        return entity.setInstance(instance, spawnPoint);
    }

    public record SpawnInfo(Point start, @Nullable Point end, @Nullable Double secondsToSpawn) {
        public SpawnInfo {
            if (end != null && secondsToSpawn == null) {
                throw new IllegalArgumentException("If end is set, secondsToSpawn must also be set.");
            }
        }

        public SpawnInfo(Point start) {
            this(start, null, null);
        }

        public Point getSpawnPoint() {
            return end != null ? end : start;
        }
    }
}
