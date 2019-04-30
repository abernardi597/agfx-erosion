import agfx.Cloud;
import agfx.fluid.Fluid;
import agfx.fluid.SPH;
import agfx.gl.Program;
import agfx.gl.Utils;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Optional;
import java.util.Random;
import java.util.Stack;

import static agfx.gl.Utils.glCheckError;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Main {
    private Main() {}

    private static final int WIDTH = 256;
    private static final int HEIGHT = 256;

    public static void main(String[] args) {
        Stack<AutoCloseable> cleanup = new Stack<>();
        try {
            System.out.println("LWJGL " + Version.getVersion());
            System.out.println("GLFW " + glfwGetVersionString());
            GLFWErrorCallback.createPrint(System.err).set();
            cleanup.push(() -> Optional.ofNullable(glfwSetErrorCallback(null)).ifPresent(Callback::free));
            if (!glfwInit()) throw new RuntimeException("GLFW failed to initialize");
            cleanup.push(org.lwjgl.glfw.GLFW::glfwTerminate);
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

            final long window = glfwCreateWindow(WIDTH, HEIGHT, "agfx-erosion", NULL, NULL);
            if (window == NULL) throw new RuntimeException("GLFW failed to create window");
            cleanup.push(() -> glfwFreeCallbacks(window));
            cleanup.push(() -> glfwDestroyWindow(window));

            GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (mode == null) throw new RuntimeException("Unable to get video mode of primary monitor");

            glfwSetWindowPos(
                    window,
                    (mode.width() - WIDTH) / 2,
                    (mode.height() - HEIGHT) / 2
            );

            Vector2d MOUSE = new Vector2d();
            Vector2d ROTATION = new Vector2d();
            glfwSetCursorPosCallback(window, GLFWCursorPosCallback.create((window1, xpos, ypos) -> {
                if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS)
                    ROTATION.add(new Vector2d(xpos, ypos).sub(MOUSE).perpendicular().mul(1.0 / 100, -1.0 / 100));
                MOUSE.set(xpos, ypos);
            }));

            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);

            GL.createCapabilities();
            cleanup.push(GL::destroy);

            Cloud cloud = Cloud.sphere(0.5, new Vector3d(0, 0, 0), 1 << 14, new Random(28));
            // Cloud cloud = Cloud.load(new FileInputStream("res/clouds/teapot_10000.txt"));
            int count = cloud.points.size();

            Vector3dc extent = new Vector3d(cloud.max).sub(cloud.min);
            double h = 1.0 / 12;
            double volume = 4*Math.PI/3 * 0.5*0.5*0.5;

            SPH sph = new SPH(count, h, cloud.min,  cloud.max);
            Fluid water = new Fluid(0.01801528, 995.7, 101325, 25, 1498, 0.001);
            sph.gravity.set(0, -9.8, 0);

            for (Cloud.Point point : cloud.points) {
                SPH.Particle p = sph.addParticle(water, water.restDensity / count * volume);
                p.pos.set(point.vertex);
                p.normal.set(point.normal).negate();
                p.pack();
            }

            int vao = glGenVertexArrays();
            cleanup.push(() -> glDeleteVertexArrays(vao));
            glBindVertexArray(vao);

            int vbo = glGenBuffers();
            cleanup.push(() -> glDeleteBuffers(vbo));
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Utils.sizeof(GL_FLOAT), 0);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Utils.sizeof(GL_FLOAT), 12);
            glEnableVertexAttribArray(1);
            glCheckError();
            IntBuffer indices = MemoryUtil.memAllocInt(count);
            cleanup.push(() -> MemoryUtil.memFree(indices));

            Program program = new Program();
            cleanup.push(program);
            program.attach(GL_VERTEX_SHADER, new File("res/shader/splat.v.glsl"));
            program.attach(GL_FRAGMENT_SHADER, new File("res/shader/splat.f.glsl"));
            program.link();
            glUseProgram(program.id);

            int u_mvp = glGetUniformLocation(program.id, "u_mvp");
            FloatBuffer mvp_buf = MemoryUtil.memAllocFloat(16);
            cleanup.push(() -> MemoryUtil.memFree(mvp_buf));

            glClearColor(0, 0, 0, 0);
            glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);
            glEnable(GL_POINT_SPRITE);
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            double dt = 0.002;

            glfwShowWindow(window);
            while (!glfwWindowShouldClose(window)) {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                if (glfwGetWindowAttrib(window, GLFW_HOVERED) != 0 && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_RELEASE)
                    sph.update(dt);
                glBufferData(GL_ARRAY_BUFFER, sph.pool.buffer(), GL_DYNAMIC_DRAW);

                Matrix4d mvp = new Matrix4d();
                mvp.setOrtho(-1, 1, -1, 1, 0, 10);
                mvp.lookAt(0, 0, 5, 0, 0, 0, 0, 1, 0);
                mvp.arcball(1, 0, 0, 0, ROTATION.x, ROTATION.y);
                mvp.scale(1/extent.get(extent.maxComponent()));
                mvp.translate(extent.mul(0.5, new Vector3d()).add(cloud.min).negate());
                mvp.get(mvp_buf);
                glUniformMatrix4fv(u_mvp, false, mvp_buf);

                indices.clear();
                sph.pool.streamUsed().forEach(indices::put);
                indices.flip();

                glDrawElements(GL_POINTS, indices);

                glfwSwapBuffers(window);
                glfwPollEvents();
                glCheckError();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            while (!cleanup.isEmpty()) {
                try {
                    cleanup.pop().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
