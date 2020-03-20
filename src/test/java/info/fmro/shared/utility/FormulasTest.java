package info.fmro.shared.utility;

import info.fmro.shared.stream.enums.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormulasTest {
    @Test
    void getClosestOdds() {
        double result = Formulas.getClosestOdds(5, Side.B);
        assertEquals(1d, result, "1");
        result = Formulas.getClosestOdds(5, Side.L);
        assertEquals(1d, result, "1L");

        result = Formulas.getClosestOdds(5d, Side.B);
        assertEquals(5d, result, "2");
        result = Formulas.getClosestOdds(5d, Side.L);
        assertEquals(5d, result, "2L");

        result = Formulas.getClosestOdds(0.5d, Side.B);
        assertEquals(1d, result, "3");
        result = Formulas.getClosestOdds(0.5d, Side.L);
        assertEquals(1d, result, "3L");

        result = Formulas.getClosestOdds(1_200d, Side.B);
        assertEquals(1_001d, result, "4");
        result = Formulas.getClosestOdds(1_200d, Side.L);
        assertEquals(1_001d, result, "4L");

        result = Formulas.getClosestOdds(4.22d, Side.B);
        assertEquals(4.3d, result, "5");
        result = Formulas.getClosestOdds(4.22d, Side.L);
        assertEquals(4.2d, result, "5L");

        result = Formulas.getClosestOdds(4.25d, Side.B);
        assertEquals(4.3d, result, "6");
        result = Formulas.getClosestOdds(4.25d, Side.L);
        assertEquals(4.2d, result, "6L");

        result = Formulas.getClosestOdds(4.26d, Side.B);
        assertEquals(4.3d, result, "7");
        result = Formulas.getClosestOdds(4.26d, Side.L);
        assertEquals(4.2d, result, "7L");

        result = Formulas.getClosestOdds(1.00999d, Side.B);
        assertEquals(1.01d, result, "8");
        result = Formulas.getClosestOdds(1.00999d, Side.L);
        assertEquals(1.01d, result, "8L");

        result = Formulas.getClosestOdds(999.999d, Side.B);
        assertEquals(1_000d, result, "9");
        result = Formulas.getClosestOdds(999.999d, Side.L);
        assertEquals(1_000d, result, "9L");
    }

    @Test
    void getNextOddsDown() {
        double result = Formulas.getNextOddsDown(5, Side.B);
        assertEquals(4.9d, result, "1");
        result = Formulas.getNextOddsDown(5, Side.L);
        assertEquals(4.9d, result, "1L");

        result = Formulas.getNextOddsDown(1.01d, Side.B);
        assertEquals(1d, result, "2");
        result = Formulas.getNextOddsDown(1.01d, Side.L);
        assertEquals(1d, result, "2L");

        result = Formulas.getNextOddsDown(-5, Side.B);
        assertEquals(1d, result, "3");
        result = Formulas.getNextOddsDown(-5, Side.L);
        assertEquals(1d, result, "3L");

        result = Formulas.getNextOddsDown(2_000, Side.B);
        assertEquals(1_000d, result, "4");
        result = Formulas.getNextOddsDown(2_000, Side.L);
        assertEquals(1_000d, result, "4L");

        result = Formulas.getNextOddsDown(1_000.001d, Side.B);
        assertEquals(1_000d, result, "5");
        result = Formulas.getNextOddsDown(1_000.001d, Side.L);
        assertEquals(1_000d, result, "5L");

        result = Formulas.getNextOddsDown(990d, Side.B);
        assertEquals(980d, result, "6");
        result = Formulas.getNextOddsDown(990d, Side.L);
        assertEquals(980d, result, "6L");
    }

    @Test
    void getNextOddsUp() {
        double result = Formulas.getNextOddsUp(5, Side.B);
        assertEquals(5.1d, result, "1");
        result = Formulas.getNextOddsUp(5, Side.L);
        assertEquals(5.1d, result, "1L");

        result = Formulas.getNextOddsUp(1.01d, Side.B);
        assertEquals(1.02d, result, "2");
        result = Formulas.getNextOddsUp(1.01d, Side.L);
        assertEquals(1.02d, result, "2L");

        result = Formulas.getNextOddsUp(-5, Side.B);
        assertEquals(1.01d, result, "3");
        result = Formulas.getNextOddsUp(-5, Side.L);
        assertEquals(1.01d, result, "3L");

        result = Formulas.getNextOddsUp(2_000, Side.B);
        assertEquals(1_001d, result, "4");
        result = Formulas.getNextOddsUp(2_000, Side.L);
        assertEquals(1_001d, result, "4L");

        result = Formulas.getNextOddsUp(1_000.001d, Side.B);
        assertEquals(1_001d, result, "5");
        result = Formulas.getNextOddsUp(1_000.001d, Side.L);
        assertEquals(1_001d, result, "5L");

        result = Formulas.getNextOddsUp(990d, Side.B);
        assertEquals(1_000d, result, "6");
        result = Formulas.getNextOddsUp(990d, Side.L);
        assertEquals(1_000d, result, "6L");

        result = Formulas.getNextOddsUp(1.009d, Side.B);
        assertEquals(1.01d, result, "7");
        result = Formulas.getNextOddsUp(1.009d, Side.L);
        assertEquals(1.01d, result, "7L");

        result = Formulas.getNextOddsUp(1_000.01d, Side.B);
        assertEquals(1_001d, result, "8");
        result = Formulas.getNextOddsUp(1_000.01d, Side.L);
        assertEquals(1_001d, result, "8L");
    }
}
