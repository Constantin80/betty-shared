package info.fmro.shared.logic;

import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.stream.cache.Utils;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.order.OrderCache;
import info.fmro.shared.stream.cache.order.OrderMarket;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.SynchronizedMap;
import info.fmro.shared.utility.SynchronizedSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("WeakerAccess")
public class ManagedEvent
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedEvent.class);
    private static final long serialVersionUID = 9206333179442623395L;
    private final String id;
    private String eventName;
    private double amountLimit = -1d;
    public final SynchronizedSet<String> marketIds = new SynchronizedSet<>(); // managedMarket ids associated with this event
    public transient ManagedMarketsMap marketsMap; // managedMarkets associated with this event
    @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
    private transient Event event;

    public ManagedEvent(@NotNull final String id, @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final ListOfQueues listOfQueues) {
        this.id = id;
        //noinspection ThisEscapedInObjectConstruction
        this.marketsMap = new ManagedMarketsMap(this, markets);
        attachEvent(eventsMap, listOfQueues);
    }

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.marketsMap = new ManagedMarketsMap(this);
    }

//    private synchronized HashMap<String, ManagedMarket> getMarketsMap() {
//        if (!isMarketsMapInitialized()) {
//            initializeMarketsMap();
//        } else { // already initialized, nothing to do
//        }
//
//        return new HashMap<>(marketsMap);
//    }

    @SuppressWarnings("UnusedReturnValue")
    synchronized boolean setAmountLimit(final double newAmountLimit, @NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                        @NotNull final ManagedEventsMap events, @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck, @NotNull final AtomicBoolean marketsMapModified,
                                        @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck, final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, @NotNull final ExistingFunds safetyLimits,
                                        final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, final long programStartTime) {
        final boolean modified;
        if (Double.isNaN(newAmountLimit)) {
            modified = false;
        } else  //noinspection FloatingPointEquality
            if (this.amountLimit == newAmountLimit) {
                modified = false;
            } else {
                this.amountLimit = newAmountLimit;
                modified = true;
            }

        if (modified) {
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setEventAmountLimit, this.id, this.amountLimit));
            if (pendingOrdersThread != null && marketCataloguesMap != null) {
                calculateMarketLimits(markets, listOfQueues, marketsToCheck, events, rulesHaveChanged, marketsForOutsideCheck, marketsMapModified, newMarketsOrEventsForOutsideCheck, pendingOrdersThread, orderCache, safetyLimits, marketCataloguesMap,
                                      marketCache, programStartTime);
            } else { // Statics variables don't exist, so I'm in a Client, and I won't calculate the limits here
            }
            rulesHaveChanged.set(true);
        }
        return modified;
    }

//    public synchronized boolean addManagedMarket(ManagedMarket managedMarket) {
//        if (marketIds.add(managedMarket.getId())) {
//            Statics.rulesManager.rulesHaveChanged.set(true);
//        }
//
//        return markets.add(managedMarket);
//    }

//    public synchronized boolean containsManagedMarket(ManagedMarket managedMarket) {
//        return markets.contains(managedMarket);
//    }

    //    public synchronized boolean containsMarketId(String marketId) {
//        return marketIds.contains(marketId);
//    }

    public synchronized String getId() {
        return this.id;
    }

    public synchronized String simpleGetEventName() {
        return this.eventName;
    }

    public synchronized String getEventName(@NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final ListOfQueues listOfQueues) {
        if (this.eventName == null) {
            attachEvent(eventsMap, listOfQueues);
        } else { // I already have eventName, I'll just return it
        }
        return this.eventName;
    }

    public synchronized void setEventName(final String eventName, @NotNull final ListOfQueues listOfQueues) {
        if (this.eventName == null && eventName != null) {
            this.eventName = eventName;
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setEventName, this.id, this.eventName));
        } else { // I'll keep the old name
        }
    }

    public final synchronized void attachEvent(@NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final ListOfQueues listOfQueues) {
        if (this.event == null) {
            this.event = eventsMap.get(this.id);
            if (this.event == null) {
                logger.error("no event found in eventsMap for: {}", this.id);
            } else {
                setEventName(this.event.getName(), listOfQueues);
            }
        } else { // I already have the event, nothing to be done
        }
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    public synchronized double getSimpleAmountLimit() {
        return this.amountLimit;
    }

    public synchronized double getAmountLimit(@NotNull final ExistingFunds safetyLimits) {
        final double result;
        final double safetyLimit = safetyLimits.getDefaultEventLimit();
        result = this.amountLimit >= 0 ? Math.min(this.amountLimit, safetyLimit) : safetyLimit;
        return result;
    }

    synchronized void calculateMarketLimits(@NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final ManagedEventsMap events,
                                            @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck, @NotNull final AtomicBoolean marketsMapModified,
                                            @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, @NotNull final ExistingFunds safetyLimits,
                                            @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, final long programStartTime) {
        final double maxEventLimit = getAmountLimit(safetyLimits);
        //noinspection NonPrivateFieldAccessedInSynchronizedContext
        Utils.calculateMarketLimits(maxEventLimit, this.marketsMap.valuesCopy(markets), true, true, pendingOrdersThread, orderCache, safetyLimits, marketCataloguesMap, marketCache, listOfQueues,
                                    marketsToCheck, events, markets, rulesHaveChanged, marketsForOutsideCheck, marketsMapModified, newMarketsOrEventsForOutsideCheck, programStartTime);
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public synchronized boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ManagedEvent that = (ManagedEvent) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(this.id);
    }
}
