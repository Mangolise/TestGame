package net.mangolise.testgame.mobs.spawning;

import net.mangolise.testgame.mobs.AttackableMob;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.scoreboard.Sidebar;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompleteWaveEvent implements Event, InstanceEvent {
    private final Instance instance;
    private final List<AttackableMob> entities;
    private final int waveNumber;
    
    public CompleteWaveEvent(@NotNull Instance instance, List<AttackableMob> entities, int waveNumber) {
        this.instance = instance;
        this.entities = entities;
        this.waveNumber = waveNumber;
    }
    
    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }
    
    public List<AttackableMob> getEntities() {
        return entities;
    }
    
    public int getWaveNumber() {
        return waveNumber;
    }
}
