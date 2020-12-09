package info.fmro.shared.stream.cache.market;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.OrdersList;
import info.fmro.shared.stream.cache.RunnerOrderModification;
import info.fmro.shared.stream.definitions.LevelPriceSizeLadder;
import info.fmro.shared.stream.definitions.RunnerChange;
import info.fmro.shared.stream.definitions.RunnerDefinition;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;

@SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
public class MarketRunner
        implements Serializable { // amounts are in underlying currency (GBP), but the only way to get amounts from PriceSize object is with getSizeEUR method
    @Serial
    private static final long serialVersionUID = -7071355306184374342L;
    private static final long recentModificationPeriod = 1_000L; // milliseconds for a modification to be recent
    private static final Logger logger = LoggerFactory.getLogger(MarketRunner.class);
    //    private final Market market;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final String marketId;
    private final RunnerId runnerId;
    private final LinkedList<RunnerOrderModification> recentModifications = new LinkedList<>(); // will contain sizes in GBP, and will only be extracted after conversion to EUR

    // Level / Depth Based Ladders
//    private MarketRunnerPrices marketRunnerPrices = new MarketRunnerPrices();
    private final PriceSizeLadder atlPrices = PriceSizeLadder.newLay(); // available to lay
    private final PriceSizeLadder atbPrices = PriceSizeLadder.newBack(); // available to back
    private final PriceSizeLadder trdPrices = PriceSizeLadder.newLay(); // traded
    private final PriceSizeLadder spbPrices = PriceSizeLadder.newBack();
    private final PriceSizeLadder splPrices = PriceSizeLadder.newLay();

    // Full depth Ladders
    private final LevelPriceSizeLadder batbPrices = new LevelPriceSizeLadder();
    private final LevelPriceSizeLadder batlPrices = new LevelPriceSizeLadder();
    private final LevelPriceSizeLadder bdatbPrices = new LevelPriceSizeLadder();
    private final LevelPriceSizeLadder bdatlPrices = new LevelPriceSizeLadder();

    // special prices
    private double spn; // starting price near projected
    private double spf; // starting price far
    private double ltp; // last traded price
    private double tv; // total value traded
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private RunnerDefinition runnerDefinition;

    MarketRunner(final String marketId, final RunnerId runnerId) {
        this.marketId = marketId;
        this.runnerId = runnerId;
    }

    private synchronized void removeExpiredModifications() {
        final long currentTime = System.currentTimeMillis();
        if (this.recentModifications.isEmpty()) { // nothing to remove
        } else {
            boolean removingFirstElement;
            do {
                final RunnerOrderModification modification = this.recentModifications.peek();
                if (modification == null) {
                    logger.error("null modification in removeExpiredModifications for: {}", Generic.objectToString(this.recentModifications));
                    removingFirstElement = true;
                } else {
                    final long stamp = modification.getTimeStamp();
                    removingFirstElement = stamp + recentModificationPeriod < currentTime;
                }
                if (removingFirstElement) {
                    this.recentModifications.remove();
                } else { // not expired, won't remove
                }
            } while (!this.recentModifications.isEmpty() && removingFirstElement);
        }
    }

//    private synchronized void addRecentModification(@NotNull final RunnerOrderModification modification) {
//        this.recentModifications.add(modification);
//        removeExpiredModifications();
//    }

    private synchronized void addRecentModifications(final Collection<RunnerOrderModification> modifications) {
        if (modifications == null) { // nothing to be done
        } else {
            this.recentModifications.addAll(modifications);
        }
        removeExpiredModifications();
    }

    synchronized void onPriceChange(final boolean isImage, @NotNull final RunnerChange runnerChange) {
        addRecentModifications(this.atlPrices.onPriceChangeGetModifications(isImage, runnerChange.getAtl()));
        addRecentModifications(this.atbPrices.onPriceChangeGetModifications(isImage, runnerChange.getAtb()));
        this.trdPrices.onPriceChange(isImage, runnerChange.getTrd());
        this.spbPrices.onPriceChange(isImage, runnerChange.getSpb());
        this.splPrices.onPriceChange(isImage, runnerChange.getSpl());

        this.batbPrices.onPriceChange(isImage, runnerChange.getBatb());
        this.batlPrices.onPriceChange(isImage, runnerChange.getBatl());
        this.bdatbPrices.onPriceChange(isImage, runnerChange.getBdatb());
        this.bdatlPrices.onPriceChange(isImage, runnerChange.getBdatl());

        this.setSpn(Formulas.selectPrice(isImage, this.getSpn(), runnerChange.getSpn()));
        this.setSpf(Formulas.selectPrice(isImage, this.getSpf(), runnerChange.getSpf()));
        this.setLtp(Formulas.selectPrice(isImage, this.getLtp(), runnerChange.getLtp()));
        this.setTv(Formulas.selectPrice(isImage, this.getTv(), runnerChange.getTv()));
    }

    synchronized void onRunnerDefinitionChange(final RunnerDefinition newRunnerDefinition) {
        this.runnerDefinition = newRunnerDefinition;
    }

    @Nullable
    public synchronized Integer getSortPriority() {
        return this.runnerDefinition == null ? null : this.runnerDefinition.getSortPriority();
    }

//    public synchronized double getBestAvailableLayPrice(final Map<String, ? extends Order> unmatchedOrders, final double calculatedLimit, @NotNull final AtomicDouble currencyRate) {
//        final PriceSizeLadder modifiedPrices = this.atlPrices.copy();
//        if (unmatchedOrders == null) { // normal case, and nothing to be done
//        } else {
//            for (final Order order : unmatchedOrders.values()) {
//                final Side side = order.getSide();
//                if (side == null) {
//                    logger.error("null side in getBestAvailableLayPrice for order: {}", Generic.objectToString(order));
//                } else if (side == Side.B) {
//                    final Double price = order.getP(), sizeRemaining = order.getSr();
//                    final double sizeRemainingPrimitive = sizeRemaining == null ? 0d : sizeRemaining;
//                    modifiedPrices.removeAmountEUR(price, sizeRemainingPrimitive, currencyRate);
//                } else { // uninteresting side, nothing to be done
//                }
//            }
//        }
//        return modifiedPrices.getBestPrice(calculatedLimit, currencyRate);
//    }
//
//    public synchronized double getBestAvailableBackPrice(final Map<String, ? extends Order> unmatchedOrders, final double calculatedLimit, @NotNull final AtomicDouble currencyRate) {
//        final PriceSizeLadder modifiedPrices = this.atbPrices.copy();
//        if (unmatchedOrders == null) { // normal case, and nothing to be done
//        } else {
//            for (final Order order : unmatchedOrders.values()) {
//                final Side side = order.getSide();
//                if (side == null) {
//                    logger.error("null side in getBestAvailableBackPrice for order: {}", Generic.objectToString(order));
//                } else if (side == Side.L) {
//                    final Double price = order.getP(), sizeRemaining = order.getSr();
//                    final double sizeRemainingPrimitive = sizeRemaining == null ? 0d : sizeRemaining;
//                    modifiedPrices.removeAmountEUR(price, sizeRemainingPrimitive, currencyRate);
//                } else { // uninteresting side, nothing to be done
//                }
//            }
//        }
//        return modifiedPrices.getBestPrice(calculatedLimit, currencyRate);
//    }

    public synchronized boolean isActive() {
        return this.runnerDefinition != null && this.runnerDefinition.isActive();
    }

    public synchronized RunnerId getRunnerId() {
        return this.runnerId;
    }

    @Contract(pure = true)
    private synchronized double getSpn() {
        return this.spn;
    }

    public synchronized double getSpnEUR(@NotNull final AtomicDouble currencyRate) {
        return getSpn() * currencyRate.get();
    }

    private synchronized void setSpn(final double spn) {
        this.spn = spn;
    }

    @Contract(pure = true)
    private synchronized double getSpf() {
        return this.spf;
    }

    public synchronized double getSpfEUR(@NotNull final AtomicDouble currencyRate) {
        return getSpf() * currencyRate.get();
    }

    private synchronized void setSpf(final double spf) {
        this.spf = spf;
    }

    public synchronized double getLtp() {
        return this.ltp;
    }

    private synchronized void setLtp(final double ltp) {
        this.ltp = ltp;
    }

    @Contract(pure = true)
    private synchronized double getTv() {
        return this.tv;
    }

    public synchronized double getTvEUR(@NotNull final AtomicDouble currencyRate) {
        return getTv() * currencyRate.get();
    }

    private synchronized void setTv(final double tv) {
        this.tv = tv;
    }

    @NotNull
    public synchronized OrdersList getAvailableToLay(@NotNull final AtomicDouble currencyRate) {
        removeExpiredModifications();
        final double currencyRatePrimitive = currencyRate.get();
        final TreeMap<Double, Double> orders = this.atlPrices.getSimpleTreeMap(currencyRatePrimitive);
        final LinkedList<RunnerOrderModification> layRecentModifications = new LinkedList<>();
        for (final RunnerOrderModification runnerOrderModification : this.recentModifications) {
            if (runnerOrderModification == null) {
                SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null runnerOrderModification in getAvailableToLay");
            } else {
                final Side side = runnerOrderModification.getSide();
                if (side == Side.L) {
                    final RunnerOrderModification runnerOrderModificationEUR = new RunnerOrderModification(side, runnerOrderModification.getPrice(), runnerOrderModification.getSize() * currencyRatePrimitive);
                    layRecentModifications.add(runnerOrderModificationEUR);
                } else { // not the side I want
                }
            }
        }
        return new OrdersList(orders, layRecentModifications);
    }

    @NotNull
    public synchronized OrdersList getAvailableToBack(@NotNull final AtomicDouble currencyRate) {
        removeExpiredModifications();
        final double currencyRatePrimitive = currencyRate.get();
        final TreeMap<Double, Double> orders = this.atbPrices.getSimpleTreeMap(currencyRatePrimitive);
        final LinkedList<RunnerOrderModification> backRecentModifications = new LinkedList<>();
        for (final RunnerOrderModification runnerOrderModification : this.recentModifications) {
            if (runnerOrderModification == null) {
                SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null runnerOrderModification in getAvailableToBack");
            } else {
                final Side side = runnerOrderModification.getSide();
                if (side == Side.B) {
                    final RunnerOrderModification runnerOrderModificationEUR = new RunnerOrderModification(side, runnerOrderModification.getPrice(), runnerOrderModification.getSize() * currencyRatePrimitive);
                    backRecentModifications.add(runnerOrderModificationEUR);
                } else { // not the side I want
                }
            }
        }
        return new OrdersList(orders, backRecentModifications);
    }
}
