package info.fmro.shared.stream.cache.order;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.stream.definitions.OrderMarketChange;
import info.fmro.shared.stream.definitions.OrderRunnerChange;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class OrderMarket
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(OrderMarket.class);
    private static final long serialVersionUID = 6849187708144779801L;
    private final String marketId;
    private final @NotNull Map<RunnerId, OrderMarketRunner> marketRunners = new ConcurrentHashMap<>(4); // only place where orderMarketRunners are stored
    private boolean isClosed;

    OrderMarket(final String marketId, @NotNull final AtomicBoolean newOrderMarketCreated) {
        this.marketId = marketId;
        newOrderMarketCreated.set(true);
        logger.info("newOrderMarketCreated: {}", marketId);
    }

    synchronized void onOrderMarketChange(@NotNull final OrderMarketChange orderMarketChange, final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate) {
        // update runners
        if (orderMarketChange.getOrc() != null) {
            for (final OrderRunnerChange orderRunnerChange : orderMarketChange.getOrc()) {
                onOrderRunnerChange(orderRunnerChange, pendingOrdersThread, currencyRate);
            }
        }

        this.isClosed = Boolean.TRUE.equals(orderMarketChange.getClosed());
    }

    private synchronized void onOrderRunnerChange(@NotNull final OrderRunnerChange orderRunnerChange, final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate) {
        final RunnerId runnerId = new RunnerId(orderRunnerChange.getId(), orderRunnerChange.getHc());
        final OrderMarketRunner orderMarketRunner = this.marketRunners.computeIfAbsent(runnerId, r -> new OrderMarketRunner(getMarketId(), r));

        // update the runner
        orderMarketRunner.onOrderRunnerChange(orderRunnerChange, pendingOrdersThread, currencyRate);
        if (orderMarketRunner.isEmpty()) {
            this.marketRunners.remove(runnerId);
            logger.info("removing empty OrderMarketRunner: {} {}", this.marketId, Generic.objectToString(runnerId));
        } else { // no empty, won't remove
        }
    }

    public synchronized boolean isEmpty() {
        return this.marketRunners.isEmpty();
    }

    @SuppressWarnings({"SuspiciousGetterSetter", "WeakerAccess", "RedundantSuppression"})
    public synchronized boolean isClosed() {
        return this.isClosed;
    }

    public synchronized String getMarketId() {
        return this.marketId;
    }

    public synchronized HashSet<RunnerId> getRunnerIds() {
        return new HashSet<>(this.marketRunners.keySet());
    }

    public synchronized ArrayList<OrderMarketRunner> getOrderMarketRunners() {
        return new ArrayList<>(this.marketRunners.values());
    }

    public synchronized OrderMarketRunner getOrderMarketRunner(final RunnerId runnerId) {
        return this.marketRunners.get(runnerId);
    }
}
