package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

record InterpolationImpl(Path a, Path b, double length, Path derivative) implements Path, PathImpl {

    InterpolationImpl {
        if (a.equals(b)) {
            throw new IllegalArgumentException("a and b must not be equal");
        }
        if (a instanceof ConstImpl && b instanceof ConstImpl) {
            throw new IllegalArgumentException("a and b must not both be ConstImpl, This is a straight line, NOT an interpolation.");
        }
    }

    public InterpolationImpl(Path a, Path b) {
        this(a, b, createDerivative(a, b));
    }

    public InterpolationImpl(Path a, Path b, Path derivative) {
        this(a, b, calculateLength(a, b, derivative), derivative);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    public Vec sample(double delta) {
        double weightA = 1 - delta;
        double weightB = delta;

        Vec a = this.a().sample(delta);
        Vec b = this.b().sample(delta);

        double x = a.x() * weightA + b.x() * weightB;
        double y = a.y() * weightA + b.y() * weightB;
        double z = a.z() * weightA + b.z() * weightB;

        return new Vec(x, y, z);
    }

    @Override
    public double distance(double deltaStart, double deltaEnd) {
        double derivativeDist = derivative().distance(deltaStart, deltaEnd);
        double ratio = length() / derivative().length();
        return ratio * derivativeDist;
    }

    @Override
    public double delta(double deltaStart, double distance) {
        double derivativeDelta = derivative().delta(deltaStart, distance);
        double ratio = derivative().length() / length();
        return derivativeDelta * ratio;
    }

    private static Vec sample(Path first, Path last, double delta) {
        double weightA = 1 - delta;
        double weightB = delta;

        Vec a = first.sample(delta);
        Vec b = last.sample(delta);

        double x = a.x() * weightA + b.x() * weightB;
        double y = a.y() * weightA + b.y() * weightB;
        double z = a.z() * weightA + b.z() * weightB;

        return new Vec(x, y, z);
    }

    private static double calculateLength(Path a, Path b, Path derivative) {
        // Approximate the length using brute force sampling.
        int partitions = 128;
        
        double sum = 0.0;

        for (int i = 0; i < partitions; i++) {
            double t1 = (double) i / (double) partitions;
            double t2 = (i + 1.0) / (double) partitions;

            Vec sample1 = sample(a, b, t1);
            Vec sample2 = sample(a, b, t2);

            double dx = sample2.x() - sample1.x();
            double dy = sample2.y() - sample1.y();
            double dz = sample2.z() - sample1.z();

            sum += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return sum;
    }

    private static Path createDerivative(Path a, Path b) {
        Path da = a.derivative();
        Path db = b.derivative();
        // Check if the derivative could be converted to a line.
        if (da instanceof ConstImpl constDa && db instanceof ConstImpl constDb) {
            return Path.line(constDa.value(), constDb.value());
        }
        return new InterpolationImpl(da, db);
    }

    @Override
    public String toString() {
        return "Interpolate(" + a() + ", " + b() + ")";
    }
}
