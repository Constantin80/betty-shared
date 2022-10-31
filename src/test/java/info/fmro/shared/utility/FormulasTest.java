package info.fmro.shared.utility;

import info.fmro.shared.objects.AmountsNavigableMap;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(990d, result, "9L");

        result = Formulas.getClosestOdds(1.18518518518518518519d, Side.B);
        assertEquals(1.19d, result, "10");
        result = Formulas.getClosestOdds(1.18518518518518518519d, Side.L);
        assertEquals(1.18d, result, "10L");

        result = Formulas.getClosestOdds(3.0408163265306122449d, Side.B);
        assertEquals(3.05d, result, "11");
        result = Formulas.getClosestOdds(3.0408163265306122449d, Side.L);
        assertEquals(3d, result, "11L");

        result = Formulas.getClosestOdds(1_000.001d, Side.B);
        assertEquals(1_001d, result, "12");
        result = Formulas.getClosestOdds(1_000.001d, Side.L);
        assertEquals(1_000d, result, "12L");
    }

    @Test
    void getNextOddsDown() {
        double result = Formulas.getNextOddsDown(5d, Side.B);
        assertEquals(4.9d, result, "1");
        result = Formulas.getNextOddsDown(5d, Side.L);
        assertEquals(4.9d, result, "1L");

        result = Formulas.getNextOddsDown(1.01d, Side.B);
        assertEquals(1d, result, "2");
        result = Formulas.getNextOddsDown(1.01d, Side.L);
        assertEquals(1d, result, "2L");

        result = Formulas.getNextOddsDown(-5d, Side.B);
        assertEquals(1d, result, "3");
        result = Formulas.getNextOddsDown(-5d, Side.L);
        assertEquals(1d, result, "3L");

        result = Formulas.getNextOddsDown(2_000d, Side.B);
        assertEquals(1_001d, result, "4");
        result = Formulas.getNextOddsDown(2_000d, Side.L);
        assertEquals(1_001d, result, "4L");

        result = Formulas.getNextOddsDown(1_000.001d, Side.B);
        assertEquals(1_001d, result, "5");
        result = Formulas.getNextOddsDown(1_000.001d, Side.L);
        assertEquals(1_000d, result, "5L");

        result = Formulas.getNextOddsDown(990d, Side.B);
        assertEquals(980d, result, "6");
        result = Formulas.getNextOddsDown(990d, Side.L);
        assertEquals(980d, result, "6L");
    }

    @Test
    void getNextOddsUp() {
        double result = Formulas.getNextOddsUp(5d, Side.B);
        assertEquals(5.1d, result, "1");
        result = Formulas.getNextOddsUp(5d, Side.L);
        assertEquals(5.1d, result, "1L");

        result = Formulas.getNextOddsUp(1.01d, Side.B);
        assertEquals(1.02d, result, "2");
        result = Formulas.getNextOddsUp(1.01d, Side.L);
        assertEquals(1.02d, result, "2L");

        result = Formulas.getNextOddsUp(-5d, Side.B);
        assertEquals(1d, result, "3");
        result = Formulas.getNextOddsUp(-5d, Side.L);
        assertEquals(1d, result, "3L");

        result = Formulas.getNextOddsUp(2_000d, Side.B);
        assertEquals(1_001d, result, "4");
        result = Formulas.getNextOddsUp(2_000d, Side.L);
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
        assertEquals(1d, result, "7L");

        result = Formulas.getNextOddsUp(1_000.01d, Side.B);
        assertEquals(1_001d, result, "8");
        result = Formulas.getNextOddsUp(1_000.01d, Side.L);
        assertEquals(1_001d, result, "8L");
    }

    @Test
    void inverseOdds() {
        assertTrue(Formulas.oddsAreInverse(1d, Formulas.inverseOdds(1d, Side.L)), "i1");
        assertTrue(Formulas.oddsAreInverse(1_001d, Formulas.inverseOdds(1_001d, Side.B)), "i2");
        assertTrue(Formulas.oddsAreInverse(1d, Formulas.inverseOdds(1111111d, Side.L)), "i3");
        assertTrue(Formulas.oddsAreInverse(1_001d, Formulas.inverseOdds(0d, Side.B)), "i4");

        for (final int oddsInList : Formulas.pricesList) {
            final double doubleOdds = oddsInList / 100d;
            assertTrue(Formulas.oddsAreInverse(doubleOdds, Formulas.inverseOdds(doubleOdds, Side.B)), oddsInList + " " + Formulas.inverseOdds(doubleOdds, Side.B) + " B");
            assertTrue(Formulas.oddsAreInverse(doubleOdds, Formulas.inverseOdds(doubleOdds, Side.L)), oddsInList + " " + Formulas.inverseOdds(doubleOdds, Side.L) + " L");
        }

        assertTrue(Formulas.orderedOddsAreInverse(Formulas.inverseOdds(1d, Side.L), 1d), "oi1");
        assertTrue(Formulas.orderedOddsAreInverse(1_001d, Formulas.inverseOdds(1_001d, Side.B)), "oi2");
        assertTrue(Formulas.orderedOddsAreInverse(Formulas.inverseOdds(1111111d, Side.L), 1d), "oi3");
        assertTrue(Formulas.orderedOddsAreInverse(1_001d, Formulas.inverseOdds(0d, Side.B)), "oi4");

        for (final int oddsInList : Formulas.pricesList) {
            final double doubleOdds = oddsInList / 100d;
            assertTrue(Formulas.orderedOddsAreInverse(doubleOdds, Formulas.inverseOdds(doubleOdds, Side.B)), oddsInList + " " + Formulas.inverseOdds(doubleOdds, Side.B) + " Bo");
            assertTrue(Formulas.orderedOddsAreInverse(Formulas.inverseOdds(doubleOdds, Side.L), doubleOdds), oddsInList + " " + Formulas.inverseOdds(doubleOdds, Side.L) + " Lo");
        }
    }

    @Test
    void getBestOddsWhereICanMoveAmountsToBetterOdds() {
        final TreeMap<Double, Double> myUnmatchedBackAmounts = new TreeMap<>(Comparator.naturalOrder());
        myUnmatchedBackAmounts.put(3.0d, 5.0d);
        final NavigableMap<Double, Double> availableToLayMap = new TreeMap<>(Comparator.naturalOrder());
        availableToLayMap.put(1.7d, 105.1d);
        final AmountsNavigableMap availableToLay = new AmountsNavigableMap(availableToLayMap, Side.L);
        final double odds = Formulas.getBestOddsWhereICanMoveAmountsToBetterOdds("x", new RunnerId(0L, 0d), Side.B, new HashMap<>(2), myUnmatchedBackAmounts, availableToLay, false);
        assertEquals(0d, odds);
    }
}
