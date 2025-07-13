package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

record RotateImpl(Path path, Vec vec, Vec angles, Path derivative) implements Path, PathImpl {

    public RotateImpl(Path path, Vec vec, Vec angles) {
        this(path, vec, angles, createDerivative(path, vec, angles));
    }

    @Override
    public Vec sample(double delta) {
        return rotation(path.sample(delta), vec, angles);
    }

    @Override
    public double distance(double deltaStart, double deltaEnd) {
        return path.distance(deltaStart, deltaEnd);
    }

    @Override
    public double delta(double deltaStart, double distance) {
        return path.delta(deltaStart, distance);
    }

    @Override
    public double length() {
        return path.length();
    }

    private static Path createDerivative(Path path, Vec vec, Vec angles) {
        return path.derivative().rotate(vec, angles);
    }

    static Vec rotation(Vec sample, Vec vec, Vec angles) {
        // Translate the position and the vec to the origin
        double x = sample.x() - vec.x();
        double y = sample.y() - vec.y();
        double z = sample.z() - vec.z();

        // Rotate around the X axis
        double angleCosX = Math.cos(angles.x());
        double angleSinX = Math.sin(angles.x());
        y = y * angleCosX - z * angleSinX;
        z = y * angleSinX + z * angleCosX;

        // Rotate around the Y axis
        double angleCosY = Math.cos(angles.y());
        double angleSinY = Math.sin(angles.y());
        x = x * angleCosX + z * angleSinY;
        z = -x * angleSinY + z * angleCosY;

        // Rotate around the Z axis
        double angleCosZ = Math.cos(angles.z());
        double angleSinZ = Math.sin(angles.z());
        x = x * angleCosZ - y * angleSinZ;
        y = x * angleSinZ + y * angleCosZ;

        // Translate the rotated position back to the original position
        return new Vec(x + vec.x(), y + vec.y(), z + vec.z());
    }
}
