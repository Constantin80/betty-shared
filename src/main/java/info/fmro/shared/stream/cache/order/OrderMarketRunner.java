package info.fmro.shared.stream.cache.order;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.objects.TwoDoubles;
import info.fmro.shared.stream.definitions.Order;
import info.fmro.shared.stream.definitions.OrderRunnerChange;
import info.fmro.shared.stream.definitions.PriceSizeLadder;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("OverlyComplexClass")
public class OrderMarketRunner
        implements Serializable { // amounts are in account currency (EUR)
    private static final long serialVersionUID = 6359709424181107081L;
    private static final Logger logger = LoggerFactory.getLogger(OrderMarketRunner.class);
    //    private final OrderMarket orderMarket;
    private final String marketId;
    private final RunnerId runnerId;
    private final PriceSizeLadder layMatches = PriceSizeLadder.newLay();
    private final PriceSizeLadder backMatches = PriceSizeLadder.newBack();
    private final Map<String, Order> unmatchedOrders = new ConcurrentHashMap<>(2); // only place where orders are permanently stored

    OrderMarketRunner(final String marketId, final RunnerId runnerId) {
        if (marketId == null || runnerId == null) {
            logger.error("bad arguments when creating OrderMarketRunner: {} {}", marketId, Generic.objectToString(runnerId));
        } else { // no error message, constructor continues normally
        }
        this.marketId = marketId;
        this.runnerId = runnerId;
    }

    synchronized void onOrderRunnerChange(final OrderRunnerChange orderRunnerChange, final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate) {
        if (pendingOrdersThread != null) {
            pendingOrdersThread.reportStreamChange(this, orderRunnerChange); // needs to happen at the start of the method, before I modify this object
        } else { // I must be in the Client, normal behaviour, nothing to be done
        }

        final boolean isImage = Boolean.TRUE.equals(orderRunnerChange.getFullImage());

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
        this.layMatches.onPriceChange(isImage, orderRunnerChange.getMl(), currencyRate);
        this.backMatches.onPriceChange(isImage, orderRunnerChange.getMb(), currencyRate);
    }

    public synchronized boolean isEmpty() {
        return this.unmatchedOrders.isEmpty() && this.backMatches.isEmpty() && this.layMatches.isEmpty();
    }

    public synchronized Order getUnmatchedOrder(final String betId) {
        @Nullable final Order returnValue;

        if (betId != null) {
            returnValue = this.unmatchedOrders.get(betId);
        } else {
            logger.error("null betId in getUnmatchedOrder");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized double getMatchedSize(final Side side, final double price, @NotNull final AtomicDouble currencyRate) {
        final double matchedSize;
        if (side == Side.B) {
            matchedSize = this.backMatches.getMatchedSize(price, currencyRate);
        } else if (side == Side.L) {
            matchedSize = this.layMatches.getMatchedSize(price, currencyRate);
        } else {
            logger.error("unknown Side in getMatchedSize for: {} {}", side, price);
            matchedSize = 0d;
        }
        return matchedSize;
    }

    public synchronized RunnerId getRunnerId() {
        return this.runnerId;
    }

    public synchronized String getMarketId() {
        return this.marketId;
    }

    public synchronized PriceSizeLadder getBackMatches() {
        return this.backMatches.copy();
    }

    public synchronized PriceSizeLadder getLayMatches() {
        return this.layMatches.copy();
    }

    public synchronized HashMap<String, Order> getUnmatchedOrders() {
        return new HashMap<>(this.unmatchedOrders);
    }

    public synchronized void getUnmatchedExposureAndProfit(@NotNull final Exposure exposure) {
        double unmatchedBackExposure = 0d, unmatchedLayExposure = 0d, unmatchedBackProfit = 0d, unmatchedLayProfit = 0d;
        for (final Order order : this.unmatchedOrders.values()) {
            order.calculateExposureAndProfit();
            unmatchedBackExposure += order.getBackExposure();
            unmatchedLayExposure += order.getLayExposure();
            unmatchedBackProfit += order.getBackProfit();
            unmatchedLayProfit += order.getLayProfit();
        }
        exposure.setBackUnmatchedExposure(unmatchedBackExposure);
        exposure.setLayUnmatchedExposure(unmatchedLayExposure);
        exposure.setBackUnmatchedProfit(unmatchedBackProfit);
        exposure.setLayUnmatchedProfit(unmatchedLayProfit);
    }

    public synchronized void getMatchedExposure(@NotNull final Exposure exposure) {
        final TwoDoubles backProfitExposurePair = this.backMatches.getBackProfitExposurePair();
        final double backProfit = backProfitExposurePair.getFirstDouble();
        final double backExposure = backProfitExposurePair.getSecondDouble();
        final TwoDoubles layProfitExposurePair = this.layMatches.getBackProfitExposurePair(); // same method, just reverse the result pair
        final double layExposure = layProfitExposurePair.getFirstDouble();
        final double layProfit = layProfitExposurePair.getSecondDouble();

        exposure.setBackMatchedExposure(backExposure - layProfit);
        exposure.setLayMatchedExposure(layExposure - backProfit);
    }

    public synchronized int cancelUnmatchedAmounts(final double backExcessExposure, final double layExcessExposure, final OrdersThreadInterface pendingOrdersThread) {
        // best odds are removed first, as they're most unlikely to get matched
        // always the newer orders are removed first
        int exposureHasBeenModified = 0;
        if (backExcessExposure >= .1d) {
            final ArrayList<Order> backOrdersSorted =
                    this.unmatchedOrders.
                                                values().
                                                stream().
                                                filter(p -> p.getSide() == Side.B).
                                                sorted(Comparator.comparing(Order::getP, Comparator.reverseOrder()).
                                                        thenComparing(Order::getPd, Comparator.reverseOrder())).
                                                collect(Collectors.toCollection(ArrayList::new)); // order by reversed price, then reversed placed date
            double excessExposureLeft = backExcessExposure;
            for (final Order order : backOrdersSorted) {
                excessExposureLeft = order.removeBackExposure(this.marketId, this.runnerId, excessExposureLeft, pendingOrdersThread);
                exposureHasBeenModified++;
                if (excessExposureLeft <= 0d) {
                    break;
                } else { // some exposure still left, will continue the loop
                }
            }
        } else { // no excess exposure present, nothing to do
        }

        if (layExcessExposure >= .1d) {
            final ArrayList<Order> layOrdersSorted =
                    this.unmatchedOrders.
                                                values().
                                                stream().
                                                filter(p -> p.getSide() == Side.L).
                                                sorted(Comparator.comparing(Order::getP).
                                                        thenComparing(Order::getPd, Comparator.reverseOrder())).
                                                collect(Collectors.toCollection(ArrayList::new)); // order by price, then reversed placed date
            double excessExposureLeft = layExcessExposure;
            for (final Order order : layOrdersSorted) {
                excessExposureLeft = order.removeLayExposure(this.marketId, this.runnerId, excessExposureLeft, pendingOrdersThread);
                exposureHasBeenModified++;
                if (excessExposureLeft <= 0d) {
                    break;
                } else { // some exposure still left, will continue the loop
                }
            }
        } else { // no excess exposure present, nothing to do
        }

        return exposureHasBeenModified;
    }

    public synchronized double cancelUnmatchedExceptExcessOnTheOtherSide(final Side side, final double excessOnTheOtherSide, final OrdersThreadInterface pendingOrdersThread) {
        // worst odds are kept, as they're most likely to get matched
        // older orders are kept
        double excessOnTheOtherSideRemaining = excessOnTheOtherSide;
        if (side == null) {
            logger.error("null side in cancelUnmatchedExceptExcessOnTheOtherSide for: {} {}", excessOnTheOtherSide, Generic.objectToString(this));
        } else if (side == Side.B) {
            final ArrayList<Order> backOrdersSorted =
                    this.unmatchedOrders.
                                                values().
                                                stream().
                                                filter(p -> p.getSide() == Side.B).
                                                sorted(Comparator.comparing(Order::getP).
                                                        thenComparing(Order::getPd)).
                                                collect(Collectors.toCollection(ArrayList::new)); // order by price, then placed date
            for (final Order order : backOrdersSorted) {
                final double price = order.getP();
                final double sizeRemaining = order.getSr();
                final double excessPresentInOrder = price * sizeRemaining;
                if (excessPresentInOrder <= 0d) { // it's dangerous to try to fix the error here; orders with zero size remaining are removed from cache when the orderChange happens, so I shouldn't get zero size here
                    logger.error("order with zero or negative amount left in cancelUnmatchedExceptExcessOnTheOtherSide for: {} {} {} {} {} {}", price, sizeRemaining, side, excessOnTheOtherSide, Generic.objectToString(order), Generic.objectToString(this));
                } else if (excessOnTheOtherSideRemaining < .1d) { // no more excess left, everything from now on will be canceled
                    order.cancelOrder(this.marketId, this.runnerId, pendingOrdersThread);
                } else if (excessOnTheOtherSideRemaining >= excessPresentInOrder) { // keeping this order, and reducing the excessOnTheOtherSideRemaining
                    excessOnTheOtherSideRemaining -= excessPresentInOrder;
                } else { // excessOnTheOtherSideRemaining < excessPresentInOrder; partial cancelOrder
                    if (order.cancelOrder(this.marketId, this.runnerId, excessOnTheOtherSideRemaining / price, pendingOrdersThread)) {
                        excessOnTheOtherSideRemaining = 0d;
                    } else { // cancelOrder failed for some reason, nothing to be done
                    }
                }
            } // end for
        } else if (side == Side.L) {
            final ArrayList<Order> layOrdersSorted =
                    this.unmatchedOrders.
                                                values().
                                                stream().
                                                filter(p -> p.getSide() == Side.L).
                                                sorted(Comparator.comparing(Order::getP, Comparator.reverseOrder()).
                                                        thenComparing(Order::getPd)).
                                                collect(Collectors.toCollection(ArrayList::new)); // order by reversed price, then placed date
            for (final Order order : layOrdersSorted) {
                final double price = order.getP();
                final double sizeRemaining = order.getSr();
                final double excessPresentInOrder = price * sizeRemaining;
                if (excessPresentInOrder <= 0d) {
                    logger.error("order with zero or negative amount left in cancelUnmatchedExceptExcessOnTheOtherSide for: {} {} {} {} {} {}", price, sizeRemaining, side, excessOnTheOtherSide, Generic.objectToString(order), Generic.objectToString(this));
                } else if (excessOnTheOtherSideRemaining < .1d) { // no more excess left, everything from now on will be canceled
                    order.cancelOrder(this.marketId, this.runnerId, pendingOrdersThread);
                } else if (excessOnTheOtherSideRemaining >= excessPresentInOrder) { // keeping this order, and reducing the excessOnTheOtherSideRemaining
                    excessOnTheOtherSideRemaining -= excessPresentInOrder;
                } else { // excessOnTheOtherSideRemaining < excessPresentInOrder; partial cancelOrder
                    if (order.cancelOrder(this.marketId, this.runnerId, excessOnTheOtherSideRemaining / price, pendingOrdersThread)) {
                        excessOnTheOtherSideRemaining = 0d;
                    } else { // cancelOrder failed for some reason, nothing to be done
                    }
                }
            } // end for
        } else {
            logger.error("unknown side in cancelUnmatchedExceptExcessOnTheOtherSide for: {} {} {}", side, excessOnTheOtherSide, Generic.objectToString(this));
        }
        return excessOnTheOtherSideRemaining;
    }

    public synchronized int cancelUnmatched(final OrdersThreadInterface pendingOrdersThread) { // cancel all unmatched orders
        return cancelUnmatched(null, 0d, pendingOrdersThread);
    }

    public synchronized int cancelUnmatched(final Side sideToCancel, final OrdersThreadInterface pendingOrdersThread) { // cancel all unmatched orders on that side
        return cancelUnmatched(sideToCancel, 0d, pendingOrdersThread);
    }

    public synchronized int cancelUnmatched(final Side sideToCancel, final double worstNotCanceledOdds, final OrdersThreadInterface pendingOrdersThread) {
        int modifications = 0;
        if (this.runnerId == null) {
            logger.error("null runnerId in orderMarketRunner.cancelAllUnmatched: {}", Generic.objectToString(this));
        } else {
            for (final Order order : this.unmatchedOrders.values()) {
                if (order == null) {
                    logger.error("null order in cancelAllUnmatched for: {}", Generic.objectToString(this));
                } else {
                    final Side side = order.getSide();
                    final Double price = order.getP();
                    final Double size = order.getSr();
                    final String betId = order.getId();
                    if (side == null || price == null || size == null || betId == null) {
                        logger.error("null order attributes in cancelAllUnmatched for: {} {} {} {} {}", side, price, size, betId, Generic.objectToString(order));
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
                            } else {
                                shouldCancelOrder = info.fmro.shared.utility.Formulas.oddsAreWorse(side, worstNotCanceledOdds, price);
                            }

                        if (shouldCancelOrder) {
                            modifications += Generic.booleanToInt(order.cancelOrder(this.marketId, this.runnerId, pendingOrdersThread));
                        } else { // won't cancel, nothing to be done
                        }
                    }
                }
            } // end for
        }
        return modifications;
    }

    public synchronized int cancelUnmatchedTooGoodOdds(@NotNull final Side sideToCancel, final double worstOddsThatAreGettingCanceled, final OrdersThreadInterface pendingOrdersThread) {
        int modifications = 0;
        if (this.runnerId == null || worstOddsThatAreGettingCanceled <= 0d) {
            logger.error("null runnerId or bogus worstOddsThatAreGettingCanceled in orderMarketRunner.cancelUnmatchedTooGoodOdds: {} {} {}", sideToCancel, worstOddsThatAreGettingCanceled, Generic.objectToString(this));
        } else {
            for (final Order order : this.unmatchedOrders.values()) {
                if (order == null) {
                    logger.error("null order in cancelUnmatchedTooGoodOdds for: {}", Generic.objectToString(this));
                } else {
                    final Side side = order.getSide();
                    final Double price = order.getP();
                    final Double size = order.getSr();
                    final String betId = order.getId();
                    if (side == null || price == null || size == null || betId == null) {
                        logger.error("null order attributes in cancelUnmatchedTooGoodOdds for: {} {} {} {} {}", side, price, size, betId, Generic.objectToString(order));
                    } else {
                        // odds matter and are same or better
                        // odds matter but are not worse
                        // not the right side
                        final boolean shouldCancelOrder = sideToCancel == side && !Formulas.oddsAreWorse(side, worstOddsThatAreGettingCanceled, price);
                        if (shouldCancelOrder) {
                            modifications += Generic.booleanToInt(order.cancelOrder(this.marketId, this.runnerId, pendingOrdersThread));
                        } else { // won't cancel, nothing to be done
                        }
                    }
                }
            } // end for
        }
        return modifications;
    }

    @NotNull
    public synchronized TreeMap<Double, Double> getUnmatchedBackAmounts() {
        final TreeMap<Double, Double> result = new TreeMap<>(Comparator.naturalOrder());
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
                        final Double existingMapValue = result.get(price);
                        final double existingMapValuePrimitive = existingMapValue == null ? 0d : existingMapValue;
                        result.put(price, existingMapValuePrimitive + remainingSize);
                    }
                } else { // wrong side, nothing to be done
                }
            }
        }
        return result;
    }

    @NotNull
    public synchronized TreeMap<Double, Double> getUnmatchedLayAmounts() {
        final TreeMap<Double, Double> result = new TreeMap<>(Comparator.reverseOrder());
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
                        final Double existingMapValue = result.get(price);
                        final double existingMapValuePrimitive = existingMapValue == null ? 0d : existingMapValue;
                        result.put(price, existingMapValuePrimitive + remainingSize);
                    }
                } else { // wrong side, nothing to be done
                }
            }
        }
        return result;
    }
}
