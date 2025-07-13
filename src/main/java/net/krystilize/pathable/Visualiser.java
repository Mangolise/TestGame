package net.krystilize.pathable;

import net.minestom.server.coordinate.Vec;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

class Visualiser {

    private static final int width = 128;
    private static final int pixelWidth = 4;
    private static final int actualWidth = width * pixelWidth;
    private static final int actualWidthd2 = actualWidth / 2;

    private static double zoom = 26.0;

    public static void main(String[] args) {
        // Z is ignored
//        Path a = Path.line(new Vec(-1, 0), new Vec(0, 1));
//        Path b = Path.line(new Vec(0, 1), new Vec(1, 0));
//        Path c = Path.line(new Vec(-1, 0), new Vec(0, -1));
//        Path d = Path.line(new Vec(0, -1), new Vec(1, 0));
//        Path curveA = Path.interpolation(a, b);
//        Path curveB = Path.interpolation(c, d);
//        Path join = Path.join(curveA, curveB);
//        Path path = Path.shapes().triangle(new Vec(-1, 0), new Vec(0, 1), new Vec(1, 0));
//        Path circle = Path.shapes().circle(new Vec(0, 0), 1.1);

//        Path path = Path.line(new Vec(0, 0), new Vec(1, 1));
//        Path path = Path.line(new Vec(1, 1), new Vec(2, 0));
//        Path path = Path.interpolation(Path.line(new Vec(-1, 0), new Vec(1, 1)), Path.line(new Vec(1, 1), new Vec(2, 0)));
        Path path = Path.curve(new Vec(0, 0), new Vec(1, 1), new Vec(2, 0), new Vec(0, -1));
//        Path path = Path.shapes().circle(new Vec(0, 0, 0), 1);

//        Vec radius = new Vec(1);
//        Vec center = new Vec(0, 0, 0);
//        double rx = radius.x();
//        double rxk = rx * ShapesImpl.k;
//        double rz = radius.z();
//        double rzk = rz * ShapesImpl.k;
//
//        Path path = Path.curve(center.add(-rx, 0, 0), center.add(-rx, 0, rzk), center.add(-rxk, 0, rz));
        
//        Path path = Path.shapes().ellipse(new Vec(0, 0), new Vec(1, 1));
//        path = path.rotate(new Vec(0.0), new Vec(0.0, 1.0, 0.0));

        Path finalPath = path;
        window(graphics -> {
            graphics.setColor(Color.RED);
            drawPath(graphics, finalPath);
            graphics.setColor(Color.GREEN);
            drawPath(graphics, finalPath.derivative());
            graphics.setColor(Color.BLUE);
            drawPath(graphics, finalPath.derivative().derivative());
            graphics.setColor(Color.DARK_GRAY);
            drawPath(graphics, finalPath.derivative().derivative().derivative());
        });
    }

    private static void drawTarget(Graphics graphics, Vec vec) {
        int x = (int) Math.round(vec.x() * zoom);
        int y = (int) Math.round(vec.z() * zoom);
        graphics.fillRect(actualWidthd2 + x * pixelWidth, actualWidthd2 - y * pixelWidth, pixelWidth, pixelWidth);
    }

    public static void drawPath(Graphics graphics, Path path) {
        drawPath(graphics, new Vec(0, 0), path);
    }

    public static void drawPath(Graphics graphics, Vec offset, Path path) {
        equalIterate(path, (1.0 / zoom) / Math.sqrt(2), vec -> {
            drawTarget(graphics, vec.add(offset));
        });
    }

    public static JFrame window(Consumer<Graphics> paint) {
        // Launch a JFrame that draws a square
        JFrame window = new JFrame() {
            @Override
            public void paint(Graphics graphics) {
                super.paint(graphics);
                paint.accept(graphics);
            }
        };

        window.pack();
        window.setSize(actualWidth, actualWidth);
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        window.setFocusable(true);
        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON3)
                    window.dispose();
            }
        });
        window.addMouseWheelListener(evt -> {
            zoom = Math.max(1.0, zoom + evt.getPreciseWheelRotation());
            window.repaint();
        });

        return window;
    }

    static void equalIterate(Path path, double distanceBetween, Consumer<Vec> callback) {
//        double liveDelta = 0;
//            Vec vec = path.sample(liveDelta);
//        while (liveDelta < 1.0) {
//            callback.accept(vec);
//            liveDelta += Math.max(path.delta(liveDelta, distanceBetween), 0.001);
//        }
//        callback.accept(path.sample(1.0));
        
        for (Path.Context context : path.equalIterate(distanceBetween)) {
            callback.accept(context.pos());
        }
        callback.accept(path.sample(1.0));
    }
}