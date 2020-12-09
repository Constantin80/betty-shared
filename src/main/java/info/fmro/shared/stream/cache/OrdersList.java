package info.fmro.shared.stream.cache;

import com.google.common.math.DoubleMath;
import info.fmro.shared.objects.SharedStatics;
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

    public OrdersList(@NotNull final TreeMap<Double, Double> orders, @NotNull final LinkedList<RunnerOrderModification> recentModifications) {
        this.orders = new TreeMap<>(orders);
        this.recentModifications = new LinkedList<>(recentModifications);
    }

    @NotNull
    public TreeMap<Double, Double> getOrders() {
        return new TreeMap<>(this.orders);
    }

    public boolean isEmpty() {
        return this.orders.isEmpty() && this.recentModifications.isEmpty();
    }

    @NotNull
    public LinkedList<RunnerOrderModification> getRecentModifications() {
        return new LinkedList<>(this.recentModifications);
    }

    @NotNull
    public TreeMap<Double, Double> getOrdersThatAppearInRecords(@NotNull final OrdersList recordedRecentModifications) {
        return getOrdersThatAppearInRecords(recordedRecentModifications.getRecentModifications());
    }

    @NotNull
    public TreeMap<Double, Double> getOrdersThatAppearInRecords(@NotNull final Iterable<RunnerOrderModification> recordedRecentModifications) {
        final TreeMap<Double, Double> returnMap = new TreeMap<>(this.orders);
        for (final RunnerOrderModification runnerOrderModification : this.recentModifications) {
            if (recentModificationIsContainedInList(runnerOrderModification, recordedRecentModifications)) { // this modification is fine
            } else {
                removeNotContainedModification(runnerOrderModification, returnMap);
            }
        }
        return returnMap;
    }

    private static void removeNotContainedModification(@NotNull final RunnerOrderModification runnerOrderModification, @NotNull final TreeMap<Double, Double> orders) {
        final double size = runnerOrderModification.getSize();
        final double price = runnerOrderModification.getPrice();
        final Double existingSize = orders.get(price);
        final double existingSizePrimitive = existingSize == null ? 0d : existingSize;
        double newValue = existingSizePrimitive - size;
        if (newValue < 0d) {
            if (DoubleMath.fuzzyCompare(newValue, 0d, 0.0001d) < 0) {
                logger.error("negative newValue {} in removeNotContainedModification for: {} {} {}", newValue, existingSizePrimitive, size, price);
            } else { // tiny difference due to rounding, might be fine
            }
            newValue = 0d;
        } else { // value is fine
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
