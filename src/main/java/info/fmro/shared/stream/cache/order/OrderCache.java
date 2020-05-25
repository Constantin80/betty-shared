package info.fmro.shared.stream.cache.order;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.logic.ManagedEventsMap;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.MarketsToCheckQueue;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.definitions.OrderMarketChange;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.stream.protocol.ChangeMessage;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class OrderCache
        implements Serializable, StreamObjectInterface {
    private static final Logger logger = LoggerFactory.getLogger(OrderCache.class);
    private static final long serialVersionUID = -6023803756520072425L;
    public transient ListOfQueues listOfQueues = new ListOfQueues();
    public final SynchronizedMap<String, OrderMarket> markets = new SynchronizedMap<>(4); // only place where orderMarkets are permanently stored
    private boolean orderMarketRemovedOnClose = true; // default
    public transient AtomicLong initializedFromStreamStamp = new AtomicLong();

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();
        this.initializedFromStreamStamp = new AtomicLong();
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

    public synchronized void onOrderChange(@NotNull final ChangeMessage<? extends OrderMarketChange> changeMessage, @NotNull final AtomicBoolean orderCacheHasReset, @NotNull final AtomicBoolean newOrderMarketCreated,
                                           final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate, @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache,
                                           @NotNull final ListOfQueues rulesManagerListOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final ManagedEventsMap events,
                                           @NotNull final SynchronizedMap<String, ManagedMarket> managedMarkets, @NotNull final AtomicBoolean rulesHaveChanged,
                                           @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        if (changeMessage.isStartOfNewSubscription()) {
            this.markets.clear();
            orderCacheHasReset.set(true);
            //noinspection NonPrivateFieldAccessedInSynchronizedContext
            this.initializedFromStreamStamp.set(System.currentTimeMillis());
        }

        if (changeMessage.getItems() != null) {
            for (final OrderMarketChange change : changeMessage.getItems()) {
                final OrderMarket orderMarket = onOrderMarketChange(change, newOrderMarketCreated, pendingOrdersThread, currencyRate, marketCache, rulesManagerListOfQueues, marketsToCheck, events, managedMarkets, rulesHaveChanged, marketCataloguesMap,
                                                                    programStartTime);

                if (orderMarket.isEmpty() || (this.orderMarketRemovedOnClose && orderMarket.isClosed())) {
                    // remove on close or if empty
                    this.markets.remove(orderMarket.getMarketId());
                }
            } // end for
        }
    }

    @NotNull
    private synchronized OrderMarket onOrderMarketChange(@NotNull final OrderMarketChange orderMarketChange, @NotNull final AtomicBoolean newOrderMarketCreated, final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate,
                                                         @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final ListOfQueues rulesManagerListOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                                         @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> managedMarkets, @NotNull final AtomicBoolean rulesHaveChanged,
                                                         @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        final String marketId = orderMarketChange.getId();
        final boolean newMarketAdded = !this.markets.containsKey(marketId);
        final OrderMarket orderMarket = this.markets.computeIfAbsent(marketId, k -> new OrderMarket(marketId, newOrderMarketCreated));
        orderMarket.onOrderMarketChange(orderMarketChange, pendingOrdersThread, currencyRate);

        if (newMarketAdded) {
            final ManagedMarket managedMarket = managedMarkets.get(marketId);
            if (managedMarket == null) { // no managedMarket present, nothing to be done
            } else {
                managedMarket.attachOrderMarket(this.markets, marketCache, rulesManagerListOfQueues, marketsToCheck, events, managedMarkets, rulesHaveChanged, marketCataloguesMap, programStartTime);
            }
        } else { // market was already present, nothing to be done
        }
        return orderMarket;
    }

    public synchronized boolean isOrderMarketRemovedOnClose() {
        return this.orderMarketRemovedOnClose;
    }

//    public synchronized void setOrderMarketRemovedOnClose(final boolean orderMarketRemovedOnClose) {
//        this.orderMarketRemovedOnClose = orderMarketRemovedOnClose;
//    }

    public synchronized Iterable<OrderMarket> getOrderMarkets() {
        return new ArrayList<>(this.markets.valuesCopy());
    }

    public synchronized HashSet<String> getOrderMarketKeys() {
        return new HashSet<>(this.markets.keySetCopy());
    }

    public synchronized int getNOrderMarkets() {
        return this.markets.size();
    }

    private synchronized OrderMarket getOrderMarket(final String marketId) {
        return this.markets.get(marketId);
    }

    @Nullable
    public synchronized OrderMarketRunner getOrderMarketRunner(final String marketId, final RunnerId runnerId) {
        final OrderMarket orderMarket = this.getOrderMarket(marketId);
        return orderMarket == null ? null : orderMarket.getOrderMarketRunner(runnerId);
    }
}
