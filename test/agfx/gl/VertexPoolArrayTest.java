package agfx.gl;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public class VertexPoolArrayTest {

    @Test
    void testRequest() {
        int count = 10;
        int size = 3;
        try (VertexPoolArray pool = new VertexPoolArray(count, size)) {
            for (int i = 0; i < count; ++i) {
                assertEquals(i, pool.used());
                assertEquals(count - i, pool.free());
                pool.request();
            }
            assertThrows(NoSuchElementException.class, pool::request);
        }
    }

    @Test
    void testReclaim() {
        int count = 8;
        int size = 4;
        try (VertexPoolArray pool = new VertexPoolArray(count, size)) {
            int[] v = new int[count];
            for (int i = 0; i < count; ++i) {
                assertEquals(i, pool.used());
                assertEquals(count - i, pool.free());
                v[i] = pool.request();
            }

            assertEquals(0, pool.free());
            assertEquals(count, pool.used());
            assertThrows(NoSuchElementException.class, pool::request);

            for (int i = 0; i < count; ++i) {
                assertEquals(i, pool.free());
                assertEquals(count - i, pool.used());
                pool.reclaim(v[i]);
            }

            assertEquals(0, pool.used());
            assertEquals(count, pool.free());

            for (int vi : v)
                assertThrows(NoSuchElementException.class, () -> pool.reclaim(vi));
        }
    }

    @Test
    void testCompact() {
        int count = 10;

        try (VertexPoolArray pool = new VertexPoolArray(count, Integer.BYTES)) {
            int[] v = new int[count];
            for (int i = 0; i < count; ++i) {
                v[i] = pool.request();
                pool.putInt(i, 0, i);
            }

            for (int i = 0; i < count; i += 2)
                pool.reclaim(v[i]);

            Map<Integer, Integer> map = pool.compact();
            for (int i = 0; i < count; ++i) {
                if (i % 2 == 0)
                    assertFalse(map.containsKey(v[i]));
                else assertEquals(i, pool.getInt(map.getOrDefault(v[i], v[i]), 0));
            }

            assertArrayEquals(new int[] {0, 1, 2, 3, 4}, pool.streamUsed().toArray());
            assertArrayEquals(new int[] {5, 6, 7, 8, 9}, pool.streamFree().toArray());
        }
    }

}
