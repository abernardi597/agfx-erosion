import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;

import java.util.Optional;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Main {
    private Main() {}

    private static void init() {
        System.out.println("LWJGL " + Version.getVersion());
        System.out.println("GLFW " + glfwGetVersionString());
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new RuntimeException("GLFW failed to initialize");
    }

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

        return window;
    }

    public static void main(String[] args) {
        init();
        final long window = makeWindow(1024, 800, "agfx-erosion");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        try {
            main(window);
        } finally {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);

            glfwTerminate();
            Optional.ofNullable(glfwSetErrorCallback(null)).ifPresent(Callback::free);
        }
    }

    private static void main(final long window) {
        GL.createCapabilities();
        glClearColor(0, 0, 0, 0);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

}
