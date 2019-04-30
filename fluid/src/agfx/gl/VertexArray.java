package agfx.gl;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class VertexArray implements AutoCloseable {

    protected final int blockSize;
    protected ByteBuffer buffer;

    public VertexArray(int count, int size) {
        blockSize = size;
        buffer = MemoryUtil.memAlloc(count * size);
    }

    protected int index(int vertex, int format_index) {
        return vertex * blockSize + format_index;
    }

    @Override
    public void close() {
        if (buffer != null) {
            MemoryUtil.memFree(buffer);
            buffer = null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes(); ++i) {
            if (i != 0 && i % blockSize == 0)
                sb.append(' ');
            sb.append(String.format("%02x ", Byte.toUnsignedInt(buffer.get(i))));
        }
        return sb.toString().trim();
    }

    public void resize(int count) {
        buffer = MemoryUtil.memRealloc(buffer, count * blockSize);
    }

    public int bytes() {
        return buffer.limit();
    }

    public int blocks() {
        return bytes() / blockSize;
    }

    public ByteBuffer buffer() {
        if (buffer == null) throw new IllegalStateException("Already freed");
        return buffer.asReadOnlyBuffer();
    }

    public byte getByte(int vertex, int format_index) {
        return buffer.get(index(vertex, format_index));
    }

    public int getUnsignedByte(int vertex, int format_index) {
        return Byte.toUnsignedInt(getByte(vertex, format_index));
    }

    public short getShort(int vertex, int format_index) {
        return buffer.getShort(index(vertex, format_index));
    }

    public int getUnsignedShort(int vertex, int format_index) {
        return Short.toUnsignedInt(getShort(vertex, format_index));
    }

    public int getInt(int vertex, int format_index) {
        return buffer.getInt(index(vertex, format_index));
    }

    public long getUnsignedInt(int vertex, int format_index) {
        return Integer.toUnsignedLong(getInt(vertex, format_index));
    }

    public float getFloat(int vertex, int format_index) {
        return buffer.getFloat(index(vertex, format_index));
    }

    public double getDouble(int vertex, int format_index) {
        return buffer.getDouble(index(vertex, format_index));
    }

    public <T> T get(int vertex, int format_index, BiFunction<Integer, ByteBuffer, ? extends T> getter) {
        return getter.apply(index(vertex, format_index), buffer.asReadOnlyBuffer());
    }

    public void putByte(int vertex, int format_index, byte b) {
        buffer.put(index(vertex, format_index), b);
    }

    public void putUnsignedByte(int vertex, int format_index, int ub) {
        buffer.put(index(vertex, format_index), (byte) ub);
    }

    public void putShort(int vertex, int format_index, short s) {
        buffer.putShort(index(vertex, format_index), s);
    }

    public void putUnsignedShort(int vertex, int format_index, int us) {
        buffer.putShort(index(vertex, format_index), (short) us);
    }

    public void putInt(int vertex, int format_index, int i) {
        buffer.putInt(index(vertex, format_index), i);
    }

    public void putUnsignedInt(int vertex, int format_index, long ui) {
        buffer.putInt(index(vertex, format_index), (int) ui);
    }

    public void putFloat(int vertex, int format_index, float f) {
        buffer.putFloat(index(vertex, format_index), f);
    }

    public void putDouble(int vertex, int format_index, double d) {
        buffer.putDouble(index(vertex, format_index), d);
    }

    public void put(int vertex, int format_index, BiConsumer<Integer, ByteBuffer> putter) {
        putter.accept(index(vertex, format_index), buffer);
    }

}
