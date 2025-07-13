package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

import java.util.ArrayList;
import java.util.List;

public interface Shapes {
    Path line(Vec a, Vec b);
    Path triangle(Vec a, Vec b, Vec c);
    Path polygonConcave(List<Vec> vecs, List<VecTuple> concaves);
    Path polygonConvex(List<Vec> vecs);
    Path polygonFlat(List<Vec> vecs);
    Path circle(Vec center, double radius);
    Path ellipse(Vec center, Vec radius);
    Path rectangle(Vec a, Vec b);
    Path rhombus(Vec a, Vec b, Vec c, Vec d);
    Path trapezoid(Vec a, Vec b, Vec c, Vec d);
    Path sphere(Vec center, double radius);
    Path cone(Vec center, double radius, double height);
    Path cylinder(Vec center, double radius, double height);
    Path torus(Vec center, double innerRadius, double outerRadius);
    Path helix(Vec center, double radius, double height, int revolutions);
    Path cuboid(Vec a, Vec b);
    Path prism(Vec center, double radius, double height);
}
