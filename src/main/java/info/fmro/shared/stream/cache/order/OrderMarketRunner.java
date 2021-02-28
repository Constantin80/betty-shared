package info.fmro.shared.stream.cache.order;

import com.google.common.math.DoubleMath;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.OrdersList;
import info.fmro.shared.stream.cache.RunnerOrderModification;
import info.fmro.shared.stream.definitions.Order;
import info.fmro.shared.stream.definitions.OrderRunnerChange;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@SuppressWarnings("OverlyComplexClass")
public class OrderMarketRunner
        extends Exposure
        implements Serializable { // amounts are in account currency (EUR)
    @Serial
    private static final long serialVersionUID = 6359709424181107081L;
    private static final long recentModificationPeriod = 500L; // milliseconds for a modification to be recent
    private static final Logger logger = LoggerFactory.getLogger(OrderMarketRunner.class);
    //    private final OrderMarket orderMarket;
    private final String marketId;
    private final RunnerId runnerId;
    private final PriceSizeLadder layMatches = PriceSizeLadder.newLay();
    private final PriceSizeLadder backMatches = PriceSizeLadder.newBack();
    private final Map<String, Order> unmatchedOrders = new HashMap<>(2); // only place where orders are permanently stored; orderId, order
    private final LinkedList<RunnerOrderModification> recentModifications = new LinkedList<>(); // will contain sizes in EUR
//    private boolean obsoleteObject;

    OrderMarketRunner(final String marketId, final RunnerId runnerId) {
        super();
        if (marketId == null || runnerId == null) {
            logger.error("bad arguments when creating OrderMarketRunner: {} {}", marketId, runnerId);
        } else { // no error message, constructor continues normally
        }
        this.marketId = marketId;
        this.runnerId = runnerId;
    }

    synchronized void onOrderRunnerChange(final OrderRunnerChange orderRunnerChange) {
        SharedStatics.orderCache.checkTemporaryOrdersForStreamChange(this, orderRunnerChange); // needs to happen at the start of the method, before I modify this object
        final Map<String, Order> initialMap = new HashMap<>(this.unmatchedOrders);

        final boolean isImage = orderRunnerChange.isImage();
        if (isImage) {
            this.unmatchedOrders.clear();
        }
        final List<Order> unmatchedOrdersList = orderRunnerChange.getUo();
        if (unmatchedOrdersList != null) {
            for (final Order order : unmatchedOrdersList) {
                final Double sizeRemaining = order.getSr();
                if (sizeRemaining == null || sizeRemaining == 0d) {
                    this.unmatchedOrders.remove(order.getId());
                } else {
                    this.unmatchedOrders.put(order.getId(), order);
                }
            }
        }
        this.layMatches.onPriceChange(isImage, orderRunnerChange.getMl());
        this.backMatches.onPriceChange(isImage, orderRunnerChange.getMb());

        getExposureAndProfit();

        final Collection<String> orderIdsSet = new HashSet<>(initialMap.keySet());
        orderIdsSet.addAll(this.unmatchedOrders.keySet());
        @Nullable final List<RunnerOrderModification> newModifications;
        if (orderIdsSet.isEmpty()) {
            newModifications = null;
        } else {
            newModifications = new ArrayList<>(orderIdsSet.size());
            for (final String orderId : orderIdsSet) {
                if (orderId == null) {
                    SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null orderId in onOrderRunnerChange for: {} {}", Generic.objectToString(initialMap), Generic.objectToString(this.unmatchedOrders));
                } else {
                    final Order initialOrder = initialMap.get(orderId);
                    final Order finalOrder = this.unmatchedOrders.get(orderId);
                    if (initialOrder == null && finalOrder == null) {
                        SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null initialOrder and finalOrder in onOrderRunnerChange for: {} {} {}", orderId, Generic.objectToString(initialMap),
                                                                Generic.objectToString(this.unmatchedOrders));
                    } else {
                        final double sizeRemainingModification = getSizeRemainingModification(initialOrder, finalOrder);
                        if (DoubleMath.fuzzyEquals(sizeRemainingModification, 0d, 0.0001d)) { // no modification
                        } else {
                            final Side side = initialOrder == null ? finalOrder.getSide() : initialOrder.getSide();
                            final double matchedPrice = getMatchedPrice(initialOrder, finalOrder);
                            final double price, modification;
                            if (matchedPrice == 0d) {
                                price = getUnmatchedPrice(initialOrder, finalOrder);
                                modification = sizeRemainingModification;
                            } else {
                                price = matchedPrice;
                                modification = getSizeMatchedModification(initialOrder, finalOrder);
                            }
                            final RunnerOrderModification runnerOrderModification = new RunnerOrderModification(side, price, modification);
                            newModifications.add(runnerOrderModification);
                        }
                    }
                }
            } // end for
        }
        addRecentModifications(newModifications);
    }

    private static double getSizeRemainingModification(final Order initialOrder, final Order finalOrder) {
        final double sizeRemainingModification;
        if (initialOrder == null && finalOrder == null) {
            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null initialOrder and finalOrder in getSizeRemainingModification");
            sizeRemainingModification = 0d;
        } else {
            final Double initialSR = initialOrder == null ? Double.valueOf(0d) : initialOrder.getSr();
            final Double finalSR = finalOrder == null ? Double.valueOf(0d) : finalOrder.getSr();
            final double initialSRPrimitive = initialSR == null ? 0d : initialSR;
            final double finalSRPrimitive = finalSR == null ? 0d : finalSR;
            sizeRemainingModification = finalSRPrimitive - initialSRPrimitive;
        }
        return sizeRemainingModification;
    }

    private static double getSizeMatchedModification(final Order initialOrder, final Order finalOrder) {
        final double sizeMatchedModification;
        if (initialOrder == null && finalOrder == null) {
            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null initialOrder and finalOrder in getSizeMatchedModification");
            sizeMatchedModification = 0d;
        } else {
            final Double initialSizeMatched = initialOrder == null ? Double.valueOf(0d) : initialOrder.getSm();
            final Double finalSizeMatched = finalOrder == null ? Double.valueOf(0d) : finalOrder.getSm();
            final double initialSizeMatchedPrimitive = initialSizeMatched == null ? 0d : initialSizeMatched;
            final double finalSizeMatchedPrimitive = finalSizeMatched == null ? 0d : finalSizeMatched;
            sizeMatchedModification = finalSizeMatchedPrimitive - initialSizeMatchedPrimitive;
        }
        return -sizeMatchedModification; // it's returned as negative
    }

    public static double getMatchedPrice(final Order initialOrder, final Order finalOrder) {
        final double priceMatched;
        if (initialOrder == null && finalOrder == null) {
            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null initialOrder and finalOrder in getPriceMatched");
            priceMatched = 0d;
        } else {
            final Double initialSizeMatched = initialOrder == null ? Double.valueOf(0d) : initialOrder.getSm();
            final Double finalSizeMatched = finalOrder == null ? Double.valueOf(0d) : finalOrder.getSm();
            final double initialSizeMatchedPrimitive = initialSizeMatched == null ? 0d : initialSizeMatched;
            final double finalSizeMatchedPrimitive = finalSizeMatched == null ? 0d : finalSizeMatched;
            final double sizeMatchedModification = finalSizeMatchedPrimitive - initialSizeMatchedPrimitive;
            if (DoubleMath.fuzzyEquals(sizeMatchedModification, 0d, 0.001d)) {
                priceMatched = 0d;
            } else {
                final Double initialAveragePriceMatched = initialOrder == null ? Double.valueOf(0d) : initialOrder.getAvp();
                final Double finalAveragePriceMatched = finalOrder == null ? Double.valueOf(0d) : finalOrder.getAvp();
                final double initialAveragePriceMatchedPrimitive = initialAveragePriceMatched == null ? 0d : initialAveragePriceMatched;
                final double finalAveragePriceMatchedPrimitive = finalAveragePriceMatched == null ? 0d : finalAveragePriceMatched;
                priceMatched = (finalAveragePriceMatchedPrimitive * finalSizeMatchedPrimitive - initialAveragePriceMatchedPrimitive * initialSizeMatchedPrimitive) / sizeMatchedModification;
            }
        }
        return priceMatched;
    }

    public static double getUnmatchedPrice(final Order initialOrder, final Order finalOrder) {
        final double price;
        if (initialOrder == null && finalOrder == null) {
            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null initialOrder and finalOrder in getPriceFromOrders");
            price = 0d;
        } else {
            final Order nonNullOrder = initialOrder == null ? finalOrder : initialOrder;
            final Double priceObject = nonNullOrder.getP();
            price = priceObject == null ? 0d : priceObject;
        }
        return price;
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

    private synchronized void addRecentModifications(final Collection<RunnerOrderModification> modifications) {
        if (modifications == null) { // nothing to be done
        } else {
            this.recentModifications.addAll(modifications);
        }
        removeExpiredModifications();
    }

//    synchronized boolean isObsoleteObject() {
//        return this.obsoleteObject;
//    }
//
//    synchronized void markObsoleteObject() {
//        this.obsoleteObject = true;
//    }

    synchronized boolean isEmpty() {
        return this.unmatchedOrders.isEmpty() && this.backMatches.isEmpty() && this.layMatches.isEmpty();
    }

    synchronized Order getUnmatchedOrder(final String betId) {
        @Nullable final Order returnValue;

        if (betId != null) {
            returnValue = this.unmatchedOrders.get(betId);
        } else {
            logger.error("null betId in getUnmatchedOrder for: {} {}", this.marketId, this.runnerId);
            returnValue = null;
        }
        return returnValue;
    }

    synchronized double getMatchedSizeAtBetterOrEqual(final Side side, final double price, final Iterable<List<Double>> newMatchedList, final boolean isImage) {
        final double matchedSize;
        if (side == Side.B) {
            matchedSize = this.backMatches.getMatchedSizeAtBetterOrEqual(price, newMatchedList, isImage);
        } else if (side == Side.L) {
            matchedSize = this.layMatches.getMatchedSizeAtBetterOrEqual(price, newMatchedList, isImage);
        } else {
            logger.error("unknown Side in getMatchedSizeAtBetterOrEqual for: {} {}", side, price);
            matchedSize = 0d;
        }
        return matchedSize;
    }

    synchronized double getMatchedSizeAtBetterOrEqual(final Side side, final double price) {
        final double matchedSize;
        if (side == Side.B) {
            matchedSize = this.backMatches.getMatchedSizeAtBetterOrEqual(price);
        } else if (side == Side.L) {
            matchedSize = this.layMatches.getMatchedSizeAtBetterOrEqual(price);
        } else {
            logger.error("unknown Side in getMatchedSize for: {} {}", side, price);
            matchedSize = 0d;
        }
        return matchedSize;
    }

    @SuppressWarnings("unused")
    synchronized double getMatchedSize(final Side side, final double price) {
        final double matchedSize;
        if (side == Side.B) {
            matchedSize = this.backMatches.getMatchedSize(price);
        } else if (side == Side.L) {
            matchedSize = this.layMatches.getMatchedSize(price);
        } else {
            logger.error("unknown Side in getMatchedSize for: {} {}", side, price);
            matchedSize = 0d;
        }
        return matchedSize;
    }

    synchronized RunnerId getRunnerId() {
        return this.runnerId;
    }

    synchronized String getMarketId() {
        return this.marketId;
    }

//    synchronized PriceSizeLadder getBackMatches() {
//        return this.backMatches.copy();
//    }
//
//    synchronized PriceSizeLadder getLayMatches() {
//        return this.layMatches.copy();
//    }

    public synchronized HashMap<String, Order> getUnmatchedOrders() {
        return new HashMap<>(this.unmatchedOrders);
    }

    private synchronized void getExposureAndProfit() {
        resetMatchedAndUnmatchedExposure();
        getMatchedExposureAndProfit();
        getUnmatchedExposureAndProfit();
        timeStamp();
    }

    private synchronized void getUnmatchedExposureAndProfit() {
        for (final Order order : this.unmatchedOrders.values()) {
            order.calculateExposureAndProfit(this);
        }
    }

    private synchronized void getMatchedExposureAndProfit() {
        this.backMatches.updateBackProfitExposure(this);
        this.layMatches.updateLayProfitExposure(this);
    }

    synchronized double cancelUnmatchedAmounts(final double backExcessExposure, final double layExcessExposure, @NotNull final ManagedRunner managedRunner, @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        // best odds are removed first, as they're most unlikely to get matched
        // always the newer orders are removed first
        double removedExposure = 0d;
        removedExposure += cancelUnmatchedAmounts(Side.B, backExcessExposure, managedRunner, sendPostRequestRescriptMethod, reason);
        removedExposure += cancelUnmatchedAmounts(Side.L, layExcessExposure, managedRunner, sendPostRequestRescriptMethod, reason);
        return removedExposure;
    }

    @SuppressWarnings("WeakerAccess")
    synchronized double cancelUnmatchedAmounts(final Side side, final double excessExposure, @NotNull final ManagedRunner managedRunner, @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        double excessExposureLeft = excessExposure - (side == Side.B ? this.getBackTempCancelExposure() : this.getLayTempCancelExposure());
        if (excessExposure >= .1d) {
//            excessExposureLeft = excessExposure - (side == Side.B ? this.getBackTempCancelExposure() : this.getLayTempCancelExposure());
            final Comparator<Double> priceComparator = side == Side.B ? Comparator.reverseOrder() : Comparator.naturalOrder(); // order by reversed price or price, then reversed placed date
            if (side == Side.B || side == Side.L) {
                final ArrayList<Order> sortedOrders =
                        this.unmatchedOrders.
                                                    values().
                                                    stream().
                                                    filter(p -> p.getSide() == side).
                                                    sorted(Comparator.comparing(Order::getP, Comparator.nullsFirst(priceComparator)).
                                                            thenComparing(Order::getPd, Comparator.nullsFirst(Comparator.reverseOrder()))).
                                                    collect(Collectors.toCollection(ArrayList::new));
                for (final Order order : sortedOrders) {
                    final double removedExposure = order.removeExposure(this.marketId, this.runnerId, side, excessExposureLeft, managedRunner, sendPostRequestRescriptMethod, reason);
//                    Generic.addToAtomicDouble(removedExposureDuringThisManageIteration, removedExposure);
                    excessExposureLeft -= removedExposure;
                    if (excessExposureLeft <= 0d) {
                        break;
                    } else { // some exposure still left, will continue the loop
                    }
                }
            } else {
                logger.error("unknown side in cancelUnmatchedAmounts for: {} {} {} {}", side, excessExposure, reason, Generic.objectToString(this));
            }
        } else { // no excess exposure present
//            excessExposureLeft = excessExposure;
        }
        return excessExposure - excessExposureLeft; // total removed exposure
    }

    synchronized double cancelUnmatchedExceptGivenExposure(final Side side, final double notCanceledExposure, @NotNull final ManagedRunner managedRunner, @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        // worst odds are kept, as they're most likely to get matched
        // older orders are kept
        double exposureLeftUntilIStartToCancel = notCanceledExposure + (side == Side.B ? this.getBackTempCancelExposure() : this.getLayTempCancelExposure());
        if (side == null) {
            logger.error("null side in cancelUnmatchedExceptGivenExposure for: {} {} {}", notCanceledExposure, reason, Generic.objectToString(this));
        } else {
            final Comparator<Double> priceComparator = side == Side.B ? Comparator.naturalOrder() : Comparator.reverseOrder(); // order by price or reversed price, then placed date
            final ArrayList<Order> sortedOrders =
                    this.unmatchedOrders.
                                                values().
                                                stream().
                                                filter(p -> p.getSide() == side).
                                                sorted(Comparator.comparing(Order::getP, Comparator.nullsLast(priceComparator)).
                                                        thenComparing(Order::getPd, Comparator.nullsLast(Comparator.naturalOrder()))).
                                                collect(Collectors.toCollection(ArrayList::new)); // order by price, then placed date

            if (side == Side.B) {
                for (final Order order : sortedOrders) {
                    final Double price = order.getP();
                    if (price == null) {
                        logger.error("null price in cancelUnmatchedExceptGivenExposure for: {}", Generic.objectToString(order));
                    } else {
                        final double sizeRemaining = order.getSrConsideringTempCancel();
                        if (sizeRemaining <= 0d) { // it's dangerous to try to fix the error here; orders with zero size remaining are removed from cache when the orderChange happens, so I shouldn't get zero size here
                            final Double sr = order.getSr();
                            if (sr == null || sr == 0) {
                                logger.error("order with zero or negative amount left in cancelUnmatchedExceptGivenExposure for: {} {} {} {} {} {} {}", price, sizeRemaining, side, notCanceledExposure, reason, Generic.objectToString(order),
                                             Generic.objectToString(this));
                            } else { // no error
                            }
                        } else if (exposureLeftUntilIStartToCancel < .1d) { // no more excess left, everything from now on will be canceled
                            order.cancelOrder(this.marketId, this.runnerId, managedRunner, sendPostRequestRescriptMethod, reason);
                        } else if (exposureLeftUntilIStartToCancel >= sizeRemaining) { // keeping this order, and reducing the exposureLeftUntilIStartToCancel
                            exposureLeftUntilIStartToCancel -= sizeRemaining;
                        } else { // exposureLeftUntilIStartToCancel < excessPresentInOrder; partial cancelOrder
//                        final double removedExposure;
                            order.cancelOrder(this.marketId, this.runnerId, order.getSr() - exposureLeftUntilIStartToCancel, managedRunner, sendPostRequestRescriptMethod, reason);
                            exposureLeftUntilIStartToCancel = 0d;
//                            if (order.cancelOrder(this.marketId, this.runnerId, exposureLeftUntilIStartToCancel, removedExposureDuringThisManageIteration, managedRunner, sendPostRequestRescriptMethod, reason) > 0d) {
//                                exposureLeftUntilIStartToCancel = 0d;
//                            } else { // cancelOrder failed for some reason, nothing to be done
//                            }
//                        Generic.addToAtomicDouble(removedExposureDuringThisManageIteration, removedExposure);
                        }
                    }
                } // end for
            } else if (side == Side.L) {
                for (final Order order : sortedOrders) {
                    final Double price = order.getP();
                    if (price == null) {
                        logger.error("null price in cancelUnmatchedExceptGivenExposure for: {}", Generic.objectToString(order));
                    } else {
                        final double sizeRemaining = order.getSrConsideringTempCancel();
                        final double excessPresentInOrder = Formulas.calculateLayExposure(price, sizeRemaining);
                        if (excessPresentInOrder <= 0d) {
                            final Double sr = order.getSr();
                            if (sr == null || sr == 0) {
                                logger.error("order with zero or negative amount left in cancelUnmatchedExceptGivenExposure for: {} {} {} {} {} {} {}", price, sizeRemaining, side, notCanceledExposure, reason, Generic.objectToString(order),
                                             Generic.objectToString(this));
                            } else { // no error
                            }
                        } else if (exposureLeftUntilIStartToCancel < .1d) { // no more excess left, everything from now on will be canceled
                            order.cancelOrder(this.marketId, this.runnerId, managedRunner, sendPostRequestRescriptMethod, reason);
                        } else if (exposureLeftUntilIStartToCancel >= excessPresentInOrder) { // keeping this order, and reducing the exposureLeftUntilIStartToCancel
                            exposureLeftUntilIStartToCancel -= excessPresentInOrder;
                        } else { // exposureLeftUntilIStartToCancel < excessPresentInOrder; partial cancelOrder
                            order.cancelOrder(this.marketId, this.runnerId, order.getSr() - (Formulas.oddsAreUsable(price) ? exposureLeftUntilIStartToCancel / (price - 1d) : 0d), managedRunner, sendPostRequestRescriptMethod, reason);
                            exposureLeftUntilIStartToCancel = 0d;
//                            if (order.cancelOrder(this.marketId, this.runnerId, Formulas.oddsAreUsable(price) ? exposureLeftUntilIStartToCancel / (price - 1d) : null, removedExposureDuringThisManageIteration, managedRunner, sendPostRequestRescriptMethod,
//                                                  reason) > 0d) {
//                                exposureLeftUntilIStartToCancel = 0d;
//                            } else { // cancelOrder failed for some reason, nothing to be done
//                            }
//                        Generic.addToAtomicDouble(removedExposureDuringThisManageIteration, removedExposure);
                        }
                    }
                } // end for
            } else {
                logger.error("unknown side in cancelUnmatchedExceptGivenExposure for: {} {} {} {}", side, notCanceledExposure, reason, Generic.objectToString(this));
            }
        }
        return exposureLeftUntilIStartToCancel;
    }

    //    synchronized int cancelUnmatched(@NotNull final Method sendPostRequestRescriptMethod) { // cancel all unmatched orders
//        return cancelUnmatchedAtWorseOdds(null, 0d, sendPostRequestRescriptMethod, false);
//    }
//
//    synchronized int cancelUnmatched(final Side sideToCancel, @NotNull final Method sendPostRequestRescriptMethod) { // cancel all unmatched orders on that side
//        return cancelUnmatchedAtWorseOdds(sideToCancel, 0d, sendPostRequestRescriptMethod, false);
//    }
//
//    synchronized int cancelUnmatchedAtWorseOdds(final Side sideToCancel, final double worstNotCanceledOdds, @NotNull final Method sendPostRequestRescriptMethod) {
//        return cancelUnmatchedAtWorseOdds(sideToCancel, worstNotCanceledOdds, sendPostRequestRescriptMethod, false);
//    }

    synchronized int cancelUnmatchedAtWorseOdds(final Side sideToCancel, final double worstNotCanceledOdds, final double excessExposure, @NotNull final ManagedRunner managedRunner, @NotNull final Method sendPostRequestRescriptMethod,
                                                final boolean includeTheProvidedOdds, final String reason) {
        int modifications = 0;
        if (this.runnerId == null) {
            logger.error("null runnerId in orderMarketRunner.cancelAllUnmatched: {}", Generic.objectToString(this));
        } else {
            double excessExposureLeft = excessExposure - (sideToCancel == Side.B ? this.getBackTempCancelExposure() : this.getLayTempCancelExposure());
            @NotNull final Collection<Order> orders;
            if ((sideToCancel == Side.B || sideToCancel == Side.L) && excessExposure < Double.MAX_VALUE) {
                final Comparator<Double> priceComparator = sideToCancel == Side.B ? Comparator.reverseOrder() : Comparator.naturalOrder(); // order by reversed price or price, then reversed placed date
                orders = this.unmatchedOrders.
                                                     values().
                                                     stream().
                                                     filter(p -> p.getSide() == sideToCancel).
                                                     sorted(Comparator.comparing(Order::getP, Comparator.nullsFirst(priceComparator)).
                                                             thenComparing(Order::getPd, Comparator.nullsFirst(Comparator.reverseOrder()))).
                                                     collect(Collectors.toCollection(ArrayList::new));
            } else {
                orders = this.unmatchedOrders.values();
            }
            for (final Order order : orders) {
                if (order == null) {
                    logger.error("null order in cancelAllUnmatched for: {} {}", reason, Generic.objectToString(this));
                } else {
                    final Side side = order.getSide();
                    final Double price = order.getP();
                    final Double size = order.getSr();
                    final String betId = order.getId();
                    if (side == null || price == null || size == null || betId == null) {
                        logger.error("null order attributes in cancelAllUnmatched for: {} {} {} {} {} {}", side, price, size, betId, reason, Generic.objectToString(order));
                    } else {
                        final boolean shouldCancelOrder;
                        if (sideToCancel == null) { // cancel all orders
                            shouldCancelOrder = true;
                        } else if (sideToCancel != side) { // not the right side
                            shouldCancelOrder = false;
                        } else // odds matter and are worse
                            // odds matter but are not worse
                            if (worstNotCanceledOdds == 0d) { // right side, and odds don't matter
                                shouldCancelOrder = true;
                            } else if (includeTheProvidedOdds) {
                                shouldCancelOrder = Formulas.oddsAreEqual(worstNotCanceledOdds, price) || Formulas.oddsAreWorse(side, worstNotCanceledOdds, price);
                            } else {
                                shouldCancelOrder = Formulas.oddsAreWorse(side, worstNotCanceledOdds, price);
                            }

                        if (shouldCancelOrder) {
//                            modifications += Generic.booleanToInt(order.cancelOrder(this.marketId, this.runnerId, sendPostRequestRescriptMethod));
                            logger.info("{} cancelUnmatchedAtWorseOdds: {} {} {} price:{} size:{} excess:{} reasonId:{}", reason, this.marketId, this.runnerId, side, price, size, excessExposureLeft > 1_000_000_000d ? "max" : excessExposureLeft, reason);
                            final double removedExposure = order.removeExposure(this.marketId, this.runnerId, side, excessExposureLeft, managedRunner, sendPostRequestRescriptMethod, reason);
//                            Generic.addToAtomicDouble(removedExposureDuringThisManageIteration, removedExposure);
                            excessExposureLeft -= removedExposure;
                            modifications++;
                            if (excessExposureLeft <= 0d) {
                                break;
                            } else { // some exposure still left, will continue the loop
                            }
                        } else { // won't cancel, nothing to be done
                        }
                    }
                }
            } // end for
        }
        return modifications;
    }

    synchronized int cancelUnmatchedTooGoodOdds(@NotNull final Side sideToCancel, final double worstOddsThatAreGettingCanceled, @NotNull final ManagedRunner managedRunner, @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        int modifications = 0;
        if (this.runnerId == null || worstOddsThatAreGettingCanceled <= 0d) {
            logger.error("null runnerId or bogus worstOddsThatAreGettingCanceled in orderMarketRunner.cancelUnmatchedTooGoodOdds: {} {} {} {}", sideToCancel, worstOddsThatAreGettingCanceled, reason, Generic.objectToString(this));
        } else {
            for (final Order order : this.unmatchedOrders.values()) {
                if (order == null) {
                    logger.error("null order in cancelUnmatchedTooGoodOdds for: {} {}", reason, Generic.objectToString(this));
                } else {
                    final Side side = order.getSide();
                    final Double price = order.getP();
                    final Double size = order.getSr();
                    final String betId = order.getId();
                    if (side == null || price == null || size == null || betId == null) {
                        logger.error("null order attributes in cancelUnmatchedTooGoodOdds for: {} {} {} {} {} {}", side, price, size, betId, reason, Generic.objectToString(order));
                    } else {
                        // odds matter and are same or better
                        // odds matter but are not worse
                        // not the right side
                        final boolean shouldCancelOrder = sideToCancel == side && !Formulas.oddsAreWorse(side, worstOddsThatAreGettingCanceled, price);
                        if (shouldCancelOrder) {
                            final double removedExposure = order.cancelOrder(this.marketId, this.runnerId, managedRunner, sendPostRequestRescriptMethod, reason);
                            modifications += Generic.booleanToInt(removedExposure > 0d);
//                            Generic.addToAtomicDouble(removedExposureDuringThisManageIteration, removedExposure);
                        } else { // won't cancel, nothing to be done
                        }
                    }
                }
            } // end for
        }
        return modifications;
    }

    @NotNull
    synchronized OrdersList getUnmatchedBackAmounts() {
        removeExpiredModifications();
        final TreeMap<Double, Double> orders = new TreeMap<>(Comparator.naturalOrder());
        for (final Order order : this.unmatchedOrders.values()) {
            if (order == null) {
                logger.error("null order in getUnmatchedBackAmounts for: {}", Generic.objectToString(this));
            } else {
                final Side side = order.getSide();
                if (side == null) {
                    logger.error("null side in getUnmatchedBackAmounts for: {} {}", Generic.objectToString(order), Generic.objectToString(this));
                } else if (side == Side.B) {
                    final Double price = order.getP(), remainingSize = order.getSr();
                    if (price == null || remainingSize == null) {
                        logger.error("null price or remainingSize in getUnmatchedBackAmounts for: {} {}", Generic.objectToString(order), Generic.objectToString(this));
                    } else {
                        final Double existingMapValue = orders.get(price);
                        final double existingMapValuePrimitive = existingMapValue == null ? 0d : existingMapValue;
                        orders.put(price, existingMapValuePrimitive + remainingSize);
                    }
                } else { // wrong side, nothing to be done
                }
            }
        } // end for
        final LinkedList<RunnerOrderModification> backRecentModifications = new LinkedList<>();
        for (final RunnerOrderModification runnerOrderModification : this.recentModifications) {
            if (runnerOrderModification == null) {
                SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null runnerOrderModification in getUnmatchedBackAmounts");
            } else {
                final Side side = runnerOrderModification.getSide();
                if (side == Side.B) {
                    backRecentModifications.add(runnerOrderModification);
                } else { // not the side I want
                }
            }
        }
        return new OrdersList(orders, backRecentModifications);
    }

    @NotNull
    synchronized OrdersList getUnmatchedLayAmounts() {
        removeExpiredModifications();
        final TreeMap<Double, Double> orders = new TreeMap<>(Comparator.reverseOrder());
        for (final Order order : this.unmatchedOrders.values()) {
            if (order == null) {
                logger.error("null order in getUnmatchedLayAmounts for: {}", Generic.objectToString(this));
            } else {
                final Side side = order.getSide();
                if (side == null) {
                    logger.error("null side in getUnmatchedLayAmounts for: {} {}", Generic.objectToString(order), Generic.objectToString(this));
                } else if (side == Side.L) {
                    final Double price = order.getP(), remainingSize = order.getSr();
                    if (price == null || remainingSize == null) {
                        logger.error("null price or remainingSize in getUnmatchedLayAmounts for: {} {}", Generic.objectToString(order), Generic.objectToString(this));
                    } else {
                        final Double existingMapValue = orders.get(price);
                        final double existingMapValuePrimitive = existingMapValue == null ? 0d : existingMapValue;
                        orders.put(price, existingMapValuePrimitive + remainingSize);
                    }
                } else { // wrong side, nothing to be done
                }
            }
        }
        final LinkedList<RunnerOrderModification> layRecentModifications = new LinkedList<>();
        for (final RunnerOrderModification runnerOrderModification : this.recentModifications) {
            if (runnerOrderModification == null) {
                SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null runnerOrderModification in getUnmatchedLayAmounts");
            } else {
                final Side side = runnerOrderModification.getSide();
                if (side == Side.L) {
                    layRecentModifications.add(runnerOrderModification);
                } else { // not the side I want
                }
            }
        }
        return new OrdersList(orders, layRecentModifications);
    }
}
