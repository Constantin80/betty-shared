package info.fmro.shared.objects;

import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class AmountsNavigableMap
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AmountsNavigableMap.class);
    @Serial
    private static final long serialVersionUID = -675671504823603167L;
    public static final double NOT_PRESENT = -1d;
    @NotNull
    private final NavigableMap<Double, Double> availableAmounts;
    private final double worstOddsPresent; // -1 if <10 elements, else last element
    @NotNull
    private final Side side;

    public AmountsNavigableMap(@NotNull final NavigableMap<Double, Double> availableAmounts, @NotNull final Side side) {
        this.availableAmounts = new TreeMap<>(availableAmounts);
        this.side = side;
        final int mapSize = this.availableAmounts.size();

        final Double lastKey;
        if (mapSize > 10) {
            logger.error("amountsMapSize too big {} for: {} {}", mapSize, this.side, Generic.objectToString(this.availableAmounts));
            lastKey = this.availableAmounts.lastKey();
        } else if (mapSize == 10) {
            lastKey = this.availableAmounts.lastKey();
        } else { // mapSize < 10
            lastKey = NOT_PRESENT;
        }
        if (lastKey == null) {
            logger.error("null lastKey for: {} {}", this.side, Generic.objectToString(this.availableAmounts));
            this.worstOddsPresent = NOT_PRESENT;
        } else {
            this.worstOddsPresent = lastKey;
        }
    }

    @NotNull
    public synchronized Side getSide() {
        return this.side;
    }

    public synchronized double getWorstOddsPresent() {
        return this.worstOddsPresent;
    }

    public synchronized boolean worstOddsArePresent() {
        //noinspection FloatingPointEquality
        return this.worstOddsPresent != NOT_PRESENT;
    }

    public synchronized double keepOddsWithinLimits(final double oddsToValidate) {
        final double validatedOdds;
        if (worstOddsArePresent()) {
            validatedOdds = Formulas.bestOdds(oddsToValidate, this.worstOddsPresent, this.side);
        } else {
            validatedOdds = oddsToValidate;
        }

        return validatedOdds;
    }

    public synchronized double keepOddsWithinOneStepOutsideLimits(final double initialOdds) {
        final double validatedOdds;
        if (worstOddsArePresent()) {
            if (this.side == Side.B) {
                validatedOdds = Formulas.bestOdds(initialOdds, Formulas.getNextOddsDown(this.worstOddsPresent, this.side), this.side);
            } else {
                validatedOdds = Formulas.bestOdds(initialOdds, Formulas.getNextOddsUp(this.worstOddsPresent, this.side), this.side);
            }
        } else {
            validatedOdds = initialOdds;
        }
        return validatedOdds;
    }

    public synchronized double keepOddsWithinOneStepInsideLimits(final double initialOdds) {
        final double validatedOdds;
        if (worstOddsArePresent()) {
            if (this.side == Side.B) {
                validatedOdds = Formulas.bestOdds(initialOdds, Formulas.getNextOddsUp(this.worstOddsPresent, this.side), this.side);
            } else {
                validatedOdds = Formulas.bestOdds(initialOdds, Formulas.getNextOddsDown(this.worstOddsPresent, this.side), this.side);
            }
        } else {
            validatedOdds = initialOdds;
        }
        return validatedOdds;
    }

    public synchronized SortedMap<Double, Double> headMap(final Double toKey) {
        return new TreeMap<>(this.availableAmounts.headMap(toKey));
    }

    public synchronized SortedMap<Double, Double> headMap(final Double toKey, final boolean inclusive) {
        return new TreeMap<>(this.availableAmounts.headMap(toKey, inclusive));
    }

    public synchronized NavigableSet<Double> descendingKeySet() {
        return new TreeSet<>(this.availableAmounts.descendingKeySet());
    }

//    public synchronized Set<Map.Entry<Double, Double>> entrySet() { // creates a new set for entries, which is different from standard behavior
//        return new TreeSet<>(this.availableAmounts.entrySet());
//    }

    public synchronized Double get(final Object key) {
        return this.availableAmounts.get(key);
    }

    public synchronized void removeOwnAmountsFromAvailableTreeMap(@NotNull final Map<Double, Double> amountsFromMyUnmatchedOrders, final String reason) {
        //noinspection ForLoopWithMissingComponent
        for (final Iterator<Map.Entry<Double, Double>> iterator = this.availableAmounts.entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<Double, Double> entry = iterator.next();
            final Double price = entry.getKey();
            if (price == null) {
                logger.error("null price in removeOwnAmountsFromAvailableTreeMap for: {} {} {}", Generic.objectToString(this.availableAmounts), Generic.objectToString(amountsFromMyUnmatchedOrders), reason);
            } else {
                final Double availableAmount = entry.getValue();
                final double availableAmountPrimitive = availableAmount == null ? 0d : availableAmount;
                final Double myAmount = amountsFromMyUnmatchedOrders.get(price);
                final double myAmountPrimitive = myAmount == null ? 0d : myAmount;
                final double amountFromOthers = availableAmountPrimitive - myAmountPrimitive;
                if (amountFromOthers < 0.01d) {
//                    availableAmounts.remove(price);
                    iterator.remove();
                    if (amountFromOthers <= -0.01d) {
                        if (availableAmountPrimitive / myAmountPrimitive > .995d) { // tiny difference caused most likely by currency conversion and roundings
                        } else {
                            if (reason.contains("Mandatory")) { // normal to get negative amounts, no need to print anything
                            } else {
                                logger.error("negative amount from others {} in removeOwnAmountsFromAvailableTreeMap for {}: {} {} {} {} {}", amountFromOthers, reason, price, myAmount, availableAmount, Generic.objectToString(amountsFromMyUnmatchedOrders),
                                             Generic.objectToString(this.availableAmounts));
                            }
                        }
                    } else { // no error, nothing to print
                    }
                } else {
//                    availableAmounts.replace(price, amountFromOthers);
                    entry.setValue(amountFromOthers);
                }
            }
        }
    }

    public synchronized double getAmountsSum() {
        double amountsSum = 0d;
        for (@NotNull final Map.Entry<Double, Double> entry : this.availableAmounts.entrySet()) {
//            final Double price = entry.getKey();
//            final double pricePrimitive = price == null ? 1d : price;
            final Double amount = entry.getValue();
            final double amountPrimitive = amount == null ? 0d : amount;
//            amountsSum += amountPrimitive * (pricePrimitive - 1d);
            amountsSum += amountPrimitive;
        }
        return amountsSum;
    }
}
