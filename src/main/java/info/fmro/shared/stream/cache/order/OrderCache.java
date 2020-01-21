package info.fmro.shared.stream.cache.order;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.stream.definitions.OrderMarketChange;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.stream.protocol.ChangeMessage;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class OrderCache
        implements Serializable, StreamObjectInterface {
    private static final long serialVersionUID = -6023803756520072425L;
    public transient ListOfQueues listOfQueues = new ListOfQueues();
    private final Map<String, OrderMarket> markets = new ConcurrentHashMap<>(4); // only place where orderMarkets are permanently stored
    @SuppressWarnings("FieldMayBeFinal")
    private boolean orderMarketRemovedOnClose = true; // default

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();
    }

    public synchronized OrderCache getCopy() {
        return SerializationUtils.clone(this);
    }

    public synchronized void onOrderChange(@NotNull final ChangeMessage<? extends OrderMarketChange> changeMessage, @NotNull final AtomicBoolean orderCacheHasReset, @NotNull final AtomicBoolean newOrderMarketCreated,
                                           @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate) {
        if (changeMessage.isStartOfNewSubscription()) {
            this.markets.clear();
            orderCacheHasReset.set(true);
        }

        if (changeMessage.getItems() != null) {
            for (final OrderMarketChange change : changeMessage.getItems()) {
                final OrderMarket orderMarket = onOrderMarketChange(change, newOrderMarketCreated, pendingOrdersThread, currencyRate);

                if (this.orderMarketRemovedOnClose && orderMarket.isClosed()) {
                    // remove on close
                    this.markets.remove(orderMarket.getMarketId());
                }
            } // end for
        }
    }

    @NotNull
    private synchronized OrderMarket onOrderMarketChange(@NotNull final OrderMarketChange orderMarketChange, @NotNull final AtomicBoolean newOrderMarketCreated, @NotNull final OrdersThreadInterface pendingOrdersThread,
                                                         @NotNull final AtomicDouble currencyRate) {
        final String marketId = orderMarketChange.getId();
        final OrderMarket orderMarket = this.markets.computeIfAbsent(marketId, k -> new OrderMarket(marketId, newOrderMarketCreated));

        orderMarket.onOrderMarketChange(orderMarketChange, pendingOrdersThread, currencyRate);
        return orderMarket;
    }

    public synchronized boolean isOrderMarketRemovedOnClose() {
        return this.orderMarketRemovedOnClose;
    }

//    public synchronized void setOrderMarketRemovedOnClose(final boolean orderMarketRemovedOnClose) {
//        this.orderMarketRemovedOnClose = orderMarketRemovedOnClose;
//    }

    public synchronized Iterable<OrderMarket> getOrderMarkets() {
        return new ArrayList<>(this.markets.values());
    }

    public synchronized HashSet<String> getOrderMarketKeys() {
        return new HashSet<>(this.markets.keySet());
    }

    public synchronized int getNOrderMarkets() {
        return this.markets.size();
    }

    public synchronized OrderMarket getOrderMarket(final String marketId) {
        return this.markets.get(marketId);
    }

    @Nullable
    public synchronized OrderMarketRunner getOrderMarketRunner(final String marketId, final RunnerId runnerId) {
        final OrderMarket orderMarket = this.getOrderMarket(marketId);
        return orderMarket == null ? null : orderMarket.getOrderMarketRunner(runnerId);
    }
}
