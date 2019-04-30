package agfx.gl;

import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION;

public final class Utils {

    private Utils() {}

    public static final double EPSILON;

    static {
        double e = 1;
        while (1 + 0.5*e != 1)
            e *= 0.5;
        EPSILON = e;
    }

    public static int sizeof(int type) {
        switch (type) {
            case GL_BYTE:
            case GL_UNSIGNED_BYTE:
                return 1;
            case GL_SHORT:
            case GL_UNSIGNED_SHORT:
            case GL_HALF_FLOAT:
                return 2;
            case GL_INT:
            case GL_UNSIGNED_INT:
            case GL_FLOAT:
                return 4;
            case GL_DOUBLE:
                return 8;
            default: throw new IllegalArgumentException("Unknown OpenGL type: " + type);
        }
    }

    public static int sizeof(int... types) {
        return IntStream.of(types).map(Utils::sizeof).sum();
    }

    public static String glErrorString(int err) {
        switch (err) {
            case GL_NO_ERROR: return null;
            case GL_INVALID_ENUM: return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE: return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION: return "GL_INVALID_OPERATION";
            case GL_INVALID_FRAMEBUFFER_OPERATION: return "GL_INVALID_FRAMEBUFFER_OPERATION";
            case GL_OUT_OF_MEMORY: return "GL_OUT_OF_MEMORY";
            default: return String.format("GL_ERROR %x", err);
        }
    }

    public static void glCheckError() throws RuntimeException {
        int err = glGetError();
        if (err != GL_NO_ERROR)
        throw new RuntimeException(glErrorString(err));
    }

}
