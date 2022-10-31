package info.fmro.shared.logic;

import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.Utils;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

@SuppressWarnings("WeakerAccess")
public class ManagedEvent
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedEvent.class);
    @Serial
    private static final long serialVersionUID = 9206333179442623395L;
    public static final long RECENT_PERIOD = 2_000L;
    private final String id;
    private String eventName;
    private final long creationTime;
    private double amountLimit = -1d;
    public final SynchronizedSet<String> marketIds = new SynchronizedSet<>(); // managedMarket ids associated with this event
    @NotNull
    public transient ManagedMarketsMap marketsMap; // managedMarkets associated with this event
    @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
    private transient Event event;

    public ManagedEvent(@NotNull final String id, @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final RulesManager rulesManager) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
        //noinspection ThisEscapedInObjectConstruction
        this.marketsMap = new ManagedMarketsMap(this, rulesManager.markets);
        attachEvent(eventsMap, rulesManager.listOfQueues, true);
    }

    @Serial
    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.marketsMap = new ManagedMarketsMap(this);
    }

    public synchronized boolean isRecentlyCreated() {
        return isRecentlyCreated(System.currentTimeMillis());
    }

    public synchronized boolean isRecentlyCreated(final long currentTime) {
        return currentTime - this.creationTime <= RECENT_PERIOD;
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
    synchronized boolean setAmountLimit(final double newAmountLimit, @NotNull final RulesManager rulesManager, @NotNull final ExistingFunds safetyLimits, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
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
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setEventAmountLimit, this.id, this.amountLimit));
            if (SharedStatics.programName.get() == ProgramName.SERVER) {
                calculateMarketLimits(rulesManager, safetyLimits, marketCataloguesMap);
            } else { // I'm in a Client, and I won't calculate the limits here
            }
            rulesManager.rulesHaveChanged.set(true);
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

    public String getId() {
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
        attachEvent(eventsMap, listOfQueues, false);
    }

    public final synchronized void attachEvent(@NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final ListOfQueues listOfQueues, final boolean notFindingEventIsFine) {
        if (this.event == null) {
            this.event = eventsMap.get(this.id);
            if (this.event == null) {
                if (notFindingEventIsFine) { // no error, it's fine
                } else {
                    if (isRecentlyCreated()) {
                        logger.warn("no event found in eventsMap for recently created managedEvent: {}", this.id);
                    } else {
                        logger.error("no event found in eventsMap for: {}", this.id);
                    }
                }
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

    synchronized void calculateMarketLimits(@NotNull final RulesManager rulesManager, @NotNull final ExistingFunds safetyLimits, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        final double maxEventLimit = getAmountLimit(safetyLimits);
        //noinspection NonPrivateFieldAccessedInSynchronizedContext
        Utils.calculateMarketLimits(maxEventLimit, this.marketsMap.valuesCopy(rulesManager.markets), true, true, safetyLimits, marketCataloguesMap, rulesManager);
    }

    public double calculateExposureWithMarketExposuresAlreadyCalculated() { // not synchronized; assumes market exposures have already been calculated
        double eventExposure = 0d;
        final Collection<ManagedMarket> markets = this.marketsMap.valuesCopy();
        if (markets == null) { // error message was already printed
        } else {
            for (final ManagedMarket managedMarket : markets) {
                if (managedMarket == null) {
                    logger.error("null managedMarket in calculateExposure: {} {}", Generic.objectToString(this.marketIds), Generic.objectToString(markets));
                } else {
                    eventExposure += managedMarket.getMarketTotalExposure();
                }
            }
        }
        return eventExposure;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(final Object obj) {
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
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
