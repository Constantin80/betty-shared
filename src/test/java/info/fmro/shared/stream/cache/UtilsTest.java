package info.fmro.shared.stream.cache;

import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {
// todo get these tests done, once the program can build
    @Test
    void getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure() {
        final ManagedRunner firstRunner = new ManagedRunner("marketId", new RunnerId(1L, 0d), new AtomicBoolean(), new AtomicBoolean()),
                secondRunner = new ManagedRunner("marketId", new RunnerId(1L, 0d), new AtomicBoolean(), new AtomicBoolean());
        final List<Side> sidesToPlaceExposureOn = List.of(Side.B, Side.L);
        final double excessMatchedExposure = 2d;
        @NotNull final List<Double> result = Utils.getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure);
        assertEquals(0d, result.get(0), "firstExposure");
        assertEquals(1d, result.get(1), "secondExposure");
    }

    @Test
    void getExposureToBePlacedForTwoWayMarket() {
        final ManagedRunner firstRunner = new ManagedRunner("marketId", new RunnerId(1L, 0d), new AtomicBoolean(), new AtomicBoolean()),
                secondRunner = new ManagedRunner("marketId", new RunnerId(1L, 0d), new AtomicBoolean(), new AtomicBoolean());
        final List<Side> sidesToPlaceExposureOn = List.of(Side.B, Side.L);
        final double availableLimit = 2d;
        @NotNull final List<Double> result = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, availableLimit);
        assertEquals(0d, result.get(0), "firstExposure");
        assertEquals(1d, result.get(1), "secondExposure");
    }
}
