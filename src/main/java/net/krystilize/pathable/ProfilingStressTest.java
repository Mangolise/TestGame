package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

class ProfilingStressTest {
    public static void main(String[] args) {
        // Z is ignored
        Path a = Path.line(new Vec(-1, 0), new Vec(0, 1));
        Path b = Path.line(new Vec(0, 1), new Vec(1, 0));
        Path c = Path.line(new Vec(-1, 0), new Vec(0, -1));
        Path d = Path.line(new Vec(0, -1), new Vec(1, 0));
        Path curveA = Path.interpolation(a, b);
        Path curveB = Path.interpolation(c, d);
        Path join = Path.join(curveA, curveB);
        Path path = Path.shapes().triangle(new Vec(-1, 0), new Vec(0, 1), new Vec(1, 0));
        Path circle = Path.shapes().circle(new Vec(0, 0), 1.1);

        Path curve = Path.curve(
                new Vec(-1, -1),
                new Vec(-1, 1),
                new Vec(1, 1),
                new Vec(1, -1)
        );

        int hash = 0;
        for (int i = 0; i < 1000000; i++) {
            hash += stress(a);
            hash += stress(b);
            hash += stress(c);
            hash += stress(d);

            hash += stress(curveA);
            hash += stress(curveB);

            hash += stress(join);
            hash += stress(path);
            hash += stress(circle);
            hash += stress(curve);
        }

        System.out.println(hash);
    }

    public static int stress(Path path) {
        int i = 0;
        for (Path.Context context : path.equalIterate(0.01)) {
            i += context.index();
        }
        return i;
    }
}
