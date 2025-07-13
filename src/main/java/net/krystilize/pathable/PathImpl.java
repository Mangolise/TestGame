package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

sealed interface PathImpl extends Path permits ConstImpl, InterpolationImpl, LineImpl, MultiJoinImpl, RotateImpl {

    @Override
    default Iterable<Context> equalIterate(double distanceBetween) {
        return () -> new Iterator<>() {
            Path.Context previous = null;
            double liveDelta = 0;
            double distanceFromStart = 0;
            int index = 0;

            @Override
            public boolean hasNext() {
                return liveDelta < 1.0;
            }

            @Override
            public Context next() {
                Vec vec = sample(liveDelta);
                Context context = new ContextImpl(index, distanceFromStart, liveDelta, previous, vec);

                index++;
                double additionalDelta = delta(liveDelta, distanceBetween);
                liveDelta += additionalDelta;

                distanceFromStart += distanceBetween;
                previous = context;

                return context;
            }
        };
    }

    @Override
    default Path rotate(Vec vec, Vec angles) {
        return new RotateImpl(this, vec, angles);
    }

    static Path interpolation(List<Path> paths) {
        if (paths.size() < 2) {
            throw new IllegalArgumentException("Must have at least two paths to interpolate");
        }
        if (paths.size() == 2) {
            return Path.interpolation(paths.get(0), paths.get(1));
        }
        List<Path> subPaths = paths.subList(0, paths.size() - 1);
        for (int i = 0; i < paths.size() - 1; i++) {
            subPaths.set(i, Path.interpolation(paths.get(i), paths.get(i + 1)));
        }
        return interpolation(subPaths);
    }

    static Path join(List<Map.Entry<Path, Double>> paths) {
        if (paths.size() < 2) {
            throw new IllegalArgumentException("Must have at least two paths to join");
        }
        return new MultiJoinImpl(paths);
    }

    static Path curve(List<Vec> vecs) {
        if (vecs.size() < 2) {
            throw new IllegalArgumentException("Must have at least two vecs to curve");
        }
        if (vecs.size() == 2) {
            return Path.line(vecs.get(0), vecs.get(1));
        }
        List<Path> paths = new ArrayList<>();
        for (int i = 0; i < vecs.size() - 1; i++) {
            paths.add(Path.line(vecs.get(i), vecs.get(i + 1)));
        }
        return interpolation(paths);
    }

    record ContextImpl(int index, double distanceFromStart, double delta, Path.Context previous, Vec pos) implements Path.Context {
    }
}
