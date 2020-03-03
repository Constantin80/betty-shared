package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormulasTest {
    @Test
    void getClosestOdds() {
        double result = Formulas.getClosestOdds(5);
        assertEquals(1d, result, "1");

        result = Formulas.getClosestOdds(5d);
        assertEquals(5d, result, "2");

        result = Formulas.getClosestOdds(0.5d);
        assertEquals(1d, result, "3");

        result = Formulas.getClosestOdds(1_200d);
        assertEquals(1_001d, result, "4");

        result = Formulas.getClosestOdds(4.22d);
        assertEquals(4.2d, result, "5");

        result = Formulas.getClosestOdds(4.25d);
        assertEquals(4.2d, result, "6");

        result = Formulas.getClosestOdds(4.26d);
        assertEquals(4.3d, result, "7");
    }

    @Test
    void getNextOddsDown() {
        double result = Formulas.getNextOddsDown(5);
        assertEquals(4.9d, result, "1");

        result = Formulas.getNextOddsDown(1.01d);
        assertEquals(1d, result, "2");

        result = Formulas.getNextOddsDown(-5);
        assertEquals(1d, result, "3");

        result = Formulas.getNextOddsDown(2_000);
        assertEquals(1_000d, result, "4");

        result = Formulas.getNextOddsDown(1_000.001d);
        assertEquals(1_000d, result, "5");

        result = Formulas.getNextOddsDown(990d);
        assertEquals(980d, result, "6");
    }

    @Test
    void getNextOddsUp() {
        double result = Formulas.getNextOddsUp(5);
        assertEquals(5.1d, result, "1");

        result = Formulas.getNextOddsUp(1.01d);
        assertEquals(1.02d, result, "2");

        result = Formulas.getNextOddsUp(-5);
        assertEquals(1.01d, result, "3");

        result = Formulas.getNextOddsUp(2_000);
        assertEquals(1_001d, result, "4");

        result = Formulas.getNextOddsUp(1_000.001d);
        assertEquals(1_001d, result, "5");

        result = Formulas.getNextOddsUp(990d);
        assertEquals(1_000d, result, "6");

        result = Formulas.getNextOddsUp(1.009d);
        assertEquals(1.01d, result, "7");

        result = Formulas.getNextOddsUp(1_000.01d);
        assertEquals(1_001d, result, "8");
    }
}
