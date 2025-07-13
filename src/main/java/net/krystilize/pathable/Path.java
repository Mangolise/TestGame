package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


/**
 * A path is a function that takes a delta (0 to 1) and returns a vec in 3d space.
 * NOTE: Path implementations absolute MUST check if their derivatives can be a line, and if so, use Path#line
 */
public sealed interface Path permits ConstImpl, InterpolationImpl, LineImpl, MultiJoinImpl, PathImpl, RotateImpl {

    /**
     * Samples the path at an arbitrary delta (0 to 1) along the path.
     * @param delta the delta (0 to 1) along the path
     * @return the position of the path at the given delta
     */
    Vec sample(double delta);

    /**
     * Calculates the derivative of the path
     * @return the derivative of the path
     */
    Path derivative();

    /**
     * Calculates the distance between two deltas along the path
     * @param deltaStart the starting delta (0 to 1) along the path
     * @param deltaEnd the ending delta (0 to 1) along the path
     * @return the distance between the two deltas along the path
     */
    double distance(double deltaStart, double deltaEnd);

    /**
     * Calculates the delta needed to travel a given distance along the path, from a given starting delta
     * @param deltaStart the starting delta (0 to 1) along the path
     * @param distance the distance to travel along the path
     * @return the delta needed to travel the given distance along the path NOT the absolute delta
     */
    double delta(double deltaStart, double distance);

    /**
     * Calculates the total length (in units) of the path
     * @return the total length (in units) of the path
     */
    double length();

    /**
     * Iterates over the path, calling the callback for each vec along the path.
     * @param distanceBetween the distance between each vec along the path
     * @return an iterable that can be used to iterate over the path
     */
    Iterable<Context> equalIterate(double distanceBetween);

    /**
     * Rotates this path around a vec.
     * @param vec the vec to rotate around
     * @param angles the angles to rotate by
     * @return a path that is this path rotated around the given vec by the given rotation
     */
    Path rotate(Vec vec, Vec angles);

    /**
     * Creates a linear path between two vecs
     * @param a the starting vec
     * @param b the ending vec
     * @return a linear path between the two vecs
     */
    static Path line(Vec a, Vec b) {
        if (a.equals(b)) {
            return Path.constant(a);
        }
        return new LineImpl(a, b);
    }

    /**
     * Creates a value path
     * @param value the value of the value path
     * @return a value path
     */
    static Path constant(Vec value) {
        if (value.equals(Vec.ZERO)) {
            return ConstImpl.ZERO;
        }
        return new ConstImpl(value);
    }

    /**
     * Joins two paths together
     * @param a the first path
     * @param b the second path
     * @return a path that is the first path followed by the second path
     */
    static Path join(Path a, Path b) {
        return join(new Path[] { a, b });
    }

    /**
     * Joins multiple paths together
     * @param paths the paths to join
     * @return a path that is the first path followed by the second path followed by the third path, etc.
     */
    static Path join(Path... paths) {
        return join(Stream.of(paths)
                .map(path -> Map.entry(path, 1.0))
                .toList());
    }

    /**
     * Joins multiple weighted paths together
     * @param paths the paths to join
     * @return a path that is the first path followed by the second path followed by the third path, etc.
     */
    static Path join(Map.Entry<Path, Double>... paths) {
        return join(List.of(paths));
    }

    /**
     * Joins multiple weighted paths together
     * @param paths the paths to join
     * @return a path that is the first path followed by the second path followed by the third path, etc.
     */
    static Path join(List<Map.Entry<Path, Double>> paths) {
        return PathImpl.join(paths);
    }

    /**
     * Linearly interpolates between two paths
     * @param a the starting path
     * @param b the ending path
     * @return a linear interpolation between the two paths
     */
    static Path interpolation(Path a, Path b) {
        return new InterpolationImpl(a, b);
    }

    /**
     * Linearly interpolates between multiple paths
     * @param paths the paths to interpolate between
     * @return a linear interpolation between the paths
     */
    static Path interpolation(Path... paths) {
        return interpolation(List.of(paths));
    }

    /**
     * Linearly interpolates between multiple paths
     * @param paths the paths to interpolate between
     * @return a linear interpolation between the paths
     */
    static Path interpolation(List<Path> paths) {
        return PathImpl.interpolation(paths);
    }

    /**
     * Creates a path that linearly interpolates between two vecs
     * @param a the starting vec
     * @param b the ending vec
     * @return a linear interpolation between the two vecs
     */
    static Path curve(Vec a, Vec b) {
        return line(a, b);
    }

    /**
     * Creates a path that linearly interpolates between multiple vecs
     * @param vecs the vecs to interpolate between
     * @return a linear interpolation between the vecs
     */
    static Path curve(Vec... vecs) {
        return curve(List.of(vecs));
    }

    /**
     * Creates a path that linearly interpolates between multiple vecs
     * @param vecs the vecs to interpolate between
     * @return a linear interpolation between the vecs
     */
    static Path curve(List<Vec> vecs) {
        return PathImpl.curve(vecs);
    }

    static Shapes shapes() {
        return ShapesImpl.INSTANCE;
    }

    interface Context {
        int index();

        double distanceFromStart();
        double delta();
        
        @Nullable Context previous();
        
        Vec pos();
    }
}