package info.fmro.shared.stream.cache.order;

import info.fmro.shared.stream.definitions.Order;
import info.fmro.shared.stream.definitions.OrderMarketChange;
import info.fmro.shared.stream.definitions.OrderRunnerChange;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

class OrderMarket
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(OrderMarket.class);
    @Serial
    private static final long serialVersionUID = 6849187708144779801L;
    private final String marketId;
    @NotNull
    private final Map<RunnerId, OrderMarketRunner> marketRunners = new HashMap<>(4); // only place where orderMarketRunners are stored
    private boolean isClosed;
//    private boolean obsoleteObject;

    OrderMarket(final String marketId) {
        this.marketId = marketId;
        logger.info("newOrderMarketCreated: {}", marketId);
    }

//    OrderMarket(final String marketId, @NotNull final AtomicBoolean newOrderMarketCreated) {
//        this.marketId = marketId;
//        newOrderMarketCreated.set(true);
//        logger.info("newOrderMarketCreated: {}", marketId);
//    }

    synchronized void onOrderMarketChange(@NotNull final OrderMarketChange orderMarketChange) {
        // update runners
        if (orderMarketChange.getOrc() != null) {
            for (final OrderRunnerChange orderRunnerChange : orderMarketChange.getOrc()) {
                onOrderRunnerChange(orderRunnerChange);
            }
        }

        this.isClosed = Boolean.TRUE.equals(orderMarketChange.getClosed());
    }

    private synchronized void onOrderRunnerChange(@NotNull final OrderRunnerChange orderRunnerChange) {
        final RunnerId runnerId = new RunnerId(orderRunnerChange.getId(), orderRunnerChange.getHc());
        final OrderMarketRunner orderMarketRunner = this.marketRunners.computeIfAbsent(runnerId, r -> new OrderMarketRunner(getMarketId(), r));

        // update the runner
        orderMarketRunner.onOrderRunnerChange(orderRunnerChange);
        if (orderMarketRunner.isEmpty()) {
            this.marketRunners.remove(runnerId);
            logger.info("removing empty OrderMarketRunner: {} {}", this.marketId, runnerId);
        } else { // no empty, won't remove
        }
    }

    synchronized int cancelUnmatchedAtWorseOdds(final Side sideToCancel, final double worstNotCanceledOdds, final double excessExposure, @NotNull final Method sendPostRequestRescriptMethod, final boolean includeTheProvidedOdds, final String reason) {
        int modifications = 0;
        for (final OrderMarketRunner orderMarketRunner : this.marketRunners.values()) {
            if (orderMarketRunner == null) {
                logger.error("null orderMarketRunner in cancelUnmatchedAtWorseOdds for: {}", Generic.objectToString(this)); // should never happen, no need to try to fix
            } else {
                modifications += orderMarketRunner.cancelUnmatchedAtWorseOdds(sideToCancel, worstNotCanceledOdds, excessExposure, sendPostRequestRescriptMethod, includeTheProvidedOdds, null, reason);
            }
        }
        return modifications;
    }

//    synchronized boolean isObsoleteObject() {
//        return this.obsoleteObject;
//    }
//
//    synchronized void markObsoleteObject() {
//        this.obsoleteObject = true;
//        for (final OrderMarketRunner orderMarketRunner : this.marketRunners.values()) {
//            if (orderMarketRunner == null) {
//                logger.error("null orderMarketRunner during markObsoleteObject for: {}", Generic.objectToString(this));
//            } else {
//                orderMarketRunner.markObsoleteObject();
//            }
//        }
//    }

//    @NotNull
//    synchronized Exposure getExposure(@NotNull final RunnerId runnerId) {
//        @NotNull final Exposure returnValue;
//        final OrderMarketRunner orderMarketRunner = this.marketRunners.get(runnerId);
//        if (orderMarketRunner == null) {
//            returnValue = new Exposure();
//        } else {
//            returnValue = orderMarketRunner.exposureCopy();
//        }
//        return returnValue;
//    }

//    synchronized int cancelUnmatchedAmounts(@NotNull final RunnerId runnerId, final double backExcessExposure, final double layExcessExposure, @NotNull final Method sendPostRequestRescriptMethod) {
//        int exposureHasBeenModified = 0;
//        final OrderMarketRunner orderMarketRunner = this.marketRunners.get(runnerId);
//        if (orderMarketRunner == null) { // nothing to be done
//        } else {
//            exposureHasBeenModified += orderMarketRunner.cancelUnmatchedAmounts(backExcessExposure, layExcessExposure, sendPostRequestRescriptMethod);
//        }
//        return exposureHasBeenModified;
//    }
//
//    synchronized void updateExposure(@NotNull final RunnerId runnerId, @NotNull final Exposure exposure) {
//        final OrderMarketRunner orderMarketRunner = this.marketRunners.get(runnerId);
//        if (orderMarketRunner == null) { // no update
//        } else {
//            orderMarketRunner.updateArgumentExposure(exposure);
//        }
//    }

    synchronized boolean isEmpty() {
        return this.marketRunners.isEmpty();
    }

    @SuppressWarnings({"SuspiciousGetterSetter", "WeakerAccess", "RedundantSuppression"})
    synchronized boolean isClosed() {
        return this.isClosed;
    }

    synchronized String getMarketId() {
        return this.marketId;
    }

    synchronized HashSet<RunnerId> getRunnerIds() {
        return new HashSet<>(this.marketRunners.keySet());
    }

    synchronized ArrayList<OrderMarketRunner> getOrderMarketRunners() {
        return new ArrayList<>(this.marketRunners.values());
    }

    synchronized OrderMarketRunner getOrderMarketRunner(final RunnerId runnerId) {
        return this.marketRunners.get(runnerId);
    }

    @Nullable
    synchronized Order getUnmatchedOrder(final RunnerId runnerId, final String betId) {
        final OrderMarketRunner orderMarketRunner = getOrderMarketRunner(runnerId);
        return orderMarketRunner == null ? null : orderMarketRunner.getUnmatchedOrder(betId);
    }
}
