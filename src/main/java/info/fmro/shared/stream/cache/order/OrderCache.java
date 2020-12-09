package info.fmro.shared.stream.cache.order;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.betapi.CancelOrdersThread;
import info.fmro.shared.betapi.PlaceOrdersThread;
import info.fmro.shared.entities.CancelInstruction;
import info.fmro.shared.entities.LimitOrder;
import info.fmro.shared.entities.PlaceInstruction;
import info.fmro.shared.enums.OrderType;
import info.fmro.shared.enums.PersistenceType;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.enums.TemporaryOrderType;
import info.fmro.shared.logic.BetFrequencyLimit;
import info.fmro.shared.logic.ExistingFunds;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.logic.RulesManager;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.objects.TemporaryOrder;
import info.fmro.shared.stream.cache.OrdersList;
import info.fmro.shared.stream.definitions.Order;
import info.fmro.shared.stream.definitions.OrderMarketChange;
import info.fmro.shared.stream.definitions.OrderRunnerChange;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.stream.protocol.ChangeMessage;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import info.fmro.shared.utility.SynchronizedMap;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({"OverlyComplexClass", "ClassWithTooManyMethods", "OverlyCoupledClass"})
public class OrderCache
        implements Serializable, StreamObjectInterface {
    private static final Logger logger = LoggerFactory.getLogger(OrderCache.class);
    @Serial
    private static final long serialVersionUID = -6023803756520072425L;
    public transient ListOfQueues listOfQueues = new ListOfQueues();
    public final SynchronizedMap<String, OrderMarket> markets = new SynchronizedMap<>(4); // only place where orderMarkets are permanently stored
    private boolean orderMarketRemovedOnClose = true; // default
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    public transient AtomicLong initializedStamp = new AtomicLong();
    private final Collection<TemporaryOrder> temporaryOrders = new ArrayList<>(2);

    @Serial
    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();
        this.initializedStamp = new AtomicLong();
    }

    public synchronized OrderCache getCopy() {
        return SerializationUtils.clone(this);
    }

    @SuppressWarnings("NonPrivateFieldAccessedInSynchronizedContext")
    public synchronized boolean copyFromStream(final OrderCache other) {
        final boolean readSuccessful;
        if (other == null) {
            logger.error("null other in copyFromStream for: {}", Generic.objectToString(this));
            readSuccessful = false;
        } else {
//            Generic.updateObject(this, other);

            this.markets.clear();
            this.markets.putAll(other.markets.copy());
            this.orderMarketRemovedOnClose = other.orderMarketRemovedOnClose;

            this.temporaryOrders.clear(); // no need to send these through stream, the whole OrderCache object is sent
            this.temporaryOrders.addAll(other.temporaryOrders);
            addAllTempCancelToOrderMarketRunner();

            readSuccessful = true;
        }

        final int nQueues = this.listOfQueues.size();
        if (nQueues == 0) { // normal case, nothing to be done
        } else {
            logger.error("existing queues during OrderCache.copyFromStream: {} {}", nQueues, Generic.objectToString(this));
            this.listOfQueues.clear();
        }
        return readSuccessful;
    }

//    public synchronized boolean hasBeenInitializedFromStream() {
//        return this.hasBeenInitializedFromStream;
//    }

    public synchronized void onOrderChange(@NotNull final ChangeMessage<? extends OrderMarketChange> changeMessage, @NotNull final RulesManager rulesManager) {
        final boolean isStartOfNewSubscription = changeMessage.isStartOfNewSubscription();
        if (isStartOfNewSubscription) {
//            final Collection<OrderMarket> values = this.markets.valuesCopy();
//            for (final OrderMarket orderMarket : values) {
//                if (orderMarket == null) {
//                    logger.error("null orderMarket during onOrderChange reset for: {}", Generic.objectToString(this.markets.copy()));
//                } else {
//                    orderMarket.markObsoleteObject();
//                }
//            }
            this.markets.clear();
//            orderCacheHasReset.set(true);
            //noinspection NonPrivateFieldAccessedInSynchronizedContext
            this.initializedStamp.set(System.currentTimeMillis());
        }

        if (changeMessage.getItems() != null) {
            final Collection<String> marketIds = new HashSet<>(2);
            for (final OrderMarketChange change : changeMessage.getItems()) {
                if (change == null) {
                    logger.error("null change in onOrderChange for: {}", Generic.objectToString(changeMessage));
                } else {
                    marketIds.add(change.getId());
                    final OrderMarket orderMarket = onOrderMarketChange(change, rulesManager.newOrderMarketCreated);
                    if (orderMarket.isEmpty() || (this.orderMarketRemovedOnClose && orderMarket.isClosed())) {
                        // remove on close or if empty
                        this.markets.remove(orderMarket.getMarketId());
                    }
                }
            } // end for
            rulesManager.marketsToCheck.put(marketIds, System.currentTimeMillis());
        } else { // maybe it's normal, nothing to be done
        }

        if (isStartOfNewSubscription) {
            addAllTempCancelToOrderMarketRunner();
        }
    }

    @NotNull
    private synchronized OrderMarket onOrderMarketChange(@NotNull final OrderMarketChange orderMarketChange, @NotNull final AtomicBoolean newOrderMarketCreated) {
        final String marketId = orderMarketChange.getId();
        final boolean newMarketIsBeingAdded = !this.markets.containsKey(marketId);
        final OrderMarket orderMarket;

        if (newMarketIsBeingAdded) {
            orderMarket = addNewMarket(marketId, newOrderMarketCreated, orderMarketChange);

//            final ManagedMarket managedMarket = rulesManager.markets.get(marketId);
//            if (managedMarket == null) { // no managedMarket present, nothing to be done
//            } else {
//                managedMarket.attachOrderMarket(rulesManager, marketCataloguesMap);
//            }
        } else { // market was already present, nothing to be done
            orderMarket = this.markets.get(marketId);
            orderMarket.onOrderMarketChange(orderMarketChange);
        }
        return orderMarket;
    }

    private synchronized void addAllTempCancelToOrderMarketRunner() {
        for (final TemporaryOrder temporaryOrder : this.temporaryOrders) {
            if (temporaryOrder == null) {
                logger.error("null temporaryOrder in addAllTempCancelToOrderMarketRunner");
            } else {
                addTempCancelToOrderMarketRunner(temporaryOrder);
            }
        }
    }

    private synchronized void addTempCancelToOrderMarketRunner(@NotNull final TemporaryOrder temporaryOrder) {
        final TemporaryOrderType temporaryOrderType = temporaryOrder.getType();
        if (temporaryOrderType == TemporaryOrderType.CANCEL) {
            final String betId = temporaryOrder.getBetId();
            if (betId == null) {
                logger.error("null betId in addTempCancelToOrderMarketRunner for: {}", temporaryOrder);
            } else {
                final Order order = getUnmatchedOrder(temporaryOrder.getMarketId(), temporaryOrder.getRunnerId(), betId);
                if (order == null) {
                    logger.error("null order in addTempCancelToOrderMarketRunner for: {}", Generic.objectToString(temporaryOrder));
                } else {
                    order.addSizeTempCanceled(temporaryOrder.getSizeReduction());
                }
            }
        } else { // normal, not cancel order, nothing to be done
        }
    }

    private synchronized void removeTempCancelToOrderMarketRunner(@NotNull final TemporaryOrder temporaryOrder) {
        final TemporaryOrderType temporaryOrderType = temporaryOrder.getType();
        if (temporaryOrderType == TemporaryOrderType.CANCEL) {
            final String betId = temporaryOrder.getBetId();
            if (betId == null) {
                logger.error("null betId in removeTempCancelToOrderMarketRunner for: {}", temporaryOrder);
            } else {
                final Order order = getUnmatchedOrder(temporaryOrder.getMarketId(), temporaryOrder.getRunnerId(), betId);
                if (order == null) { // can be normal, the order has just been removed for being empty, nothing to be done
                } else {
                    order.removeSizeTempCanceled(temporaryOrder.getSizeReduction());
                }
            }
        } else { // normal, not cancel order, nothing to be done
        }
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public synchronized void addTempOrder(@NotNull final TemporaryOrder temporaryOrder) {
        this.temporaryOrders.add(temporaryOrder);
        addTempCancelToOrderMarketRunner(temporaryOrder);
        //noinspection NonPrivateFieldAccessedInSynchronizedContext
        this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addTempOrder, temporaryOrder));
    }

    public synchronized void removeTempOrder(@NotNull final TemporaryOrder temporaryOrder) {
        removeTempCancelToOrderMarketRunner(temporaryOrder);
        while (this.temporaryOrders.remove(temporaryOrder)) { // empty while, removes all equal elements from list
        }
        //noinspection NonPrivateFieldAccessedInSynchronizedContext
        this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeTempOrder, temporaryOrder));
    }

    private synchronized void removeTempOrder(@NotNull final Iterator<TemporaryOrder> iterator, final TemporaryOrder temporaryOrder) {
        iterator.remove();
        removeTempCancelToOrderMarketRunner(temporaryOrder);
        //noinspection NonPrivateFieldAccessedInSynchronizedContext
        this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeTempOrder, temporaryOrder));
    }

    @NotNull
    public synchronized Collection<TemporaryOrder> getTempOrders(final String marketId, @NotNull final RunnerId runnerId) {
        final Collection<TemporaryOrder> returnList = new ArrayList<>(2);
        for (final TemporaryOrder temporaryOrder : this.temporaryOrders) {
            if (temporaryOrder == null) {
                logger.error("null temporaryOrder during getTempOrders"); // this should never happen, no need to try to fix it
            } else if (temporaryOrder.runnerEquals(marketId, runnerId)) {
                returnList.add(temporaryOrder);
            } else { // order not on the runner I'm interested in, nothing to be done
            }
        }
        return returnList;
    }

    @NotNull
    public synchronized OrdersList getUnmatchedBackAmounts(final String marketId, @NotNull final RunnerId runnerId) {
        final OrderMarketRunner runner = getOrderMarketRunner(marketId, runnerId);
        return runner == null ? new OrdersList(new TreeMap<>(Comparator.naturalOrder()), new LinkedList<>()) : runner.getUnmatchedBackAmounts();
    }

    @NotNull
    public synchronized OrdersList getUnmatchedLayAmounts(final String marketId, @NotNull final RunnerId runnerId) {
        final OrderMarketRunner runner = getOrderMarketRunner(marketId, runnerId);
        return runner == null ? new OrdersList(new TreeMap<>(Comparator.reverseOrder()), new LinkedList<>()) : runner.getUnmatchedLayAmounts();
    }

    public synchronized int cancelUnmatchedAmounts(final String marketId, @NotNull final RunnerId runnerId, final double backExcessExposure, final double layExcessExposure, @NotNull final Method sendPostRequestRescriptMethod,
                                                   final AtomicDouble removedExposureDuringThisManageIterationBack, final AtomicDouble removedExposureDuringThisManageIterationLay, final String reason) {
        final OrderMarketRunner runner = getOrderMarketRunner(marketId, runnerId);
        return runner == null ? 0 : runner.cancelUnmatchedAmounts(backExcessExposure, layExcessExposure, sendPostRequestRescriptMethod, removedExposureDuringThisManageIterationBack, removedExposureDuringThisManageIterationLay, reason);
    }

    public synchronized double cancelUnmatchedExceptExcessOnTheOtherSide(final String marketId, @NotNull final RunnerId runnerId, final Side side, final double excessOnTheOtherSide, @NotNull final Method sendPostRequestRescriptMethod,
                                                                         final AtomicDouble removedExposureDuringThisManageIteration, final String reason) {
        final OrderMarketRunner runner = getOrderMarketRunner(marketId, runnerId);
        return runner == null ? 0 : runner.cancelUnmatchedExceptGivenExposure(side, excessOnTheOtherSide, sendPostRequestRescriptMethod, removedExposureDuringThisManageIteration, reason);
    }

    public synchronized int cancelUnmatched(final String marketId, @NotNull final Method sendPostRequestRescriptMethod, final String reason) { // cancel all unmatched orders
        final OrderMarket orderMarket = this.getOrderMarket(marketId);
        return orderMarket == null ? 0 : orderMarket.cancelUnmatchedAtWorseOdds(null, 0d, Double.MAX_VALUE, sendPostRequestRescriptMethod, false, reason);
    }

    public synchronized int cancelUnmatched(final String marketId, @NotNull final RunnerId runnerId, @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        // cancel all unmatched orders
        return cancelUnmatchedAtWorseOdds(marketId, runnerId, null, 0d, Double.MAX_VALUE, sendPostRequestRescriptMethod, false, null, reason);
    }

    public synchronized int cancelUnmatched(final String marketId, @NotNull final RunnerId runnerId, final Side sideToCancel, @NotNull final Method sendPostRequestRescriptMethod, final AtomicDouble removedExposureDuringThisManageIteration,
                                            final String reason) { // cancel all unmatched orders on that side
        return cancelUnmatchedAtWorseOdds(marketId, runnerId, sideToCancel, 0d, Double.MAX_VALUE, sendPostRequestRescriptMethod, false, removedExposureDuringThisManageIteration, reason);
    }

    public synchronized int cancelUnmatched(final String marketId, @NotNull final RunnerId runnerId, final Side sideToCancel, final double worstNotCanceledOdds, @NotNull final Method sendPostRequestRescriptMethod,
                                            final AtomicDouble removedExposureDuringThisManageIteration, final String reason) {
        // cancel all unmatched orders
        return cancelUnmatchedAtWorseOdds(marketId, runnerId, sideToCancel, worstNotCanceledOdds, Double.MAX_VALUE, sendPostRequestRescriptMethod, false, removedExposureDuringThisManageIteration, reason);
    }

    public synchronized int cancelUnmatchedAtWorseOdds(final String marketId, @NotNull final RunnerId runnerId, final Side sideToCancel, final double worstNotCanceledOdds, @NotNull final Method sendPostRequestRescriptMethod,
                                                       final AtomicDouble removedExposureDuringThisManageIteration, final String reason) {
        return cancelUnmatchedAtWorseOdds(marketId, runnerId, sideToCancel, worstNotCanceledOdds, Double.MAX_VALUE, sendPostRequestRescriptMethod, false, removedExposureDuringThisManageIteration, reason);
    }

    public synchronized int cancelUnmatchedAtWorseOdds(final String marketId, @NotNull final RunnerId runnerId, final Side sideToCancel, final double worstNotCanceledOdds, @NotNull final Method sendPostRequestRescriptMethod,
                                                       final boolean includeTheProvidedOdds, final AtomicDouble removedExposureDuringThisManageIteration, final String reason) {
        return cancelUnmatchedAtWorseOdds(marketId, runnerId, sideToCancel, worstNotCanceledOdds, Double.MAX_VALUE, sendPostRequestRescriptMethod, includeTheProvidedOdds, removedExposureDuringThisManageIteration, reason);
    }

    public synchronized int cancelUnmatchedAtWorseOdds(final String marketId, @NotNull final RunnerId runnerId, final Side sideToCancel, final double worstNotCanceledOdds, final double excessExposure, @NotNull final Method sendPostRequestRescriptMethod,
                                                       final boolean includeTheProvidedOdds, final AtomicDouble removedExposureDuringThisManageIteration, final String reason) {
        final OrderMarketRunner runner = getOrderMarketRunner(marketId, runnerId);
        return runner == null ? 0 : runner.cancelUnmatchedAtWorseOdds(sideToCancel, worstNotCanceledOdds, excessExposure, sendPostRequestRescriptMethod, includeTheProvidedOdds, removedExposureDuringThisManageIteration, reason);
    }

    public synchronized int cancelUnmatchedTooGoodOdds(final String marketId, @NotNull final RunnerId runnerId, @NotNull final Side sideToCancel, final double worstOddsThatAreGettingCanceled, @NotNull final Method sendPostRequestRescriptMethod,
                                                       final AtomicDouble removedExposureDuringThisManageIteration, final String reason) {
        final OrderMarketRunner runner = getOrderMarketRunner(marketId, runnerId);
        return runner == null ? 0 : runner.cancelUnmatchedTooGoodOdds(sideToCancel, worstOddsThatAreGettingCanceled, sendPostRequestRescriptMethod, removedExposureDuringThisManageIteration, reason);
    }

    public synchronized void updateExposure(@NotNull final ManagedRunner managedRunner) {
        final OrderMarketRunner runner = getOrderMarketRunner(managedRunner);
        if (runner == null) {
            managedRunner.resetMatchedAndUnmatchedExposure();
        } else {
            runner.updateArgumentExposure(managedRunner);
        }
        updateExposureFromTemporaryOrders(managedRunner);
    }

//    @NotNull
//    @Unmodifiable
//    private synchronized List<Exposure> getExposure(final String marketId, @NotNull final RunnerId firstRunnerId, @NotNull final RunnerId secondRunnerId) {
//        return List.of(getExposure(marketId, firstRunnerId), getExposure(marketId, secondRunnerId));
//    }

//    @NotNull
//    private synchronized Exposure getExposure(final String marketId, @NotNull final RunnerId runnerId) {
//        @NotNull final Exposure exposureFromMap;
//        final OrderMarket orderMarket = this.markets.get(marketId);
//        if (orderMarket == null) {
//            exposureFromMap = new Exposure();
//        } else {
//            exposureFromMap = orderMarket.getExposure(runnerId);
//        }
//        updateExposureFromTemporaryOrders(marketId, runnerId, exposureFromMap);
//        return exposureFromMap;
//    }

    private synchronized void updateExposureFromTemporaryOrders(@NotNull final ManagedRunner managedRunner) {
        managedRunner.resetTempExposure();
        final String marketId = managedRunner.getMarketId();
        final RunnerId runnerId = managedRunner.getRunnerId();
        updateExposureFromTemporaryOrders(marketId, runnerId, managedRunner);
    }

    private synchronized void updateExposureFromTemporaryOrders(final String marketId, @NotNull final RunnerId runnerId, @NotNull final Exposure exposure) {
        for (final TemporaryOrder temporaryOrder : this.temporaryOrders) {
            if (temporaryOrder == null) {
                logger.error("null temporaryOrder during updateExposureFromTemporaryOrders"); // this should never happen, no need to try to fix it
            } else if (temporaryOrder.runnerEquals(marketId, runnerId)) {
                temporaryOrder.updateExposure(exposure);
            } else { // order not on the runner I'm interested in, nothing to be done
            }
        }
    }

    private synchronized boolean placePriceEqualsTemporaryOrderExists(final String marketIdToCheck, final RunnerId runnerIdToCheck, final Side sideToCheck, final double priceToCheck) {
        boolean orderFound = false;
        for (final TemporaryOrder temporaryOrder : this.temporaryOrders) {
            if (temporaryOrder == null) {
                logger.error("null temporaryOrder during updateExposureFromTemporaryOrders"); // this should never happen, no need to try to fix it
            } else if (temporaryOrder.placePriceEquals(marketIdToCheck, runnerIdToCheck, sideToCheck, priceToCheck)) {
                orderFound = true;
                break;
            } else { // order not what I serch for, nothing to be done
            }
        }
        return orderFound;
    }

    @Contract(pure = true)
    private synchronized boolean hasTemporaryOrders() {
        return !this.temporaryOrders.isEmpty();
    }

    @SuppressWarnings("OverlyNestedMethod")
    synchronized void checkTemporaryOrdersForStreamChange(final OrderMarketRunner orderMarketRunner, final OrderRunnerChange orderRunnerChange) {
        if (this.hasTemporaryOrders()) {
            final String marketId = orderMarketRunner.getMarketId();
            final RunnerId runnerId = orderMarketRunner.getRunnerId();
            if (marketId != null && runnerId != null) {
                @NotNull final Iterator<TemporaryOrder> iterator = this.temporaryOrders.iterator();
                while (iterator.hasNext()) {
                    final TemporaryOrder temporaryOrder = iterator.next();
                    final String temporaryMarketId = temporaryOrder.getMarketId();
                    if (marketId.equals(temporaryMarketId)) {
                        final RunnerId tempRunnerId = temporaryOrder.getRunnerId();
                        if (runnerId.equals(tempRunnerId)) {
                            final TemporaryOrderType orderType = temporaryOrder.getType();
                            final String betId = temporaryOrder.getBetId();
                            final Side side = temporaryOrder.getSide();
                            final double price = temporaryOrder.getPrice();
                            final double size = temporaryOrder.getSize();

                            if (orderType == TemporaryOrderType.PLACE) {
                                if (betId != null) {
                                    final Order foundOrder = orderRunnerChange.getUnmatchedOrder(betId);
                                    if (foundOrder != null) {
                                        removeTempOrder(iterator, temporaryOrder);
                                    } else { // unmatched order with betId not found, looking into matched orders
                                        final double existingSize = orderMarketRunner.getMatchedSizeAtBetterOrEqual(side, price);
                                        final List<List<Double>> newMatchedList = orderRunnerChange.getMatchedList(side);
                                        final double newSize = orderMarketRunner.getMatchedSizeAtBetterOrEqual(side, price, newMatchedList, orderRunnerChange.isImage());
                                        final double sizeModification = newSize - existingSize;
                                        final boolean areEqual = sizeModification > size - .02d;
                                        if (areEqual) {
                                            removeTempOrder(iterator, temporaryOrder);
                                        } else { // matched amount not found or not sufficient, won't remove the temporaryOrder
                                        }
                                    }
                                } else { // placeOrder command hasn't finished and no betId available yet; yet stream only returns once, so I'll use this branch too
                                    final double existingSize = orderMarketRunner.getMatchedSizeAtBetterOrEqual(side, price);
                                    final List<List<Double>> newMatchedList = orderRunnerChange.getMatchedList(side);
                                    final double newSize = orderMarketRunner.getMatchedSizeAtBetterOrEqual(side, price, newMatchedList, orderRunnerChange.isImage());
                                    final double sizeModification = newSize - existingSize;
                                    final boolean areEqual = sizeModification > size - .02d;
                                    if (areEqual) {
                                        removeTempOrder(iterator, temporaryOrder);
                                    } else { // matched amount not found or not sufficient, won't remove the temporaryOrder
                                    }
                                }
                            } else if (orderType == TemporaryOrderType.CANCEL) {
                                if (betId != null) {
                                    final Order foundOrder = orderRunnerChange.getUnmatchedOrder(betId);
                                    final Order previousOrderState = orderMarketRunner.getUnmatchedOrder(betId);
                                    if (foundOrder != null && previousOrderState != null) {
                                        final Double sizeReduction = temporaryOrder.getSizeReduction();
                                        final Double sizeRemaining = foundOrder.getSr();
                                        final Double previousSizeRemaining = previousOrderState.getSr();
                                        if (sizeRemaining == null || sizeRemaining == 0d || previousSizeRemaining == null || previousSizeRemaining == 0d) { // no size remaining, or the size remaining was zero, though the latter might not be normal
                                            removeTempOrder(iterator, temporaryOrder);
                                        } else if (sizeReduction == null) { // the entire order should be canceled, but it's not completely canceled, else it should have entered the previous branch, might be normal, nothing to be done
                                        } else { // a certain amount canceled
                                            final Double sizeCanceled = foundOrder.getSc();
                                            final Double previousSizeCanceled = previousOrderState.getSc();
                                            final double sizeCanceledPrimitive = sizeCanceled == null ? 0 : sizeCanceled;
                                            final double previousSizeCanceledPrimitive = previousSizeCanceled == null ? 0 : previousSizeCanceled;
                                            final double sizeModification = sizeCanceledPrimitive - previousSizeCanceledPrimitive;
//                                            final boolean areEqual = Math.abs(sizeReduction - sizeModification) < .02d;
                                            final boolean areEqual = sizeModification > sizeReduction - .02d;
                                            if (areEqual) {
                                                removeTempOrder(iterator, temporaryOrder);
                                            } else { // canceled amount not found or not sufficient, won't remove the temporaryOrder
                                            }
                                        }
                                    } else { // proper betId not found, nothing to be done
                                    }
                                } else {
                                    logger.error("null betId for CANCEL orderType in reportStreamChange: {}", Generic.objectToString(temporaryOrder));
                                    removeTempOrder(iterator, temporaryOrder);
                                }
                            } else {
                                logger.error("unknown TemporaryOrderType in reportStreamChange: {}", orderType);
                                removeTempOrder(iterator, temporaryOrder);
                            }
                        } else { // not what I look for, nothing to be done
                        }
                    } else { // not what I look for, nothing to be done
                    }
                }
            } else {
                logger.error("null marketId or runnerId in reportStreamChange for: {} {} {} {}", marketId, runnerId, Generic.objectToString(orderMarketRunner), Generic.objectToString(orderRunnerChange));
            }
        } else { // no temporary orders, nothing to check
        }
    }

    private synchronized double checkLimitsAreRespected(final String marketId, final RunnerId runnerId, @NotNull final Side side, final double price, final double size, final double exposureLimit) {
        // very simple limit check using a provided runner limit; it doesn't consider other complicated limits, as that is not at all trivial
        final OrderMarketRunner runner = getOrderMarketRunner(marketId, runnerId);
        final Exposure exposure = new Exposure();
        if (runner == null) { // nothing to update
        } else {
            runner.updateArgumentExposure(exposure);
        }
        updateExposureFromTemporaryOrders(marketId, runnerId, exposure);

        final double existingTotalExposure = side == Side.B ? exposure.getBackTotalExposure() : exposure.getLayTotalExposure();
        final double availableExposure = exposureLimit - existingTotalExposure;
        final double maxSize = Generic.roundDoubleAmount(Formulas.getBetSizeFromExposure(side, price, availableExposure));
        if (maxSize >= size) { // normal, no log message needed
        } else {
            logger.error("limit breached {} vs {} in checkLimitsAreRespected for: {} {} {} {} {}", size, maxSize, marketId, runnerId, side, price, exposureLimit);
        }
        return Math.min(maxSize, size);
    }

    public synchronized double addPlaceOrder(final String marketId, final RunnerId runnerId, final Side side, final double price, final double size, final double exposureLimit, @NotNull final Exposure exposure,
                                             @NotNull final Method sendPostRequestRescriptMethod, @NotNull final BetFrequencyLimit speedLimit, @NotNull final ExistingFunds existingFunds, @NotNull final AtomicBoolean keepAtInPlay,
                                             final boolean isBalancingToRemoveExistingExposure, final String reason) {
        // amounts, in general, will have 2 decimals rounded half down (Generic.roundDoubleAmount)
        final double sizePlaced;
        if (isInitialized()) {
            if (marketId != null && runnerId != null && side != null && price > 0d && size > 0d && Formulas.oddsAreUsable(price)) {
                final double sizeToPlace = Generic.roundDoubleAmount(size);
//                final TemporaryOrder temporaryOrderUsedForDuplicateCheck = new TemporaryOrder(marketId, runnerId, side, price, sizeToPlace, SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get(), reason);
//                if (this.temporaryOrders.contains(temporaryOrderUsedForDuplicateCheck)) { // check is done with sizeToPlace, and not with sizeToPlaceWithinLimits, because sizeToPlaceWithinLimits is calculated considering existing temporaryOrders
//                    // this first if branch might no longer be necessary, as this error is always caught on the second branch; I'll keep it, for now at least, to distinguish between the 2 errors
//                    final TemporaryOrder existingTempOrder = getExistingTemporaryOrderThatEquals(temporaryOrderUsedForDuplicateCheck);
//                    logger.error("will not post duplicate placeOrder existingReason={}: {}", existingTempOrder == null ? "ERROR null Order" : existingTempOrder.getReasonId(), Generic.objectToString(temporaryOrderUsedForDuplicateCheck));
//                    sizePlaced = 0d;
//                } else if (placePriceEqualsTemporaryOrderExists(marketId, runnerId, side, price)) {
//                    final TemporaryOrder existingTempOrder = getExistingTemporaryOrderThatEquals(temporaryOrderUsedForDuplicateCheck);
//                    logger.error("will not post similar placeOrder reason={} existingReason={}: {} {} {} p:{} size:{} limit:{}", reason, existingTempOrder == null ? "ERROR null Order" : existingTempOrder.getReasonId(), marketId, runnerId, side, price,
//                                 size, exposureLimit);
//                    sizePlaced = 0d;
//                } else {
                if (sizeToPlace >= .01d) {
                    if (existingFunds.reserveBreached() && !isBalancingToRemoveExistingExposure) {
                        SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.INFO, "not placing order because reserve breached, available:{} reserve:{} for: {} {} {} {} p:{} size:{} lim:{}", existingFunds.getAvailableFunds(),
                                                                existingFunds.getReserve(), reason, marketId, runnerId, side, price, sizeToPlace, exposureLimit);
                        sizePlaced = 0d;
                    } else if (sizeToPlace >= 2d || Formulas.exposure(side, price, sizeToPlace) >= 2d) {
                        final double sizeToPlaceWithinLimits = checkLimitsAreRespected(marketId, runnerId, side, price, sizeToPlace, exposureLimit);
                        if (sizeToPlaceWithinLimits >= 2d || Formulas.exposure(side, price, sizeToPlaceWithinLimits) >= 2d) {
                            @NotNull final TemporaryOrder temporaryOrder = new TemporaryOrder(marketId, runnerId, side, price, sizeToPlaceWithinLimits, SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get(), reason);
                            addTempOrder(temporaryOrder);
//                                this.temporaryOrders.add(temporaryOrder);
//                                //noinspection NonPrivateFieldAccessedInSynchronizedContext
//                                this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addTempOrder, temporaryOrder));
//                            this.newOrderAdded.set(true);

                            final LimitOrder limitOrder = new LimitOrder();
                            limitOrder.setPersistenceType(keepAtInPlay.get() ? PersistenceType.PERSIST : PersistenceType.LAPSE);
                            limitOrder.setPrice(price);
                            limitOrder.setSize(sizeToPlaceWithinLimits);

                            final PlaceInstruction placeInstruction = new PlaceInstruction();
                            placeInstruction.setHandicap(runnerId.getHandicap());
                            placeInstruction.setOrderType(OrderType.LIMIT);
                            placeInstruction.setSelectionId(runnerId.getSelectionId());
                            placeInstruction.setSide(side.toStandardSide());
                            placeInstruction.setLimitOrder(limitOrder);

                            final List<PlaceInstruction> placeInstructionsList = new ArrayList<>(1);
                            placeInstructionsList.add(placeInstruction);

                            SharedStatics.threadPoolExecutorImportant.execute(new PlaceOrdersThread(marketId, placeInstructionsList, temporaryOrder, sendPostRequestRescriptMethod, speedLimit));
                            sizePlaced = sizeToPlaceWithinLimits;
                        } else { // size too small to place order
                            logger.info("too small value {} for sizeToPlaceWithinLimits in addPlaceOrder for: sizeToPlace:{} {} {} {} p:{} size:{} {}", sizeToPlaceWithinLimits, sizeToPlace, marketId, runnerId, side, price, size, reason);
                            sizePlaced = 0d;
                        }
                    } else { // size too small to place order
                        logger.info("too small but not zero value {} for sizeToPlace in addPlaceOrder for: {} {} {} p:{} size:{} {}", sizeToPlace, marketId, runnerId, side, price, size, reason);
                        sizePlaced = 0d;
                    }
                } else if (sizeToPlace == 0d) {
                    sizePlaced = 0d;
                } else {
                    logger.error("bogus too small but not zero value {} for sizeToPlace in addPlaceOrder for: {} {} {} p:{} size:{} {}", sizeToPlace, marketId, runnerId, side, price, size, reason);
                    sizePlaced = 0d;
                }
//                }
            } else {
                logger.error("bogus arguments in addPlaceOrder: {} {} {} {} {} {}", marketId, runnerId, side, price, size, reason);
                sizePlaced = 0d;
            }
        } else {
            logger.error("orderCache not initialized in addPlaceOrder: {} {} {} {} {} {}", marketId, runnerId, side, price, size, reason);
            sizePlaced = 0d;
        }
        return sizePlaced;
    }

    public synchronized double addCancelOrder(final String marketId, final RunnerId runnerId, final Side side, final double price, final double size, final double sizeMinusSizeTempCanceled, final String betId, final Double sizeReduction,
                                              final AtomicDouble removedExposureDuringThisManageIteration, @NotNull final Exposure exposure, @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        // runnerId, side, price, size are needed to identify the order in the stream, or not, but they might have some use; size in this case is sizeRemaining
        final double exposureCanceled;
        if (marketId != null && betId != null && (sizeReduction == null || Generic.roundDoubleAmount(sizeReduction) > 0d)) {
            @NotNull final TemporaryOrder temporaryOrder = new TemporaryOrder(marketId, runnerId, side, price, size, betId, sizeReduction, SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get(), reason);
//            if (this.temporaryOrders.contains(temporaryOrder)) {
//                final TemporaryOrder existingTempOrder = getExistingTemporaryOrderThatEquals(temporaryOrder);
//                logger.info("will not post duplicate cancelOrder existingReason={}: {}", existingTempOrder == null ? "ERROR null Order" : existingTempOrder.getReasonId(), Generic.objectToString(temporaryOrder));
//                exposureCanceled = 0d;
//            } else {
            addTempOrder(temporaryOrder);
//                this.temporaryOrders.add(temporaryOrder);
//                //noinspection NonPrivateFieldAccessedInSynchronizedContext
//                this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addTempOrder, temporaryOrder));
//                this.newOrderAdded.set(true);

            final CancelInstruction cancelInstruction = new CancelInstruction();
            cancelInstruction.setBetId(betId);
            cancelInstruction.setSizeReduction(sizeReduction);

            final List<CancelInstruction> cancelInstructionsList = new ArrayList<>(1);
            cancelInstructionsList.add(cancelInstruction);

            SharedStatics.threadPoolExecutorImportant.execute(new CancelOrdersThread(marketId, cancelInstructionsList, temporaryOrder, sendPostRequestRescriptMethod));
            exposureCanceled = Formulas.exposure(side, price, sizeReduction == null ? sizeMinusSizeTempCanceled : Math.min(Generic.roundDoubleAmount(sizeReduction), sizeMinusSizeTempCanceled));
//            }
        } else {
            logger.error("bogus arguments in addCancelOrder: {} {} {} {}", marketId, betId, sizeReduction, reason);
            exposureCanceled = 0d;
        }
        Generic.addToAtomicDouble(removedExposureDuringThisManageIteration, exposureCanceled);
        return exposureCanceled;
    }

    @Nullable
    private synchronized TemporaryOrder getExistingTemporaryOrderThatEquals(@NotNull final TemporaryOrder temporaryOrder) {
        TemporaryOrder existingTempOrder = null;
        for (final TemporaryOrder existingOrder : this.temporaryOrders) {
            if (temporaryOrder.equals(existingOrder)) {
                existingTempOrder = existingOrder;
                break;
            } else { // noth the order I want, I'll keep looking
            }
        }
        return existingTempOrder;
    }

    @SuppressWarnings("OverlyNestedMethod")
    public synchronized void addTemporaryAmountsToOwnAmounts(final String marketId, final RunnerId runnerId, @NotNull final Side side, @NotNull final TreeMap<Double, Double> tree) {
        for (final TemporaryOrder temporaryOrder : this.temporaryOrders) {
            if (temporaryOrder == null) { // should never happen; if it does happen, it gets removed in checkForExpiredOrders()
                logger.error("null temporaryOrder in addTemporaryAmountsToOwnAmounts");
            } else {
                final String orderMarketId = temporaryOrder.getMarketId();
                if (Objects.equals(marketId, orderMarketId)) {
                    final RunnerId orderRunnerId = temporaryOrder.getRunnerId();
                    if (Objects.equals(runnerId, orderRunnerId)) {
                        final Side orderSide = temporaryOrder.getSide();
                        if (side == orderSide) {
                            @NotNull final TemporaryOrderType type = temporaryOrder.getType();
                            final double price = temporaryOrder.getPrice(), size = temporaryOrder.getSize();
                            final Double sizeInTree = tree.get(price); // can easily be null if element not found
                            final double primitiveSizeInTree = sizeInTree == null ? 0d : sizeInTree;
                            if (type == TemporaryOrderType.PLACE) {
                                tree.put(price, primitiveSizeInTree + size);
                            } else if (type == TemporaryOrderType.CANCEL) {
                                @Nullable final Double sizeReduction = temporaryOrder.getSizeReduction();
                                final double sizeReductionPrimitive = sizeReduction == null ? size : sizeReduction;
                                final double remainingSize = primitiveSizeInTree - sizeReductionPrimitive;
                                if (remainingSize >= 0.01d) {
                                    tree.replace(price, remainingSize);
                                } else {
                                    if (remainingSize <= -0.01d) {
                                        logger.error("negative remainingSize {} in addTemporaryAmountsToOwnAmounts for: {} {} {} {} {} {} {} {}", remainingSize, primitiveSizeInTree, sizeReductionPrimitive, marketId, runnerId, side,
                                                     price, Generic.objectToString(temporaryOrder), Generic.objectToString(tree));
                                    } else { // no error, nothing to print
                                    }
                                    tree.remove(price);
                                }
                            } else {
                                logger.error("unknown TemporaryOrderType {} in addTemporaryAmountsToOwnAmounts for: {}", type, Generic.objectToString(temporaryOrder));
                            }
                        } else { // not the side I look for, nothing to be done
                        }
                    } else { // not the runner I look for, nothing to be done
                    }
                } else { // not the market I look for, nothing to be done
                }
            }
        }
    }

    public synchronized long checkForExpiredOrders() {
        long timeToSleep = 10L * Generic.MINUTE_LENGTH_MILLISECONDS; // initialized
        if (this.temporaryOrders.isEmpty()) { // no orders, timeToSleep is already initialized, nothing to do
        } else {
            final long currentTime = System.currentTimeMillis();
            final Iterator<TemporaryOrder> iterator = this.temporaryOrders.iterator();
            while (iterator.hasNext()) {
                final TemporaryOrder temporaryOrder = iterator.next();
                if (temporaryOrder == null) {
                    logger.error("null temporaryOrder in checkForExpiredOrders");
                    removeTempOrder(iterator, temporaryOrder);
                } else {
                    final long expirationTime = temporaryOrder.getExpirationTime();
                    final long timeTillExpired = expirationTime - currentTime;

                    if (timeTillExpired <= 0) {
                        logger.error("removing expired by {}ms temporaryOrder, {}s after creation: {}", -timeTillExpired, Generic.millisecondsToSecondsString(currentTime - temporaryOrder.getCreationTime()), Generic.objectToString(temporaryOrder));
                        removeTempOrder(iterator, temporaryOrder);
                    } else {
                        timeToSleep = Math.min(timeToSleep, timeTillExpired);
//                        if (temporaryOrder.isTooOld(currentTime)) {
//                            final LogLevel logLevel = SharedStatics.denyBetting.get() || SharedStatics.notPlacingOrders ? LogLevel.INFO : LogLevel.ERROR;
//                            SharedStatics.alreadyPrintedMap.logOnce(5L * Generic.MINUTE_LENGTH_MILLISECONDS, logger, logLevel, "temporaryOrder too old: {}ms for: {}",
//                                                                    Generic.addCommas(currentTime - temporaryOrder.getCreationTime()), Generic.objectToString(temporaryOrder));
//                        } else { // not too old, nothing to do
//                        }
                    }
                }
            } // end while
        }

//        this.newOrderAdded.set(false);
        return timeToSleep;
    }

    @NotNull
    private synchronized OrderMarket addNewMarket(final String marketId, @NotNull final AtomicBoolean newOrderMarketCreated, @NotNull final OrderMarketChange orderMarketChange) {
        final OrderMarket orderMarket = new OrderMarket(marketId);
        orderMarket.onOrderMarketChange(orderMarketChange);
        this.markets.put(marketId, orderMarket, true);
        newOrderMarketCreated.set(true);

        return orderMarket;
    }

    public boolean isInitialized() { // no need to synchronize, it only accesses an atomicLong
        return this.initializedStamp.get() > 0L;
    }

    public synchronized boolean isOrderMarketRemovedOnClose() {
        return this.orderMarketRemovedOnClose;
    }

//    public synchronized void setOrderMarketRemovedOnClose(final boolean orderMarketRemovedOnClose) {
//        this.orderMarketRemovedOnClose = orderMarketRemovedOnClose;
//    }

    public void printOrderMarkets() { // not synchronized, used for debugging
        for (final OrderMarket orderMarket : this.markets.valuesCopy()) {
            logger.info("listing orderMarket: {}", Generic.objectToString(orderMarket));
        }
    }

    public synchronized HashSet<String> getOrderMarketKeys() {
        return new HashSet<>(this.markets.keySetCopy());
    }

    public synchronized int getNOrderMarkets() {
        return this.markets.size();
    }

    @Nullable
    private synchronized OrderMarket getOrderMarket(final String marketId) {
        return this.markets.get(marketId);
    }

    @Nullable
    private synchronized OrderMarket getOrderMarket(@NotNull final ManagedRunner managedRunner) {
        return this.markets.get(managedRunner.getMarketId());
    }

    @Nullable
    private synchronized OrderMarketRunner getOrderMarketRunner(final String marketId, final RunnerId runnerId) {
        final OrderMarket orderMarket = this.getOrderMarket(marketId);
        return orderMarket == null ? null : orderMarket.getOrderMarketRunner(runnerId);
    }

    @Nullable
    private synchronized OrderMarketRunner getOrderMarketRunner(@NotNull final ManagedRunner managedRunner) {
        final OrderMarket orderMarket = this.getOrderMarket(managedRunner);
        return orderMarket == null ? null : orderMarket.getOrderMarketRunner(managedRunner.getRunnerId());
    }

    @Nullable
    private synchronized Order getUnmatchedOrder(final String marketId, final RunnerId runnerId, final String betId) {
        final OrderMarket orderMarket = this.getOrderMarket(marketId);
        return orderMarket == null ? null : orderMarket.getUnmatchedOrder(runnerId, betId);
    }
}
