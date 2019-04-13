package agfx.gl;

import org.joml.*;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VertexArrayTest {

    @Test
    void testLifecycle() {
        try (VertexArray a = new VertexArray(1, 1)) {
            assertEquals(1, a.buffer().capacity());
            assertEquals(1, a.bytes());
            assertEquals(1, a.blocks());
        }
    }

    @Test
    void testSize() {
        try (VertexArray a = new VertexArray(1, 16)) {
            assertEquals(16, a.bytes());
            assertEquals(16, a.buffer().capacity());
            assertEquals(1, a.blocks());
        }

        try (VertexArray a = new VertexArray(16, 1)) {
            assertEquals(16, a.bytes());
            assertEquals(16, a.buffer().capacity());
            assertEquals(16, a.blocks());
        }

        try (VertexArray a = new VertexArray(4, 4)) {
            assertEquals(16, a.bytes());
            assertEquals(16, a.buffer().capacity());
            assertEquals(4, a.blocks());
        }
    }

    @Test
    void testPutGet() {
        Random rng = new Random();
        byte b = (byte) rng.nextInt();
        int ub = Byte.toUnsignedInt((byte) rng.nextInt());
        short s = (short) rng.nextInt();
        int us = Short.toUnsignedInt((byte) rng.nextInt());
        int i = rng.nextInt();
        long ui = Integer.toUnsignedLong(rng.nextInt());
        float f = rng.nextFloat();
        double d = rng.nextDouble();
        Vector2ic v2i = new Vector2i(rng.nextInt(), rng.nextInt());
        Vector3ic v3i = new Vector3i(rng.nextInt(), rng.nextInt(), rng.nextInt());
        Vector4ic v4i = new Vector4i(rng.nextInt(), rng.nextInt(), rng.nextInt(), rng.nextInt());
        Vector2fc v2f = new Vector2f(rng.nextFloat(), rng.nextFloat());
        Vector3fc v3f = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
        Vector4fc v4f = new Vector4f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
        Vector2dc v2d = new Vector2d(rng.nextDouble(), rng.nextDouble());
        Vector3dc v3d = new Vector3d(rng.nextDouble(), rng.nextDouble(), rng.nextDouble());
        Vector4dc v4d = new Vector4d(rng.nextDouble(), rng.nextDouble(), rng.nextDouble(), rng.nextDouble());
        try (VertexArray a = new VertexArray(1, 170)) {
            a.putByte(0, 0, b);            assertEquals(b, a.getByte(0, 0));
            a.putUnsignedByte(0, 1, ub);   assertEquals(ub, a.getUnsignedByte(0, 1));
            a.putShort(0, 2, s);           assertEquals(s, a.getShort(0, 2));
            a.putUnsignedShort(0, 4, us);  assertEquals(us, a.getUnsignedShort(0, 4));
            a.putInt(0, 6, i);             assertEquals(i, a.getInt(0, 6));
            a.putUnsignedInt(0, 10, ui);   assertEquals(ui, a.getUnsignedInt(0, 10));
            a.putFloat(0, 14, f);          assertEquals(f, a.getFloat(0, 14));
            a.putDouble(0, 18, d);         assertEquals(d, a.getDouble(0, 18));
            a.put(0, 26, v2i::get);       assertEquals(v2i, a.get(0, 26, Vector2i::new));
            a.put(0, 34, v3i::get);       assertEquals(v3i, a.get(0, 34, Vector3i::new));
            a.put(0, 46, v4i::get);       assertEquals(v4i, a.get(0, 46, Vector4i::new));
            a.put(0, 62, v2f::get);       assertEquals(v2f, a.get(0, 62, Vector2f::new));
            a.put(0, 70, v3f::get);       assertEquals(v3f, a.get(0, 70, Vector3f::new));
            a.put(0, 82, v4f::get);       assertEquals(v4f, a.get(0, 82, Vector4f::new));
            a.put(0, 98, v2d::get);       assertEquals(v2d, a.get(0, 98, Vector2d::new));
            a.put(0, 114, v3d::get);      assertEquals(v3d, a.get(0, 114, Vector3d::new));
            a.put(0, 138, v4d::get);      assertEquals(v4d, a.get(0, 138, Vector4d::new));


            assertEquals(b, a.getByte(0, 0));
            assertEquals(ub, a.getUnsignedByte(0, 1));
            assertEquals(s, a.getShort(0, 2));
            assertEquals(us, a.getUnsignedShort(0, 4));
            assertEquals(i, a.getInt(0, 6));
            assertEquals(ui, a.getUnsignedInt(0, 10));
            assertEquals(f, a.getFloat(0, 14));
            assertEquals(d, a.getDouble(0, 18));
            assertEquals(v2i, a.get(0, 26, Vector2i::new));
            assertEquals(v3i, a.get(0, 34, Vector3i::new));
            assertEquals(v4i, a.get(0, 46, Vector4i::new));
            assertEquals(v2f, a.get(0, 62, Vector2f::new));
            assertEquals(v3f, a.get(0, 70, Vector3f::new));
            assertEquals(v4f, a.get(0, 82, Vector4f::new));
            assertEquals(v2d, a.get(0, 98, Vector2d::new));
            assertEquals(v3d, a.get(0, 114, Vector3d::new));
            assertEquals(v4d, a.get(0, 138, Vector4d::new));
        }
    }

    @Test
    void testResize() {
        try (VertexArray a = new VertexArray(1, Integer.BYTES)) {
            a.putInt(0, 0, 0);
            for (int count : new int[] { 1, 2, 4, 8, 5, 100, 3, 2048}) {
                int old = a.blocks();

                a.resize(count);
                assertEquals(count, a.blocks());

                while (old < count)
                    a.putInt(old, 0, old++);

                for (int i = 0; i < count; ++i)
                    assertEquals(i, a.getInt(i, 0));

            }
        }
    }

}
