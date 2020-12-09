package info.fmro.shared.stream.cache.market;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.RunnerOrderModification;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class PriceSizeLadder
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(PriceSizeLadder.class);
    @Serial
    private static final long serialVersionUID = -5267061740378308330L;
    @NotNull
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

    @Nullable
    private Side getSide() {
        @Nullable final Side side;
        final Comparator<? super Double> comparator = this.priceToSize.comparator();
        if (comparator.equals(Comparator.reverseOrder())) {
            side = Side.B;
        } else if (comparator.equals(Comparator.naturalOrder())) {
            side = Side.L;
        } else {
            logger.error("unknown comparator in getSide: {}", Generic.objectToString(comparator));
            side = null;
        }
        return side;
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
    synchronized TreeMap<Double, Double> getSimpleTreeMap(final double currencyRate) {
        final TreeMap<Double, Double> result = new TreeMap<>(this.priceToSize.comparator());
        for (final PriceSize priceSize : this.priceToSize.values()) {
            if (priceSize == null) {
                logger.error("null priceSize in getSimpleTreeMap for: {}", Generic.objectToString(this));
            } else {
                final double price = priceSize.getPrice(), size = priceSize.getSizeEUR(currencyRate);
                final Double existingSize = result.get(price);
                final double existingSizePrimitive = existingSize == null ? 0d : existingSize;
                result.put(price, existingSizePrimitive + size);
            }
        }

        return result;
    }

    public synchronized double getMatchedSize(final double price, @NotNull final AtomicDouble currencyRate) {
        final double matchedSize;

//        if (this.priceToSize != null) {
        if (this.priceToSize.containsKey(price)) {
            final PriceSize priceSize = this.priceToSize.get(price);
            if (priceSize != null) {
                matchedSize = priceSize.getSizeEUR(currencyRate.get());
            } else {
                logger.error("priceSize null in getMatchedSize for: {} {}", price, Generic.objectToString(this));
                matchedSize = 0d;
            }
        } else { // normal case, proper price not found
            matchedSize = 0d;
        }
//        } else {
//            logger.error("null priceToSize in getMatchedSize for: {} {}", price, Generic.objectToString(this));
//            matchedSize = 0d;
//        }

        return matchedSize;
    }

    @Nullable
    synchronized List<RunnerOrderModification> onPriceChangeGetModifications(final boolean isImage, final Iterable<? extends List<Double>> prices) {
        final Map<Double, PriceSize> initialMap = new HashMap<>(this.priceToSize);
        onPriceChange(isImage, prices);
//        final Map<Double, PriceSize> finalMap = new HashMap<>(this.priceToSize);

        final Collection<Double> pricesSet = new HashSet<>(initialMap.keySet());
        pricesSet.addAll(this.priceToSize.keySet());
        final Side side = getSide();
        @Nullable final List<RunnerOrderModification> returnList;
        if (pricesSet.isEmpty() || side == null) {
            returnList = null;
        } else {
            returnList = new ArrayList<>(pricesSet.size());
            for (final Double price : pricesSet) {
                if (price == null) {
                    SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null price in onPriceChangeGetModifications for: {} {}", Generic.objectToString(initialMap), Generic.objectToString(this.priceToSize));
                } else {
                    final PriceSize initialPriceSize = initialMap.get(price);
                    final PriceSize finalPriceSize = this.priceToSize.get(price);
                    final double initialSize = initialPriceSize == null ? 0d : initialPriceSize.getSizeGBP();
                    final double finalSize = finalPriceSize == null ? 0d : finalPriceSize.getSizeGBP();
                    final double modification = finalSize - initialSize;
                    if (DoubleMath.fuzzyEquals(modification, 0d, 0.0001d)) { // no modification
                    } else {
                        final RunnerOrderModification runnerOrderModification = new RunnerOrderModification(side, price, modification);
                        returnList.add(runnerOrderModification);
                    }
                }
            } // end for
        }

        return returnList;
    }

    synchronized void onPriceChange(final boolean isImage, final Iterable<? extends List<Double>> prices) {
        if (isImage) {
            this.priceToSize.clear();
        }
        if (prices != null) {
            for (final List<Double> price : prices) {
                final PriceSize priceSize = new PriceSize(price);
                if (priceSize.getSizeGBP() == 0d) {
                    this.priceToSize.remove(priceSize.getPrice());
                } else {
                    this.priceToSize.put(priceSize.getPrice(), priceSize);
                }
            }
        } else { // nothing to be done
        }
    }

    public synchronized double getBestPrice(final double calculatedLimit, @NotNull final AtomicDouble currencyRate) {
        double result = 0d;
        final double currencyRatePrimitive = currencyRate.get();
//        if (this.priceToSize == null) {
//            logger.error("null priceToSize in getBestPrice for: {}", Generic.objectToString(this));
//            result = 0d;
//        } else
        if (this.priceToSize.isEmpty()) {
            result = 0d;
        } else {
            final double minimumAmountConsideredSignificant = Math.min(calculatedLimit * .05d, 10d); // these defaults are rather basic
            for (final PriceSize priceSize : this.priceToSize.values()) {
                if (priceSize == null) {
                    logger.error("null priceSize in getBestPrice {} for: {}", calculatedLimit, Generic.objectToString(this));
                } else {
                    final double size = priceSize.getSizeEUR(currencyRatePrimitive);
                    if (size >= minimumAmountConsideredSignificant) {
                        result = priceSize.getPrice();
                        break;
                    }
                }
            }
        }
        return result;
    }

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
