package net.mangolise.testgame.util;

import net.minestom.server.event.Event;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

import java.lang.constant.Constable;
import java.util.*;

/**
 * Schdules tasks to be run asap, but throttles them to avoid overloading the server.
 */
public class ThrottledScheduler {
    
    private final double msPerTick;
    private final Queue<Runnable> taskQueue = new ArrayDeque<>();
    
    public static ThrottledScheduler from(Instance instance, String name, double msPerTick) {
        Tag<ThrottledScheduler> tag = Tag.<ThrottledScheduler>Transient("testgame.scheduler." + name).defaultValue(() -> null);
        ThrottledScheduler scheduler = instance.getTag(tag);
        
        if (scheduler != null) {
            return scheduler;
        }
        
        ThrottledScheduler newScheduler = new ThrottledScheduler(msPerTick);
        instance.eventNode().addListener(InstanceTickEvent.class, Event -> {
            newScheduler.tick();
        });
        
        instance.setTag(tag, newScheduler);
        return newScheduler;
    }

    /**
     * Schedules the given task to be run in the future, throttled by a certain amount of milliseconds per tick.
     * @param instance the instance to use for scheduling
     * @param name the name of the scheduler, used to identify it
     * @param msPerTick the amount of milliseconds to throttle the scheduler by per tick
     * @param task the task to be scheduled
     */
    public static void use(Instance instance, String name, double msPerTick, Runnable task) {
        ThrottledScheduler scheduler = from(instance, name, msPerTick);
        scheduler.schedule(task);
    }

    private ThrottledScheduler(double msPerTick) {
        this.msPerTick = msPerTick;
    }
    
    public void schedule(Runnable task) {
        synchronized (taskQueue) {
            taskQueue.add(task);
        }
    }
    
    public void tick() {
        double startTimeMs = System.nanoTime() / 1_000_000.0;
        synchronized (taskQueue) {
            while (System.nanoTime() / 1_000_000.0 - startTimeMs < msPerTick && !taskQueue.isEmpty()) {
                Runnable task = taskQueue.poll();
                task.run();
            }
        }
    }
}
