package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

record LineImpl(Vec start, Vec end, double length, Path derivative) implements Path, PathImpl {

    public LineImpl(Vec start, Vec end) {
        this(start, end, calculateLength(start, end), createDerivative(start, end));
    }

    @Override
    public Vec sample(double delta) {
//        double x = start.x() + (end.x() - start.x()) * delta;
//        double y = start.y() + (end.y() - start.y()) * delta;
//        double z = start.z() + (end.z() - start.z()) * delta;
        double inverseDelta = 1 - delta;
        double x = start.x() * inverseDelta + end.x() * delta;
        double y = start.y() * inverseDelta + end.y() * delta;
        double z = start.z() * inverseDelta + end.z() * delta;
        return new Vec(x, y, z);
    }

    @Override
    public double distance(double deltaStart, double deltaEnd) {
        return length() * (deltaEnd - deltaStart);
    }

    @Override
    public Path rotate(Vec vec, Vec angles) {
        return Path.line(RotateImpl.rotation(start, vec, angles), RotateImpl.rotation(end, vec, angles));
    }

    @Override
    public double delta(double deltaStart, double distance) {
        return distance / length();
    }

    public double length() {
        return Math.sqrt(Math.pow(end.x() - start.x(), 2) + Math.pow(end.y() - start.y(), 2) + Math.pow(end.z() - start.z(), 2));
    }

    private static double calculateLength(Vec start, Vec end) {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double dz = end.z() - start.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Path createDerivative(Vec start, Vec end) {
        return Path.constant(end.sub(start));
    }

    @Override
    public String toString() {
        return "Line(" + start() + ", " + end() + ")";
    }
}
