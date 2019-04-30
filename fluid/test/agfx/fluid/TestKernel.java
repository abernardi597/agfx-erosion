package agfx.fluid;

import agfx.gl.Utils;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestKernel {

    private static final Random rng = new Random();

    @RepeatedTest(100)
    void testPow() {
        double d = rng.nextDouble();
        int i = rng.nextInt(Short.MAX_VALUE);
        double actual = Kernel.iPow(d, i);
        double expected = StrictMath.pow(d, i);
        assertEquals(expected, actual, Utils.EPSILON, String.format("%f^%d", d, i));
    }

}
