package agfx;

import agfx.gl.Utils;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Cloud {

    public static Cloud load(InputStream in) throws NoSuchElementException, IllegalStateException {
        Scanner scan = new Scanner(in);
        Set<Point> points = new LinkedHashSet<>();
        while (scan.hasNext()) {
            double x = scan.nextDouble();
            double y = scan.nextDouble();
            double z = scan.nextDouble();
            Vector3d vertex = new Vector3d(x, y, z);

            x = scan.nextDouble();
            y = scan.nextDouble();
            z = scan.nextDouble();
            Vector3d normal = new Vector3d(x, y, z);

            points.add(new Point(vertex, normal));
        }
        return new Cloud(points);
    }

    public static Cloud sphere(double r, Vector3dc center, int count, Random rng) {
        Set<Point> points = new LinkedHashSet<>();
        while (points.size() < count) {
            Vector3d vertex = new Vector3d(2*r*rng.nextDouble() - r,2*r*rng.nextDouble() - r, 2*r*rng.nextDouble() - r).add(center);
            if (vertex.distanceSquared(center) <= r*r)
                points.add(new Point(vertex, vertex.sub(center, new Vector3d()).normalize()));
        }
        return new Cloud(points);
    }

    public static class Point {

        public final Vector3dc vertex;
        public final Vector3dc normal;

        private Point(Vector3dc v, Vector3dc n) {
            vertex = v;
            normal = n;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vertex, normal);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point) {
                Point p = ((Point) obj);
                return vertex.equals(p.vertex, Utils.EPSILON) && normal.equals(p.vertex, Utils.EPSILON);
            } else return false;
        }
    }

    public final Set<Point> points;
    public final Vector3dc min;
    public final Vector3dc max;

    public Cloud(Collection<Point> data) {
        points = Collections.unmodifiableSet(new LinkedHashSet<>(data));

        Vector3d min = new Vector3d();
        Vector3d max = new Vector3d();
        points.forEach(p -> {
            min.min(p.vertex);
            max.max(p.vertex);
        });
        this.min = min;
        this.max = max;
    }

    public Cloud(Point... data) {
        this(Arrays.asList(data));
    }

}
