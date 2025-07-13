package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

import java.util.List;
import java.util.Map;

record MultiJoinImpl(List<Map.Entry<Path, Double>> paths, double length, Path derivative) implements Path, PathImpl {

    public MultiJoinImpl {
        paths = PathUtil.normaliseAndCopy(paths);
    }

    public MultiJoinImpl(List<Map.Entry<Path, Double>> paths) {
        this(paths, calculateLength(paths), createDerivative(paths));
    }

    @Override
    public Vec sample(double delta) {
        int i = 0;
        var pathEntry = paths.get(i);
        while (i < paths.size() && delta > (pathEntry = paths.get(i)).getValue()) {
            delta -= pathEntry.getValue();
            i++;
        }
        return pathEntry.getKey().sample(delta / pathEntry.getValue());
    }

    @Override
    public double distance(double deltaStart, double deltaEnd) {
        double total = 0.0;

        for (var entry : paths()) {
            Path path = entry.getKey();
            double weight = entry.getValue();

            // Section has not started
            if (deltaStart > weight) {
                deltaStart -= weight;
                deltaEnd -= weight;
                continue;
            }

            // Section has started/starts within this path
            double sectionStart = Math.max(deltaStart, 0.0) / weight;
            double sectionEnd = Math.min(deltaEnd, weight) / weight;

            total += path.distance(sectionStart, sectionEnd);

            deltaStart -= weight;
            deltaEnd -= (sectionEnd * weight);

            if (deltaEnd <= 0.0) {
                break;
            }
        }

        return total;
    }

    @Override
    public double delta(double deltaStart, double distance) {
        double total = 0.0;

        for (var entry : paths()) {
            Path path = entry.getKey();
            double weight = entry.getValue();

            // Section has not started
            if (deltaStart >= weight) {
                deltaStart -= weight;
                continue;
            }

            // Section has started/starts within this path
            double sectionStart = Math.max(deltaStart, 0.0) / weight;

            double sectionDelta = path.delta(sectionStart, distance);
            double distanceCovered = path.distance(sectionStart, sectionStart + sectionDelta);

            total += sectionDelta * weight;
            distance -= distanceCovered;

            if (distance <= 0.0) {
                break;
            }
        }

        return total;
    }

    private static double calculateLength(List<Map.Entry<Path, Double>> paths) {
        return paths.stream().mapToDouble(entry -> entry.getKey().length()).sum();
    }

    private static Path createDerivative(List<Map.Entry<Path, Double>> paths) {
        List<Map.Entry<Path, Double>> derivatives = paths.stream()
                .map(entry -> Map.entry(entry.getKey().derivative(), entry.getValue()))
                .toList();
        if (derivatives.stream().map(Map.Entry::getKey).allMatch(ConstImpl.ZERO::equals)) {
            return ConstImpl.ZERO;
        }
        return Path.join(derivatives);
    }

    @Override
    public String toString() {
        return "Join(" + paths() + ")";
    }
}
