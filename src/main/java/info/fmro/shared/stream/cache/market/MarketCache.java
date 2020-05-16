package info.fmro.shared.stream.cache.market;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.logic.ManagedEventsMap;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.MarketsToCheckQueue;
import info.fmro.shared.stream.definitions.MarketChange;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.stream.protocol.ChangeMessage;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

public class MarketCache
        implements Serializable, StreamObjectInterface {
    private static final Logger logger = LoggerFactory.getLogger(MarketCache.class);
    private static final long serialVersionUID = -6721530926161875702L;
    public transient ListOfQueues listOfQueues = new ListOfQueues();
    public final SynchronizedMap<String, Market> markets = new SynchronizedMap<>(32); // only place where markets are permanently stored
    private boolean isMarketRemovedOnClose = true; // default

    //conflation indicates slow consumption
    private int conflatedCount;

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();
    }

    public synchronized MarketCache getCopy() {
        return SerializationUtils.clone(this);
    }

    @SuppressWarnings("NonPrivateFieldAccessedInSynchronizedContext")
    public synchronized boolean copyFromStream(final MarketCache other) {
        final boolean readSuccessful;
        if (other == null) {
            logger.error("null other in copyFromStream for: {}", Generic.objectToString(this));
            readSuccessful = false;
        } else {
//            Generic.updateObject(this, other);

            this.markets.clear();
            this.markets.putAll(other.markets.copy());
            this.isMarketRemovedOnClose = other.isMarketRemovedOnClose;
            this.conflatedCount = other.conflatedCount;

            readSuccessful = true;
        }

        final int nQueues = this.listOfQueues.size();
        if (nQueues == 0) { // normal case, nothing to be done
        } else {
            logger.error("existing queues during MarketCache.copyFromStream: {} {}", nQueues, Generic.objectToString(this));
            this.listOfQueues.clear();
        }
        return readSuccessful;
    }

    public synchronized void onMarketChange(@NotNull final ChangeMessage<? extends MarketChange> changeMessage, @NotNull final AtomicDouble currencyRate, @NotNull final ListOfQueues rulesManagerListOfQueues,
                                            @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> managedMarkets,
                                            @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        if (changeMessage.isStartOfNewSubscription()) {
            // was it right to disable markets.clear() in isStartOfNewSubscription ?; maybe, it seems markets are properly updated, although some old no longer used markets are probably not removed, I'll see more with testing
            //clear cache ... no clear anymore, because of multiple clients
//            markets.clear();
        }
        if (changeMessage.getItems() != null) {
            for (final MarketChange marketChange : changeMessage.getItems()) {
                final Market market = onMarketChange(marketChange, currencyRate, rulesManagerListOfQueues, marketsToCheck, events, managedMarkets, rulesHaveChanged, marketCataloguesMap, programStartTime);

                if (this.isMarketRemovedOnClose && market.isClosed()) {
                    //remove on close
                    this.markets.remove(market.getMarketId());
                }
                // dispatchMarketChanged(market, marketChange);
            } // end for
        }
    }

    @NotNull
    private synchronized Market onMarketChange(@NotNull final MarketChange marketChange, @NotNull final AtomicDouble currencyRate, @NotNull final ListOfQueues rulesManagerListOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                               @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> managedMarkets, @NotNull final AtomicBoolean rulesHaveChanged,
                                               @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        if (Boolean.TRUE.equals(marketChange.getCon())) {
            this.conflatedCount++;
        }
        final String marketId = marketChange.getId();
        final boolean newMarketAdded = !this.markets.containsKey(marketId);
        //        final Market market;
//        if (this.markets.containsKey(marketId)) {
//            market = this.markets.get(marketId);
//        } else {
//            market = new Market(marketId, marketsToCheck);
//            this.markets.put(marketId, market);
//        }
        final Market market = this.markets.computeIfAbsent(marketId, k -> new Market(marketId, marketsToCheck));
        market.onMarketChange(marketChange, currencyRate);
        if (newMarketAdded) {
            final ManagedMarket managedMarket = managedMarkets.get(marketId);
            if (managedMarket == null) { // no managedMarket present, nothing to be done
            } else {
                managedMarket.attachMarket(this.markets, rulesManagerListOfQueues, marketsToCheck, events, managedMarkets, rulesHaveChanged, marketCataloguesMap, programStartTime, true);
            }
        } else { // market was already present, nothing to be done
        }
        return market;
    }

    public synchronized int getConflatedCount() {
        return this.conflatedCount;
    }

//    public synchronized void setConflatedCount(final int conflatedCount) {
//        this.conflatedCount = conflatedCount;
//    }

    @SuppressWarnings("SuspiciousGetterSetter")
    public synchronized boolean isMarketRemovedOnClose() {
        return this.isMarketRemovedOnClose;
    }

//    @SuppressWarnings("SuspiciousGetterSetter")
//    public synchronized void setMarketRemovedOnClose(final boolean marketRemovedOnClose) {
//        this.isMarketRemovedOnClose = marketRemovedOnClose;
//    }

    public synchronized Market getMarket(final String marketId) {
        //queries by market id - the result is invariant for the lifetime of the market.
        return this.markets.get(marketId);
    }

    public synchronized Iterable<Market> getMarkets() {
        //all the cached markets
        return this.markets.valuesCopy();
    }

    public synchronized int getMarketCount() {
        //market count
        return this.markets.size();
    }

    public synchronized boolean isEmpty() {
        return this.markets.isEmpty();
    }

    public synchronized boolean contains(final String marketId) {
        return this.markets.containsKey(marketId);
    }

    public synchronized boolean contains(@NotNull final ManagedMarket managedMarket) {
        final String marketId = managedMarket.getMarketId();
        return this.markets.containsKey(marketId);
    }
//    public synchronized long getTimeClean() {
//        return timeClean;
//    }
//
//    public synchronized void setTimeClean(long timeClean) {
//        this.timeClean = timeClean;
//    }
//
//    public synchronized void timeCleanStamp() {
//        this.timeClean = System.currentTimeMillis();
//    }
//
//    public synchronized void timeCleanAdd(long addedTime) {
//        final long currentTime = System.currentTimeMillis();
//        if (currentTime - this.timeClean >= addedTime) {
//            this.timeClean = currentTime + addedTime;
//        } else {
//            this.timeClean += addedTime;
//        }
//    }

//    public synchronized void maintenanceClean() {
//        this.timeCleanAdd(Generic.MINUTE_LENGTH_MILLISECONDS * 30L);
//        // Maintenance method for removing old no longer used markets from marketCache
//        // I don't have timeStamps on the markets, so I can't do maintenance properly
//        // 2 possible solutions:
//        // 1: add proper stamps, with each onMarketChange, but some markets with no activity won't get stamped, except in the beginning
//        // 2: no cache persistence on the disk or cache picking up where it left, which means I don't need maintenance; management of markets would only start after the caches are updated by the stream
//    }

    // Event for each market change
//    private synchronized void dispatchMarketChanged(Market market, MarketChange marketChange) {
//        final MarketChangeEvent marketChangeEvent = new MarketChangeEvent(this);
//        marketChangeEvent.setMarket(market);
//        marketChangeEvent.setChange(marketChange);
//    }
}
