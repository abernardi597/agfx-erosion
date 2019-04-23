package agfx.fluid;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public class SPH {
    private static final double R = 8.3144598f;

    public static class Particle {
        public final Fluid fluid;
        public final Vector3d pos;
        public final Vector3d vel;
        public final double mass;

        public boolean fixed;

        protected final List<Particle> neighbors;
        public final Vector3d force;
        public final Vector3d acc;
        public final Vector3d normal;
        public double density;
        public double pressure;

        public Particle(Fluid f, double m) {
            fluid = f;
            pos = new Vector3d();
            vel = new Vector3d();
            mass = m;

            neighbors = new ArrayList<>();
            force = new Vector3d();
            acc = new Vector3d();
            normal = new Vector3d();
        }
    }

    public final ArrayList<Particle> particles;
    public final Vector3d gravity;

    private final double kernelRadius;
    private final int gridWidth;
    private final int gridHeight;
    private final int gridDepth;

    public SPH(int capacity, double h, Vector3dc dim) {
        particles = new ArrayList<>(capacity);
        gravity = new Vector3d();
        kernelRadius = h;
        gridWidth = (int) Math.ceil(dim.x() / h);
        gridHeight = (int) Math.ceil(dim.y() / h);
        gridDepth = (int) Math.ceil(dim.z() / h);
    }

    private int indexOf(Particle p) {
        int ix = (int) Math.floor(p.pos.x / kernelRadius);
        int iy = (int) Math.floor(p.pos.y / kernelRadius);
        int iz = (int) Math.floor(p.pos.z / kernelRadius);
        return ix + iy * gridWidth + iz * gridWidth * gridHeight;
    }

    public void update(double dt) {
        // Using Velocity Verlet integeration
        // 1. Update particle positions
        // 2. Compute forces
        // 3. Compute new acceleration

        if (particles.removeIf(p -> !p.pos.isFinite()))
            System.err.println("SPH: Instability detected");
        particles.parallelStream().forEach(p -> {
            p.pos.add(p.acc.mul(dt / 2, new Vector3d()).add(p.vel).mul(dt));

            if (p.pos.x < 0) {
                p.pos.x = 0;
                p.vel.x *= -0.99999;
                p.acc.x = Math.max(0, p.acc.x);
            }
            if (p.pos.x > gridWidth * kernelRadius) {
                p.pos.x = gridWidth * kernelRadius;
                p.vel.x *= -0.99999;
                p.acc.x = Math.min(0, p.acc.x);
            }

            if (p.pos.y < 0) {
                p.pos.y = 0;
                p.vel.y *= -0.99999;
                p.acc.y = Math.max(0, p.acc.y);
            }
            if (p.pos.y > gridHeight * kernelRadius) {
                p.pos.y = gridHeight * kernelRadius;
                p.vel.y *= -0.99999;
                p.acc.y = Math.min(0, p.acc.y);
            }

            if (p.pos.z < 0) {
                p.pos.z = 0;
                p.vel.z *= -0.99999;
                p.acc.z = Math.max(0, p.acc.z);
            }
            if (p.pos.z > gridDepth * kernelRadius) {
                p.pos.z = gridDepth * kernelRadius;
                p.vel.z *= -0.99999;
                p.acc.z = Math.min(0, p.acc.z);
            }
        });

        // Recompute particle neighborhoods
        particles.sort(Comparator.comparingInt(this::indexOf));

        int[] cells = new int[gridWidth * gridHeight * gridDepth];
        for (int i = 1; i < cells.length; ++i) {
            int index = cells[i - 1];
            while (index < particles.size() && indexOf(particles.get(index)) < i)
                ++index;
            cells[i] = index;
        }

        particles.parallelStream().forEach(p -> {
            p.neighbors.clear();
            IntStream.of(indexOf(p))
                    // Compute Cartesian product of neighboring cells
                    .flatMap(c -> IntStream.of(c, c + 1, c - 1))
                    .flatMap(c -> IntStream.of(c, c + gridWidth, c - gridWidth))
                    .flatMap(c -> IntStream.of(c, c + gridWidth * gridHeight, c - gridWidth * gridHeight))
                    .filter(c -> c >= 0 && c < cells.length)
                    // Map each cell to the particles in it
                    .mapToObj(c -> particles.subList(cells[c], c+1 < cells.length? cells[c+1] : particles.size()).stream())
                    .flatMap(Function.identity())
                    // Only include other particles within kernel radius
                    .filter(q -> p != q)
                    .filter(q -> q.pos.distanceSquared(p.pos) < kernelRadius*kernelRadius)
                    .forEach(p.neighbors::add);
        });

        // Compute the densities and resultant pressures at each particle
        particles.parallelStream().forEach(p -> {
            Vector3d scratch = new Vector3d();
            p.density = 0;
            for (Particle q : p.neighbors)
                p.density += q.mass * Kernel.POLY_6.kernel(p.pos.sub(q.pos, scratch), kernelRadius);
            p.pressure = p.fluid.restPressure + 293.15 * (p.density - p.fluid.restDensity) / p.fluid.molarMass;
        });

        // Compute the forces acting on each particle
        particles.parallelStream().forEach(p -> {
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
                tension += (p.fluid.cohesion + q.fluid.cohesion) / 2 * -Kernel.POLY_6.laplacian(p.pos.sub(q.pos, scratch), kernelRadius);
                p.normal.add(Kernel.POLY_6.gradient(scratch, kernelRadius, scratch).mul(q.mass / q.density));
            }
            double n = p.normal.length();
            if (n < Math.pow(kernelRadius, -1/3.0))
                p.normal.set(0);
            else p.force.add(p.normal.mul(tension / n, scratch));
        });

        // Compute new accelerations and velocities
        particles.parallelStream().forEach(p -> {
            Vector3d scratch = new Vector3d();
            // Store old acceleration
            scratch.set(p.acc);
            // Compute new acceleration
            p.force.div(p.density, p.acc);
            // Compute average acceleration over interval
            scratch.add(p.acc).mul(dt / 2);
            // Update velocity using averaged accelerations
            p.vel.add(scratch);
        });
    }
}
