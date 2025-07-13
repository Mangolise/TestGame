package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

record ConstImpl(Vec value) implements Path, PathImpl {

    public static final Path ZERO = new ConstImpl(Vec.ZERO);

    @Override
    public Vec sample(double delta) {
        return value;
    }

    @Override
    public double distance(double deltaStart, double deltaEnd) {
        return 0;
    }

    @Override
    public double delta(double deltaStart, double distance) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public Path rotate(Vec vec, Vec angles) {
        Vec rotated = RotateImpl.rotation(value, vec, angles);
        return Path.constant(rotated);
    }

    @Override
    public Path derivative() {
        return ZERO;
    }

    @Override
    public double length() {
        return 0;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
