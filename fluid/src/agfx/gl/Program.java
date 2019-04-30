package agfx.gl;

import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import static org.lwjgl.opengl.GL20.*;

public class Program implements AutoCloseable {

    public static void glLoadShader(int shader, File src) throws IOException, IllegalArgumentException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(src))) {
            String line;
            while ((line = in.readLine()) != null)
                sb.append(line).append(System.lineSeparator());
        }
        glShaderSource(shader, sb.toString());
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
            throw new IllegalArgumentException("Unable to compile shader: " + glGetShaderInfoLog(shader).trim());
    }

    public final int id;
    private final HashSet<Integer> shaders;

    public Program() {
        this.id = glCreateProgram();
        this.shaders = new HashSet<>();

        if (id == 0) throw new IllegalStateException("Unable to create new program");
    }

    public Program attach(int shader) {
        glAttachShader(id, shader);
        return this;
    }

    public Program attach(int type, File src) throws IOException, IllegalArgumentException {
        int shader = glCreateShader(type);
        shaders.add(shader);
        glLoadShader(shader, src);
        return attach(shader);
    }

    public Program link() throws IllegalStateException {
        glLinkProgram(id);
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE)
            throw new IllegalStateException("Unable to link program: " + glGetProgramInfoLog(id).trim());
        return this;
    }

    @Override
    public void close() {
        shaders.forEach(GL20::glDeleteShader);
        glDeleteProgram(id);
    }
}
