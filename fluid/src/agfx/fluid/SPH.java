package agfx.fluid;

import agfx.gl.Utils;
import agfx.gl.VertexPoolArray;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_FLOAT;

public class SPH {
    private static final double R = 8.3144598f;

    public class Particle {
        public final Fluid fluid;
        public final Vector3d pos;
        public final Vector3d vel;
        public final double mass;

        protected int index;

        public boolean fixed;

        protected final List<Particle> neighbors;
        public final Vector3d force;
        public final Vector3d acc;
        public final Vector3d normal;
        public double density;
        public double pressure;

        protected Particle(Fluid f, double m, int i) {
            fluid = f;
            pos = new Vector3d();
            vel = new Vector3d();
            mass = m;

            index = i;

            neighbors = new ArrayList<>();
            force = new Vector3d();
            acc = new Vector3d();
            normal = new Vector3d();
        }

        public void pack() {
            Vector3f v = new Vector3f();
            pool.put(index, 0, v.set(pos)::get);
            pool.put(index, 12, v.set(normal)::get);
        }

        protected int cell() {
            int ix = (int) Math.floor((pos.x - origin.x()) / kernelRadius);
            int iy = (int) Math.floor((pos.y - origin.y()) / kernelRadius);
            int iz = (int) Math.floor((pos.z - origin.z()) / kernelRadius);
            return ix + iy * grid.x() + iz * grid.x() * grid.y();
        }
    }

    public final VertexPoolArray pool;
    public final Particle[] particles;
    public final Vector3d gravity;

    private final double kernelRadius;
    private final Vector3dc origin;
    private final Vector3ic grid;

    public SPH(int capacity, double h, Vector3dc min, Vector3dc max) {
        pool = new VertexPoolArray(capacity, 2 * 3 * Utils.sizeof(GL_FLOAT));
        particles = new Particle[capacity];
        gravity = new Vector3d();
        kernelRadius = h;
        origin = new Vector3d(min);
        grid = new Vector3i().set(new Vector3d(max).sub(min).div(h).ceil());
        System.out.println(h);
        System.out.println(min);
        System.out.println(max);
        System.out.println(grid);
    }

    public Particle addParticle(Fluid f, double m) {
        int i = pool.request();
        return particles[i] = new Particle(f, m, i);
    }

    public Stream<Particle> particles() {
        // Assumes that the array is compact, which it should be after sorting for neighbors
        return Arrays.stream(particles, 0, pool.used());
    }

    public void update(double dt) {
        // Using Velocity Verlet integeration
        // 1. Update particle positions
        // 2. Compute forces
        // 3. Compute new acceleration

        particles().parallel().forEach(p -> {
            p.pos.add(p.acc.mul(dt / 2, new Vector3d()).add(p.vel).mul(dt));
            if (p.pos.x < origin.x()) {
                p.pos.x = origin.x();
                p.vel.x *= -0.99999;
                p.acc.x = Math.max(0, p.acc.x);
            }
            if (p.pos.x > origin.x() + grid.x() * kernelRadius) {
                p.pos.x = origin.x() + grid.x() * kernelRadius;
                p.vel.x *= -0.99999;
                p.acc.x = Math.min(0, p.acc.x);
            }

            if (p.pos.y < origin.y()) {
                p.pos.y = origin.y();
                p.vel.y *= -0.99999;
                p.acc.y = Math.max(0, p.acc.y);
            }
            if (p.pos.y > origin.y() + grid.y() * kernelRadius) {
                p.pos.y = origin.y() + grid.y() * kernelRadius;
                p.vel.y *= -0.99999;
                p.acc.y = Math.min(0, p.acc.y);
            }

            if (p.pos.z < origin.z()) {
                p.pos.z = origin.z();
                p.vel.z *= -0.99999;
                p.acc.z = Math.max(0, p.acc.z);
            }
            if (p.pos.z > origin.z() + grid.z() * kernelRadius) {
                p.pos.z = origin.z() + grid.z() * kernelRadius;
                p.vel.z *= -0.99999;
                p.acc.z = Math.min(0, p.acc.z);
            }
        });

        int len = pool.used();
        for (int i = 0; i < particles.length; ++i) {
            if (particles[i] != null && !particles[i].pos.isFinite()) {
                pool.reclaim(particles[i].index);
                particles[i] = null;
            }
        }
        if (pool.used() < len)
            System.err.println("Instability detected");

        // Recompute particle neighborhoods
        Arrays.parallelSort(particles, Comparator.nullsLast(Comparator.comparingInt(Particle::cell)));

        int[] cells = new int[grid.x() * grid.y() * grid.z()];
        for (int i = 1; i < cells.length; ++i) {
            int index = cells[i - 1];
            while (index < particles.length && particles[index] != null && particles[index].cell() < i)
                ++index;
            cells[i] = index;
        }

        particles().parallel().forEach(p -> {
            p.neighbors.clear();
            IntStream.of(p.cell())
                    // Compute Cartesian product of neighboring cells
                    .flatMap(c -> IntStream.of(c, c + 1, c - 1))
                    .flatMap(c -> IntStream.of(c, c + grid.x(), c - grid.x()))
                    .flatMap(c -> IntStream.of(c, c + grid.x() * grid.y(), c - grid.x() * grid.y()))
                    .filter(c -> c >= 0 && c < cells.length)
                    // Map each cell to the particles in it
                    .mapToObj(c -> Arrays.stream(particles, cells[c], c+1 < cells.length? cells[c+1] : pool.used()))
                    .flatMap(Function.identity())
                    // Only include other particles within kernel radius
                    .filter(q -> p != q)
                    .filter(q -> q.pos.distanceSquared(p.pos) < kernelRadius*kernelRadius)
                    .forEach(p.neighbors::add);
        });

        // Compute the densities and resultant pressures at each particle
        particles().parallel().forEach(p -> {
            Vector3d scratch = new Vector3d();
            p.density = 0;
            for (Particle q : p.neighbors)
                p.density += q.mass * Kernel.POLY_6.kernel(p.pos.sub(q.pos, scratch), kernelRadius);
            p.pressure = 293.15 * (p.density - p.fluid.restDensity) / p.fluid.molarMass - p.fluid.restPressure;
        });

        // Compute the forces acting on each particle
        particles().parallel().forEach(p -> {
            Vector3d scratch = new Vector3d();
            gravity.mul(p.density, p.force);
            p.normal.set(0);
            double tension = 0;
            for (Particle q : p.neighbors) {
                // Pressure
                p.force.sub(Kernel.SPIKY.gradient(p.pos.sub(q.pos, scratch), kernelRadius, scratch).mul(q.mass * (p.pressure / (p.density * p.density) + q.pressure / (q.density * q.density))));
                // p.force.sub(kernelPressureGradient(p.pos.sub(q.pos, scratch), kernelRadius, scratch).mul(q.mass * (p.pressure + q.pressure) / (2 * q.density)));

                // Viscosity
                double k = Kernel.VISCOSITY.laplacian(p.pos.sub(q.pos, scratch), kernelRadius);
                p.force.add(q.vel.sub(p.vel, scratch).mul((q.mass / q.density) * (p.fluid.viscosity + q.fluid.viscosity) / 2 * k));

                // Surface normal
                tension -= (p.fluid.cohesion + q.fluid.cohesion) / 2 * Kernel.POLY_6.laplacian(p.pos.sub(q.pos, scratch), kernelRadius);
                p.normal.add(Kernel.POLY_6.gradient(scratch, kernelRadius, scratch).mul(q.mass / q.density));
            }
            double n = p.normal.length();
            if (n < Utils.EPSILON)
                p.normal.set(0);
            else p.force.add(p.normal.mul(tension / n, scratch));
        });

        // Compute new accelerations and velocities
        particles().parallel().forEach(p -> {
            Vector3d scratch = new Vector3d();
            // Store old acceleration
            scratch.set(p.acc);
            // Compute new acceleration
            p.force.div(p.density, p.acc);
            // Compute average acceleration over interval
            scratch.add(p.acc).mul(dt / 2);
            // Update velocity using averaged accelerations
            p.vel.add(scratch);
            p.pack();
        });
    }

    public void close() {
        pool.close();
    }
}
