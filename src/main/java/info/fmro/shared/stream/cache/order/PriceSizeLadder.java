package info.fmro.shared.stream.cache.order;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public final class PriceSizeLadder
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(PriceSizeLadder.class);
    @Serial
    private static final long serialVersionUID = 7150436333395479059L;
    private final TreeMap<Double, PriceSize> priceToSize;

    private PriceSizeLadder(@NotNull final Comparator<? super Double> comparator) {
        if (!comparator.equals(Comparator.reverseOrder()) && !comparator.equals(Comparator.naturalOrder())) {
            logger.error("not supported comparator type in PriceSizeLadder; this check is included to make sure the used comparator is serializable: {}", comparator);
        }
        this.priceToSize = new TreeMap<>(comparator);
    }

    @NotNull
    @Contract(" -> new")
    static PriceSizeLadder newBack() {
        return new PriceSizeLadder(Comparator.reverseOrder());
    }

    @NotNull
    @Contract(" -> new")
    static PriceSizeLadder newLay() {
        return new PriceSizeLadder(Comparator.naturalOrder());
    }

//    @NotNull
//    public synchronized PriceSizeLadder copy() {
//        final PriceSizeLadder result;
//        Comparator<? super Double> comparator = this.priceToSize.comparator();
//        if (comparator == null) {
//            logger.error("null comparator in PriceSizeLadder.copy for: {}", Generic.objectToString(this));
//            comparator = Comparator.naturalOrder();
//        } else { // normal case, everything is fine, nothing to be done
//        }
//        result = new PriceSizeLadder(comparator);
//        result.updateTreeMap(this.priceToSize);
//
//        return result;
//    }
//
//    private synchronized void updateTreeMap(final Map<Double, ? extends PriceSize> map) {
//        this.priceToSize.putAll(map);
//    }

    @NotNull
    public synchronized TreeMap<Double, Double> getSimpleTreeMap() {
        final TreeMap<Double, Double> result = new TreeMap<>(this.priceToSize.comparator());

        for (final PriceSize priceSize : this.priceToSize.values()) {
            if (priceSize == null) {
                logger.error("null priceSize in getSimpleTreeMap for: {}", Generic.objectToString(this));
            } else {
                result.put(priceSize.getPrice(), priceSize.getSize());
            }
        }

        return result;
    }

    synchronized double getMatchedSizeAtBetterOrEqual(final double price, final Iterable<List<Double>> newMatchedList, final boolean isImage) {
        // best price matcher uses amounts, not exposure; for example an EUR 100 lay bet at 1.05 can be matched 40 EUR at 1.04 and 60 EUR at 1.05, with no consideration about exposure
        double matchedSize = 0d;
        if (this.priceToSize != null) {
//            final boolean isBack = this.priceToSize.comparator().equals(Comparator.reverseOrder());
            final SortedMap<Double, PriceSize> betterOrEqualPriceSize = new TreeMap<>(this.priceToSize.headMap(price, true));
            if (isImage) {
                betterOrEqualPriceSize.clear();
            }
            if (newMatchedList != null) {
                for (final List<Double> priceAndSize : newMatchedList) {
                    final PriceSize priceSize = new PriceSize(priceAndSize);
                    if (priceSize.getSize() == 0d) {
                        betterOrEqualPriceSize.remove(priceSize.getPrice());
                    } else {
                        betterOrEqualPriceSize.put(priceSize.getPrice(), priceSize);
                    }
                }
            } else { // normal, no need to update betterOrEqualPriceSize
            }

            for (final PriceSize priceSize : betterOrEqualPriceSize.values()) {
                if (priceSize != null) {
                    matchedSize += priceSize.getSize();
                } else {
                    logger.error("priceSize null in getMatchedExposureAtBetterOrEqual for: {} {}", price, Generic.objectToString(this));
                }
            }
        } else {
            logger.error("null priceToSize in getMatchedExposureAtBetterOrEqual for: {} {}", price, Generic.objectToString(this));
        }
        return matchedSize;
    }

    synchronized double getMatchedSizeAtBetterOrEqual(final double price) { // best price matcher uses amounts, not exposure; for example an EUR 100 lay bet at 1.05 can be matched 40 EUR at 1.04 and 60 EUR at 1.05, with no consideration about exposure
        double matchedSize = 0d;
        if (this.priceToSize != null) {
//            final boolean isBack = this.priceToSize.comparator().equals(Comparator.reverseOrder());
            final SortedMap<Double, PriceSize> betterOrEqualPriceSize = this.priceToSize.headMap(price, true);
            for (final PriceSize priceSize : betterOrEqualPriceSize.values()) {
                if (priceSize != null) {
                    matchedSize += priceSize.getSize();
                } else {
                    logger.error("priceSize null in getMatchedExposureAtBetterOrEqual for: {} {}", price, Generic.objectToString(this));
                }
            }
        } else {
            logger.error("null priceToSize in getMatchedExposureAtBetterOrEqual for: {} {}", price, Generic.objectToString(this));
        }
        return matchedSize;
    }

    synchronized double getMatchedSize(final double price) {
        final double matchedSize;
        if (this.priceToSize != null) {
            if (this.priceToSize.containsKey(price)) {
                final PriceSize priceSize = this.priceToSize.get(price);
                if (priceSize != null) {
                    matchedSize = priceSize.getSize();
                } else {
                    logger.error("priceSize null in getMatchedSize for: {} {}", price, Generic.objectToString(this));
                    matchedSize = 0d;
                }
            } else { // normal case, proper price not found
                matchedSize = 0d;
            }
        } else {
            logger.error("null priceToSize in getMatchedSize for: {} {}", price, Generic.objectToString(this));
            matchedSize = 0d;
        }
        return matchedSize;
    }

    synchronized void onPriceChange(final boolean isImage, final Iterable<? extends List<Double>> prices) {
        if (isImage) {
            this.priceToSize.clear();
        }
        if (prices != null) {
            for (final List<Double> priceAndSize : prices) {
                final PriceSize priceSize = new PriceSize(priceAndSize);
                if (priceSize.getSize() == 0d) {
                    this.priceToSize.remove(priceSize.getPrice());
                } else {
                    this.priceToSize.put(priceSize.getPrice(), priceSize);
                }
            }
        }
    }

    public synchronized double getBestPrice(final double calculatedLimit) {
        double result = 0d;
        if (this.priceToSize == null) {
            logger.error("null priceToSize in getBestPrice for: {}", Generic.objectToString(this));
            result = 0d;
        } else if (this.priceToSize.isEmpty()) {
            result = 0d;
        } else {
            final double minimumAmountConsideredSignificant = Math.min(calculatedLimit * .05d, 10d); // these defaults are rather basic
            for (final PriceSize priceSize : this.priceToSize.values()) {
                if (priceSize == null) {
                    logger.error("null priceSize in getBestPrice {} for: {}", calculatedLimit, Generic.objectToString(this));
                } else {
                    final double size = priceSize.getSize();
                    if (size >= minimumAmountConsideredSignificant) {
                        result = priceSize.getPrice();
                        break;
                    }
                }
            }
        }
        return result;
    }

    synchronized void updateBackProfitExposure(@NotNull final Exposure exposure) {
        for (final PriceSize priceSize : this.priceToSize.values()) {
            priceSize.updateBackProfitExposure(exposure);
        }
    }

    synchronized void updateLayProfitExposure(@NotNull final Exposure exposure) {
        for (final PriceSize priceSize : this.priceToSize.values()) {
            priceSize.updateLayProfitExposure(exposure);
        }
    }

//    @NotNull
//    @Contract(" -> new")
//    public synchronized TwoDoubles getBackProfitExposurePair() { // this works for back; for lay profit and exposure are reversed
//        double profit = 0d, exposure = 0d;
//        for (final PriceSize priceSize : this.priceToSize.values()) {
//            final TwoDoubles twoDoubles = priceSize.getBackProfitExposurePair();
//            profit += twoDoubles.getFirstDouble();
//            exposure += twoDoubles.getSecondDouble();
//        }
//        return new TwoDoubles(profit, exposure);
//    }

    public synchronized void removeAmountEUR(final Double price, final double sizeToRemove, @NotNull final AtomicDouble currencyRate) {
        final double sizeToRemoveGBP = sizeToRemove / currencyRate.get();
        removeAmountGBP(price, sizeToRemoveGBP);
    }

    private synchronized void removeAmountGBP(final Double price, final double sizeToRemove) {
        if (this.priceToSize.containsKey(price)) {
            final PriceSize priceSize = this.priceToSize.get(price);
            if (priceSize == null) {
                logger.error("null priceSize for price {} sizeToRemove {} in PriceSizeLadder.removeAmount for: {}", price, sizeToRemove, Generic.objectToString(this));
            } else {
                priceSize.removeAmountGBP(sizeToRemove);
            }
        } else {
            logger.info("price {} sizeToRemove {} not found in PriceSizeLadder.removeAmount for: {}", price, sizeToRemove, Generic.objectToString(this));
        }
    }

    public synchronized boolean isEmpty() {
        return this.priceToSize.isEmpty();
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public synchronized String toString() {
        return "{" + this.priceToSize.values() + '}';
    }
}
