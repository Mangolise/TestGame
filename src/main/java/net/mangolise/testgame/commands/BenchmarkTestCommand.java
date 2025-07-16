package net.mangolise.testgame.commands;

import net.mangolise.gamesdk.features.commands.MangoliseCommand;
import net.mangolise.testgame.mobs.AttackableMob;
import net.mangolise.testgame.util.Utils;
import net.minestom.server.instance.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BenchmarkTestCommand extends MangoliseCommand {
    private static final int ITERATIONS = 10_000;
    private static final int TRIES = 10;

    public BenchmarkTestCommand() {
        super("benchmarktest");

        addPlayerSyntax(((player, context) -> {
            Instance instance = player.getInstance();

            List<Long> times = new ArrayList<>();
            for (int i = 0; i < TRIES; i++) {
                long startTime = System.currentTimeMillis();
                for (int j = 0; j < ITERATIONS; j++) {
                    Utils.closestEntity(instance, player.getPosition().add(ThreadLocalRandom.current().nextDouble(-40, 40), 0, ThreadLocalRandom.current().nextDouble(-40, 40)), entity -> (entity instanceof AttackableMob));
                }
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                times.add(duration);
            }

            // average time
            long total = times.stream().mapToLong(Long::longValue).sum();
            long average = total / TRIES;
            player.sendMessage("Average time: " + average + " ms over " + TRIES + " tries.");

            times = new ArrayList<>();
            for (int i = 0; i < TRIES; i++) {
                long startTime = System.currentTimeMillis();
                for (int j = 0; j < ITERATIONS; j++) {
                    Utils.fastClosestEntity(instance, player.getPosition().add(ThreadLocalRandom.current().nextDouble(-40, 40), 0, ThreadLocalRandom.current().nextDouble(-40, 40)), entity -> (entity instanceof AttackableMob));
                }
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                times.add(duration);
            }

            // average time
            total = times.stream().mapToLong(Long::longValue).sum();
            average = total / TRIES;
            player.sendMessage("Average time [2]: " + average + " ms over " + TRIES + " tries.");
        }));
    }

    @Override
    protected String getPermission() {
        return "game.command.benchmarktest";
    }
}
