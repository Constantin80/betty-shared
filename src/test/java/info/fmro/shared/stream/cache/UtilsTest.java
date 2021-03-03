package info.fmro.shared.stream.cache;

import com.google.common.math.DoubleMath;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {
    @Test
    void getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure() {
        final ManagedRunner firstRunner = new ManagedRunner("marketId", new RunnerId(1L, 0d), new AtomicBoolean(), new AtomicBoolean()),
                secondRunner = new ManagedRunner("marketId", new RunnerId(1L, 0d), new AtomicBoolean(), new AtomicBoolean());
        final List<Side> sidesToPlaceExposureOn = List.of(Side.B, Side.L);
        final double excessMatchedExposure = 2d;
        {
            final @NotNull List<Double> result = Utils.getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure);
            assertAll("1",
                      () -> assertEquals(0d, result.get(0), "firstExposure"),
                      () -> assertEquals(0d, result.get(1), "secondExposure"));
        }

        ReflectionTestUtils.setField(firstRunner, "backAmountLimit", 10d);
        ReflectionTestUtils.setField(secondRunner, "layAmountLimit", 10d);
        {
            final @NotNull List<Double> result = Utils.getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure);
            assertAll("2",
                      () -> assertEquals(0d, result.get(0), "firstExposure"),
                      () -> assertEquals(0d, result.get(1), "secondExposure"));
        }

        ReflectionTestUtils.setField(firstRunner, "minBackOdds", 100d);
        ReflectionTestUtils.setField(secondRunner, "maxLayOdds", 2d);
        {
            final @NotNull List<Double> result = Utils.getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure);
            assertAll("3",
                      () -> assertEquals(0d, result.get(0), "firstExposure"),
                      () -> assertEquals(0d, result.get(1), "secondExposure"));
        }

        ReflectionTestUtils.setField(firstRunner, "idealBackExposure", 9d);
        ReflectionTestUtils.setField(secondRunner, "idealLayExposure", 7d);
        {
            final @NotNull List<Double> result = Utils.getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure);
            assertAll("4",
                      () -> assertTrue(DoubleMath.fuzzyEquals(result.get(0), 0.6730689340498313, .00001d), "firstExposure"),
                      () -> assertTrue(DoubleMath.fuzzyEquals(result.get(1), 1.3269310659501687, .00001d), "secondExposure"));
        }
    }

    @Test
    void getExposureToBePlacedForTwoWayMarket() {
        final ManagedRunner firstRunner = new ManagedRunner("marketId", new RunnerId(1L, 0d), new AtomicBoolean(), new AtomicBoolean()),
                secondRunner = new ManagedRunner("marketId", new RunnerId(1L, 0d), new AtomicBoolean(), new AtomicBoolean());
        final List<Side> sidesToPlaceExposureOn = List.of(Side.B, Side.L);
        final double availableLimit = 2d;
        {
            final @NotNull List<Double> result = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, availableLimit);
            assertAll("1",
                      () -> assertEquals(0d, result.get(0), "firstExposure"),
                      () -> assertEquals(0d, result.get(1), "secondExposure"));
        }

        ReflectionTestUtils.setField(firstRunner, "backAmountLimit", 10d);
        ReflectionTestUtils.setField(secondRunner, "layAmountLimit", 10d);
        {
            final @NotNull List<Double> result = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, availableLimit);
            assertAll("2",
                      () -> assertEquals(0d, result.get(0), "firstExposure"),
                      () -> assertEquals(0d, result.get(1), "secondExposure"));
        }

        ReflectionTestUtils.setField(firstRunner, "minBackOdds", 100d);
        ReflectionTestUtils.setField(secondRunner, "maxLayOdds", 2d);
        {
            final @NotNull List<Double> result = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, availableLimit);
            assertAll("3",
                      () -> assertEquals(0d, result.get(0), "firstExposure"),
                      () -> assertEquals(0d, result.get(1), "secondExposure"));
        }

        ReflectionTestUtils.setField(firstRunner, "idealBackExposure", 9d);
        ReflectionTestUtils.setField(secondRunner, "idealLayExposure", 7d);
        {
            final @NotNull List<Double> result = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, availableLimit);
            assertAll("4",
                      () -> assertTrue(DoubleMath.fuzzyEquals(result.get(0), 0.6730689340498313, .00001d), "firstExposure"),
                      () -> assertTrue(DoubleMath.fuzzyEquals(result.get(1), 1.3269310659501687, .00001d), "secondExposure"));
        }
    }
}
