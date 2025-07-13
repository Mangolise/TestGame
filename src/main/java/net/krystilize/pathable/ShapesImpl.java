package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

import java.util.ArrayList;
import java.util.List;

enum ShapesImpl implements Shapes {
    INSTANCE;

    public static final double k = (4.0 * (Math.sqrt(2) - 1.0)) / 3.0;

    @Override
    public Path line(Vec a, Vec b) {
        return Path.line(a, b);
    }

    @Override
    public Path triangle(Vec a, Vec b, Vec c) {
        return polygonFlat(List.of(a, b, c));
    }

    @Override
    public Path polygonConcave(List<Vec> vecs, List<VecTuple> concaves) {
        return null; // TODO: Concave polygon
    }

    @Override
    public Path polygonConvex(List<Vec> vecs) {
        return null; // TODO: Convex polygon
    }

    @Override
    public Path polygonFlat(List<Vec> vecs) {
        List<Path> paths = new ArrayList<>(vecs.size());
        for (int i = 0; i < vecs.size() - 1; i++) {
            paths.add(line(vecs.get(i), vecs.get(i + 1)));
        }
        paths.add(line(vecs.getLast(), vecs.getFirst()));
        return Path.join(paths.toArray(Path[]::new));
    }

    @Override
    public Path circle(Vec center, double radius) {
        return ellipse(center, new Vec(radius, radius));
    }

    @Override
    public Path ellipse(Vec center, Vec radius) {
        double rx = radius.x();
        double rxk = rx * ShapesImpl.k;
        double rz = radius.z();
        double rzk = rz * ShapesImpl.k;

        Path topLeft = Path.curve(center.add(-rx, 0, 0), center.add(-rx, 0, rzk), center.add(-rxk, 0, rz), center.add(0, 0, rz));
        Path topRight = Path.curve(center.add(0, 0, rz), center.add(rxk, 0, rz), center.add(rx, 0, rzk), center.add(rx, 0, 0));
        Path bottomRight = Path.curve(center.add(rx, 0, 0), center.add(rx, 0, -rzk), center.add(rxk, 0, -rz), center.add(0, 0, -rz));
        Path bottomLeft = Path.curve(center.add(0, 0, -rz), center.add(-rxk, 0, -rz), center.add(-rx, 0, -rzk), center.add(-rx, 0, 0));
        return Path.join(topLeft, topRight, bottomRight, bottomLeft);
    }

    @Override
    public Path rectangle(Vec a, Vec b) {
        return polygonFlat(List.of(a, new Vec(a.x(), b.y(), 0), b, new Vec(b.x(), a.y(), 0)));
    }

    @Override
    public Path rhombus(Vec a, Vec b, Vec c, Vec d) {
        return polygonFlat(List.of(a, b, c, d));
    }

    @Override
    public Path trapezoid(Vec a, Vec b, Vec c, Vec d) {
        return polygonFlat(List.of(a, b, c, d));
    }

    @Override
    public Path sphere(Vec center, double radius) {
        return null; // TODO: Sphere
    }

    @Override
    public Path cone(Vec center, double radius, double height) {
        return null; // TODO: Cone
    }

    @Override
    public Path cylinder(Vec center, double radius, double height) {
        return null; // TODO: Cylinder
    }

    @Override
    public Path torus(Vec center, double innerRadius, double outerRadius) {
        return null; // TODO: Torus
    }

    @Override
    public Path helix(Vec center, double radius, double height, int revolutions) {
        List<Path> circles = new ArrayList<>(revolutions);
        for (double i = 0; i < revolutions; i++) {
            circles.add(Path.shapes().circle(center, radius));
        }

        Path circlesPath = Path.join(circles.toArray(Path[]::new));

        Path line = Path.shapes().line(center, center.add(0, 0, height));

        return Path.interpolation(circlesPath, line);
    }

    @Override
    public Path cuboid(Vec a, Vec b) {
        return null; // TODO: Cuboid
    }

    @Override
    public Path prism(Vec center, double radius, double height) {
        return null;
    }
}
