package net.mangolise.testgame;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.mobs.spawning.CompleteWaveEvent;
import net.mangolise.testgame.mobs.spawning.SpawnWaveEvent;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

public class BossBarSystem {

    private final Instance instance;

    public BossBarSystem(Instance instance) {
        this.instance = instance;
    }

    private static final Tag<BossBarSystem> BOSS_BAR_SYSTEM = Tag.Transient("boss_bar_system");

    public static BossBarSystem from(Instance instance) {
        if (!instance.hasTag(BOSS_BAR_SYSTEM)) {
            BossBarSystem bossBarSystem = new BossBarSystem(instance);
            instance.setTag(BOSS_BAR_SYSTEM, bossBarSystem);
        }
        return instance.getTag(BOSS_BAR_SYSTEM);
    }

    public void start() {
        Component name = Component.text("Wave").color(TextColor.color(226, 0, 0));
        BossBar bossBar = BossBar.bossBar(name, 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);

        instance.eventNode().addListener(SpawnWaveEvent.class, event -> {
            int maxSize = event.getEntities().size();

            bossBar.progress(1.0f);
            bossBar.name(name.append(Component.text(" " + event.getWaveNumber())));
            instance.showBossBar(bossBar);

            for (AttackableMob entity : event.getEntities()) {
                entity.asEntity().eventNode().addListener(RemoveEntityFromInstanceEvent.class, d -> {
                    long currentSize = event.getEntities()
                            .stream()
                            .map(AttackableMob::asEntity)
                            .filter(Entity::isRemoved)
                            .count();

                    bossBar.progress(1.0f - (float) currentSize / maxSize);
                });
            }
        });

        instance.eventNode().addListener(CompleteWaveEvent.class, e -> {
            instance.hideBossBar(bossBar);
        });
    }
}
