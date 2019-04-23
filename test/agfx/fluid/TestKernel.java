package agfx.fluid;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestKernel {

    private static final Random rng = new Random();
    private static double EPSILON;

    @BeforeAll
    static void computeEpsilon() {
        EPSILON = 1;
        while (1 + 0.5*EPSILON != 1)
            EPSILON *= 0.5;
        System.out.println("EPSILON: " + EPSILON);
    }

    @RepeatedTest(100)
    void testPow() {
        double d = rng.nextDouble();
        int i = rng.nextInt(Short.MAX_VALUE);
        double actual = Kernel.iPow(d, i);
        double expected = StrictMath.pow(d, i);
        assertEquals(expected, actual, EPSILON, String.format("%f^%d", d, i));
    }

}
