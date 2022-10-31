package info.fmro.shared.stream.cache;

import com.google.common.math.DoubleMath;
import info.fmro.shared.objects.AmountsNavigableMap;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.TreeMap;

public class OrdersList
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -9010908019940587398L;
    private static final Logger logger = LoggerFactory.getLogger(OrdersList.class);
    @NotNull
    private final TreeMap<Double, Double> orders;
    @NotNull
    private final LinkedList<RunnerOrderModification> recentModifications;
    private final Side side;
    private final double worstOdds;

    public OrdersList(@NotNull final TreeMap<Double, Double> orders, @NotNull final LinkedList<RunnerOrderModification> recentModifications, @NotNull final Side side, final double worstOdds) {
        this.orders = new TreeMap<>(orders);
        this.recentModifications = new LinkedList<>(recentModifications);
        this.side = side;
        this.worstOdds = worstOdds;
    }

    private boolean worstOddsExists() {
        //noinspection FloatingPointEquality
        return this.worstOdds != AmountsNavigableMap.NOT_PRESENT;
    }

    private boolean priceIsWithinLadderLimits(final double price) {
        return !worstOddsExists() || Formulas.oddsCompare(this.worstOdds, price, this.side) <= 0;
    }

    @NotNull
    public synchronized TreeMap<Double, Double> getOrders() {
        return new TreeMap<>(this.orders);
    }

    private boolean isEmpty() {
        return this.orders.isEmpty() && this.recentModifications.isEmpty();
    }

    @NotNull
    private synchronized LinkedList<RunnerOrderModification> getRecentModifications() {
        return new LinkedList<>(this.recentModifications);
    }

//    @NotNull
//    public synchronized TreeMap<Double, Double> getOrdersThatAppearInRecords(@NotNull final OrdersList recordedRecentModifications) {
//        return getOrdersThatAppearInRecords(recordedRecentModifications.getRecentModifications());
//    }

    @NotNull
    public TreeMap<Double, Double> getOrdersThatAppearInRecords(@NotNull final OrdersList recordedRecentModifications) {
        @NotNull final Iterable<RunnerOrderModification> recordedRecentModificationsList = recordedRecentModifications.getRecentModifications();
        final TreeMap<Double, Double> returnMap = new TreeMap<>(this.orders);
        for (final RunnerOrderModification runnerOrderModification : this.recentModifications) {
            if (!recordedRecentModifications.priceIsWithinLadderLimits(runnerOrderModification.getPrice()) || recentModificationIsContainedInList(runnerOrderModification, recordedRecentModificationsList)) { // this modification is fine
            } else {
                removeNotContainedModification(runnerOrderModification, returnMap);
            }
        }

        if (this.orders.isEmpty() || this.recentModifications.isEmpty()) { // won't print any message for empty maps
        } else {
            logger.debug("getOrdersThatAppearInRecords before: {} recentModifications: {} recordedRecentModifications: {} after: {}", Generic.objectToString(this.orders), Generic.objectToString(this.recentModifications, "timeStamp"),
                         Generic.objectToString(recordedRecentModificationsList, "timeStamp"), Generic.objectToString(returnMap));
        }
        return returnMap;
    }

    private static void removeNotContainedModification(@NotNull final RunnerOrderModification runnerOrderModification, @NotNull final TreeMap<Double, Double> orders) {
        final double price = runnerOrderModification.getPrice();
        double newValue;
        if (runnerOrderModification.modificationSizeUnknown()) {
            newValue = 0d;
        } else {
            final double size = runnerOrderModification.getSize(); // todo need to recheck all this
            final Double existingSize = orders.get(price); // always >= 0, as these are my existing unmatched orders (while modifications are either cancel or place)
            final double existingSizePrimitive = existingSize == null ? 0d : existingSize;
            newValue = existingSizePrimitive - size;
            if (newValue < 0d) {
                if (DoubleMath.fuzzyCompare(newValue, 0d, 0.0001d) < 0) { // I don't know if this would be an error or normal; might be normal
                    logger.warn("negative newValue {} in removeNotContainedModification for: {} {} {}", newValue, existingSizePrimitive, size, price);
                } else { // tiny difference due to rounding, might be fine
                }
                newValue = 0d;
            } else { // value is fine
            }
        }
        orders.put(price, newValue);
    }

    private static boolean recentModificationIsContainedInList(final RunnerOrderModification runnerOrderModification, @NotNull final Iterable<RunnerOrderModification> recordedRecentModifications) {
        boolean isContained = false;
        if (runnerOrderModification == null) {
            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null runnerOrderModification in recentModificationIsContainedInList");
            isContained = true; // a modification that contains nothing is always contained
        } else {
            for (final RunnerOrderModification recordedRunnerOrderModification : recordedRecentModifications) {
                if (recordedRunnerOrderModification == null) {
                    SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null recordedRunnerOrderModification in recentModificationIsContainedInList");
                } else {
                    if (recordedRunnerOrderModification.contains(runnerOrderModification)) {
                        isContained = true;
                        break;
                    } else { // not contained, will keep checking
                    }
                }
            } // end for
        }
        return isContained;
    }
}
