package agfx.gl;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

public class VertexPoolArray extends VertexArray {

    private static void move(ByteBuffer buf, int src, int dst, int len) {
        buf = buf.slice();
        buf.position(src);
        buf.limit(src + len);
        ByteBuffer bytes = buf.slice();

        buf.position(dst);
        buf.limit(dst + len);
        buf.put(bytes);
    }

    private final BitSet used;
    private int inUse;

    public VertexPoolArray(int initialCapacity, int size) {
        super(initialCapacity, size);
        used = new BitSet(initialCapacity);
        inUse = 0;
    }

    @Override
    public void close() {
        super.close();
        used.clear();
        inUse = 0;
    }

    public int used() {
        return inUse;
    }

    public int free() {
        return blocks() - inUse;
    }

    public int request() throws NoSuchElementException {
        int v = streamFree().findAny().orElseThrow();
        used.set(v);
        ++inUse;
        return v;
    }

    public void reclaim(int v) throws NoSuchElementException {
        if (used.get(v)) {
            used.clear(v);
            --inUse;
        } else throw new NoSuchElementException("Index " + v + " not in use");
    }

    public Map<Integer, Integer> compact() {
        Map<Integer, Integer> map = new HashMap<>();
        int len = blocks();
        int firstFree = used.nextClearBit(0);
        int lastUsed = used.previousSetBit(len);
        while (firstFree < lastUsed) {
            move(buffer, lastUsed * blockSize, firstFree * blockSize, blockSize);
            used.set(firstFree);
            used.clear(lastUsed);
            map.put(lastUsed, firstFree);
            firstFree = used.nextClearBit(firstFree);
            lastUsed = used.previousSetBit(lastUsed);
        }
        return map;
    }

    public IntStream streamUsed() {
        return used.stream();
    }

    public IntStream streamFree() {
        return IntStream.range(0, blocks()).filter(i -> !used.get(i));
    }


}
