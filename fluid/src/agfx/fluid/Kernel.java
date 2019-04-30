package agfx.fluid;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface Kernel {

    double kernel(Vector3dc r, double h);
    Vector3d gradient(Vector3dc r, double h, Vector3d result);
    double laplacian(Vector3dc r, double h);

    static double iPow(double b, int exp) {
        double res = 1;
        while (exp > 0) {
            // If lsb is 1, then multiply by b, otherwise multiply by one
            res *= (exp % 2) * b + (1 - (exp % 2));
            exp >>= 1;
            b *= b;
        }
        return res;
    }

    // Kernels from Particle-based Fluid Simulation for Interactive Applications by Matthias and Mueller, 2003
    Kernel POLY_6 = new Kernel() {
        @Override
        public double kernel(Vector3dc r, double h) {
            return 315 / (64 * Math.PI * iPow(h, 9)) * iPow(h*h - r.lengthSquared(), 3);
        }

        @Override
        public Vector3d gradient(Vector3dc r, double h, Vector3d result) {
            return r.mul(-945 / (32 * Math.PI * iPow(h, 9)) * iPow(h*h - r.lengthSquared(), 2), result);
        }

        @Override
        public double laplacian(Vector3dc r, double h) {
            double d2 = r.lengthSquared();
            double h2 = h*h;
            return -945 / (32 * Math.PI * iPow(h, 9)) * (d2-h2)*(7*d2-3*h2);
        }
    };

    Kernel SPIKY = new Kernel() {
        @Override
        public double kernel(Vector3dc r, double h) {
            return 15 / (Math.PI * iPow(h, 6)) * iPow(h - r.length(), 3);
        }

        @Override
        public Vector3d gradient(Vector3dc r, double h, Vector3d result) {
            double r1 = r.length();
            double d = h - r1;
            return r.mul(-45 / (Math.PI * iPow(h, 6)) * d*d / r1, result);
        }

        @Override
        public double laplacian(Vector3dc r, double h) {
            return (-90*h*h + 3*h - 2*r.lengthSquared()) / (Math.PI * iPow(h, 6) * r.length());
        }
    };

    Kernel VISCOSITY = new Kernel() {
        @Override
        public double kernel(Vector3dc r, double h) {
            double rph = r.length() / h;
            return 15 / (2 * Math.PI * iPow(h, 3)) * (-0.5*iPow(rph, 3) + rph*rph + 0.5/rph - 1);
        }

        @Override
        public Vector3d gradient(Vector3dc r, double h, Vector3d result) {
            double r1 = r.length();
            return r.mul(15 / (4 * Math.PI * iPow(h, 6)) * (-iPow(h, 4) / iPow(r1, 3) + 4*h - 3*r1), result);
        }

        @Override
        public double laplacian(Vector3dc r, double h) {
            return 45 / (Math.PI * iPow(h, 6)) * (h - r.length());
        }
    };

}
