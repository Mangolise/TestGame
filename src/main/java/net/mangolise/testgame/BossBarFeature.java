package net.mangolise.testgame;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.mangolise.gamesdk.Game;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.mobs.spawning.CompleteWaveEvent;
import net.mangolise.testgame.mobs.spawning.SpawnWaveEvent;
import net.mangolise.testgame.mobs.spawning.WaveSystem;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;

public class BossBarFeature implements Game.Feature<TestGame> {
    private static final Tag<Instance> LAST_INSTANCE_TAG = Tag.Transient("testgame.boss_bar_feature.last_instance");

    private static boolean hasRegisteredGlobal = false;
    private Instance instance;

    public BossBarFeature() {}

    @Override
    public void setup(Context<TestGame> context) {
        instance = context.game().instance();

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

        context.eventNode().addListener(PlayerSpawnEvent.class, e -> {
            if (e.getPlayer().getTag(LAST_INSTANCE_TAG) == instance) {
                e.getPlayer().hideBossBar(bossBar);
            } else {
                MinecraftServer.getSchedulerManager().scheduleEndOfTick(() -> {
                    if (e.getInstance() == instance && !WaveSystem.from(instance).isNextWaveWaiting()) {
                        e.getPlayer().showBossBar(bossBar);
                        e.getPlayer().sendMessage("Joined instance");
                    }
                });
            }
        });

        if (!hasRegisteredGlobal) {
            hasRegisteredGlobal = true;
            MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, e -> {
                MinecraftServer.getSchedulerManager().scheduleEndOfTick(() -> {
                    e.getPlayer().setTag(LAST_INSTANCE_TAG, e.getInstance());
                });
            });
        }
    }
}
