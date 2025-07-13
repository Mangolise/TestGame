package net.mangolise.testgame.util;

import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Throttler {
    
    private static final Tag<Map<String, Throttler>> THROTTLERS_TAG = Tag.<Map<String, Throttler>>Transient("instance.throttlers")
            .defaultValue(ConcurrentHashMap::new);
    
    private static Throttler getThrottler(Instance instance, String id, double msPerTick) {
        Map<String, Throttler> throttlers = instance.getTag(THROTTLERS_TAG);
        if (throttlers.containsKey(id)) {
            return throttlers.get(id);
        }

        var throttler = new Throttler(msPerTick);
        instance.eventNode().addListener(InstanceTickEvent.class, event -> {
            throttler.runningTimeMs = 0;
        });
        throttlers.put(id, throttler);
        instance.setTag(THROTTLERS_TAG, throttlers);
        return throttler;
    }

    private final double msPerTick;
    private double runningTimeMs;

    public Throttler(double msPerTick) {
        this.msPerTick = msPerTick;
        this.runningTimeMs = 0;
    }
    
    public static boolean shouldThrottle(Instance instance, String id, double msPerTick) {
        Throttler throttler = getThrottler(instance, id, msPerTick);
        return throttler.runningTimeMs >= msPerTick;
    }
    
    public static void useTime(Instance instance, String id, double msPerTick, Runnable task) {
        Throttler throttler = getThrottler(instance, id, msPerTick);
        
        double currentTime = System.nanoTime() / 1_000_000.0; // Convert to milliseconds
        task.run();
        double elapsedTime = System.nanoTime() / 1_000_000.0 - currentTime;
        
        throttler.runningTimeMs += elapsedTime;
    }
}
