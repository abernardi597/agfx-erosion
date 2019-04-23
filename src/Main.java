import agfx.fluid.Fluid;
import agfx.fluid.SPH;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.util.Optional;
import java.util.Stack;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Main {
    private Main() {}

    private static final Vector2d MOUSE = new Vector2d();
    private static final Vector2d ROTATION = new Vector2d();

    private static long makeWindow(int width, int height, String title) {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        final long window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) throw new RuntimeException("GLFW failed to create window");

        GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (mode == null) throw new RuntimeException("Unable to get video mode of primary monitor");

        glfwSetWindowPos(
                window,
                (mode.width() - width) / 2,
                (mode.height() - height) / 2
        );

        glfwSetCursorPosCallback(window, GLFWCursorPosCallback.create((window1, xpos, ypos) -> {
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS)
                ROTATION.add(new Vector2d(xpos, ypos).sub(MOUSE).perpendicular().mul(1.0 / 100, -1.0 / 100));
            MOUSE.set(xpos, ypos);
        }));

        return window;
    }

    public static void main(String[] args) {
        Stack<Runnable> cleanup = new Stack<>();
        try {
            System.out.println("LWJGL " + Version.getVersion());
            System.out.println("GLFW " + glfwGetVersionString());
            GLFWErrorCallback.createPrint(System.err).set();
            cleanup.push(() -> Optional.ofNullable(glfwSetErrorCallback(null)).ifPresent(Callback::free));
            if (!glfwInit()) throw new RuntimeException("GLFW failed to initialize");
            cleanup.push(org.lwjgl.glfw.GLFW::glfwTerminate);
            final long window = makeWindow(800, 800, "agfx-erosion");
            cleanup.push(() -> glfwFreeCallbacks(window));
            cleanup.push(() -> glfwDestroyWindow(window));

            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);
            glfwShowWindow(window);

            GL.createCapabilities();
            cleanup.push(GL::destroy);

            int count = 1 << 13;
            double h = 1.0 / 8;
            double r = 0.4;
            SPH sph = new SPH(count, h, new Vector3d(1, 1, 1));
            Fluid water = new Fluid(0.01801528, 995.7, 101325, 60, 1498, 0.001);
            sph.gravity.set(0, -9.8, 0);

            for (int i = 0; i < count; ++i) {
                SPH.Particle p = new SPH.Particle(water, water.restDensity / count * 4 / 3 * Math.PI * r*r*r);
                do {
                    p.pos.set(Math.random(), Math.random(), Math.random());
                } while (p.pos.distanceSquared(0.5, 0.5, 0.5) > r*r);
                sph.particles.add(p);
            }

            glClearColor(0, 0, 0, 0);
            glEnable(GL_POINT_SMOOTH);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glPointSize(8);
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, 1, 0, 1, 0, 10);
            double dt = 0.01;
            while (!glfwWindowShouldClose(window)) {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    DoubleBuffer buf = stack.mallocDouble(16);

                    new Matrix4d().lookAt(0, 0, 3, 0, 0, 0, 0, 1, 0).translate(0.5, 0.5, 0.5).scale(0.5, 0.5, 0.5).arcball(1, 0, 0, 0, ROTATION.x, ROTATION.y).translate(-0.5, -0.5, -0.5).get(buf);
                    glMatrixMode(GL_MODELVIEW);
                    glLoadMatrixd(buf);
                }

                // Vector3d v = new Vector3d();
                // glBegin(GL_LINES);
                // for (Particle p : sph.particles) {
                //     // p.vel.mul(5 * dt, v);
                //     // glColor4d(1, 0, 0, 0.5);
                //     // glVertex3d(p.pos.x, p.pos.y, p.pos.z);
                //     // glVertex3d(p.pos.x + v.x, p.pos.y + v.y, p.pos.z + v.z);
                //     //
                //     // p.acc.mul(0.5 * dt, v);
                //     // glColor4d(0, 1, 0, 0.5);
                //     // glVertex3d(p.pos.x, p.pos.y, p.pos.z);
                //     // glVertex3d(p.pos.x + v.x, p.pos.y + v.y, p.pos.z + v.z);
                //     glColor4d(1,1,1,0.2);
                //     v.set(p.normal);
                //     if (v.lengthSquared() > 0) {
                //         v.normalize().mul(0.05);
                //         p.pos.sub(v, v);
                //         glVertex3d(p.pos.x, p.pos.y, p.pos.z);
                //         glVertex3d(v.x, v.y, v.z);
                //     }
                // }
                // glEnd();

                glBegin(GL_POINTS);
                glColor4d(1, 1, 1, 0.5);
                for (SPH.Particle p : sph.particles) {
                    // if (!p.fixed) glColor4d(p.density / p.fluid.restDensity, p.pressure / p.fluid.restPressure, p.normal.length(), 0.5);
                    // else glColor4d(1, 1, 1, 0.2);
                    glVertex3d(p.pos.x, p.pos.y, p.pos.z);
                }
                glEnd();

                glColor3d(0.5, 0.5, 0.5);
                glBegin(GL_LINE_LOOP);
                glVertex3f(0, 0, 0);
                glVertex3f(0, 0, 1);
                glVertex3f(0, 1, 1);
                glVertex3f(0, 1, 0);
                glVertex3f(1, 1, 0);
                glVertex3f(1, 1, 1);
                glVertex3f(1, 0, 1);
                glVertex3f(1, 0, 0);
                glEnd();

                sph.update(dt);

                glfwSwapBuffers(window);
                glfwPollEvents();
            }
        } finally {
            while (!cleanup.isEmpty())
                cleanup.pop().run();
        }
    }

}
