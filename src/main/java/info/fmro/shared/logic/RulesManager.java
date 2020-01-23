package info.fmro.shared.logic;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.stream.cache.Utils;
import info.fmro.shared.stream.cache.market.MarketCache;
import info.fmro.shared.stream.cache.order.OrderCache;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.MarketCatalogueInterface;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import info.fmro.shared.utility.SynchronizedSet;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("WeakerAccess")
public class RulesManager
        implements Serializable, StreamObjectInterface {
    private static final Logger logger = LoggerFactory.getLogger(RulesManager.class);
    private static final long serialVersionUID = -3496383465286913313L;
    public static final long fullCheckPeriod = Generic.MINUTE_LENGTH_MILLISECONDS;
    public transient ListOfQueues listOfQueues = new ListOfQueues();
    public final ManagedEventsMap events = new ManagedEventsMap(); // managedEvents are permanently stored only here
    public final SynchronizedMap<String, ManagedMarket> markets = new SynchronizedMap<>(); // managedMarkets are permanently stored only here
    //    public final SynchronizedSafeSet<RulesManagerStringObject> marketsToCheck = new SynchronizedSafeSet<>();
    public final ConcurrentLinkedQueue<String> marketsToCheck = new ConcurrentLinkedQueue<>(); // don't forget to activate the marketsToCheckExist marker when a new marketsToCheck is added
    public final SynchronizedSet<String> addManagedRunnerCommands = new SynchronizedSet<>();
    public transient AtomicBoolean newAddManagedRunnerCommand = new AtomicBoolean();
    public transient AtomicBoolean newOrderMarketCreated = new AtomicBoolean();
    public transient AtomicBoolean marketsToCheckExist = new AtomicBoolean();
    public transient AtomicBoolean marketsMapModified = new AtomicBoolean();
    //    public transient AtomicLong marketsToCheckStamp = new AtomicLong();
    public transient AtomicLong addManagedMarketsForExistingOrdersStamp = new AtomicLong();
    public transient AtomicBoolean rulesHaveChanged = new AtomicBoolean();
    public transient AtomicBoolean orderCacheHasReset = new AtomicBoolean();
    private transient long timeLastFullCheck;

    private Integer testMarker; // this variable should be the last declared and not be primitive, to attempt to have it serialized last

    // todo test with 1 runner, no cross runner matching; amountLimit on back and lay, limit on market & event; time limit; min odds for back and max odds for lay, with bets depending on prices existing on that runner
    // todo code beautification and simple tests, to prepare for the far more complicated integration tests

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();

        this.newAddManagedRunnerCommand = new AtomicBoolean();
        this.newOrderMarketCreated = new AtomicBoolean();
        this.marketsToCheckExist = new AtomicBoolean();
        this.marketsMapModified = new AtomicBoolean();
//        this.marketsToCheckStamp = new AtomicLong();
        this.addManagedMarketsForExistingOrdersStamp = new AtomicLong();
        this.rulesHaveChanged = new AtomicBoolean();
        this.orderCacheHasReset = new AtomicBoolean();

        this.timeLastFullCheck = 0L;
    }

    public synchronized RulesManager getCopy() {
        return SerializationUtils.clone(this);
    }

    public synchronized boolean copyFrom(final RulesManager other) {
        final boolean readSuccessful;
        if (!this.events.isEmpty() || !this.markets.isEmpty()) {
            logger.error("not empty map in RulesManager copyFrom: {}", Generic.objectToString(this));
            readSuccessful = false;
        } else {
            if (other == null) {
                logger.error("null other in copyFrom for: {}", Generic.objectToString(this));
                readSuccessful = false;
            } else {
                Generic.updateObject(this, other);

//                this.events.copyFrom(other.events);
//                this.markets.clear();
//                this.markets.putAll(other.markets.copy());
//                // likely forgot addManagedRunnerCommands

                this.setTestMarker(other.testMarker);

//                if (Statics.resetTestMarker) {
//                    logger.error("resetTestMarker {} , will still exit the program after reset", this.testMarker);
//                    final boolean objectModified = this.setTestMarker(Statics.TEST_MARKER);
//                    if (objectModified) {
//                        VarsIO.writeSettings();
//                    }
//                    readSuccessful = false; // in order to exit the program after reset
//                } else {
//                    readSuccessful = this.testMarker != null && this.testMarker == Statics.TEST_MARKER; // testMarker needs to have a certain value
//                }

                if (this.markets.isEmpty()) { // map still empty, no modification was made
                } else {
                    this.rulesHaveChanged.set(true);
                    this.marketsMapModified.set(true);
                }

                readSuccessful = true;
            }
        }
//        associateMarketsWithEvents( marketCataloguesMap);

        final int nQueues = this.listOfQueues.size();
        if (nQueues == 0) { // normal case, nothing to be done
        } else {
            logger.error("existing queues during RulesManager.copyFrom: {} {}", nQueues, Generic.objectToString(this));
            this.listOfQueues.send(this.getCopy());
        }

        return readSuccessful;
    }

//    private synchronized void associateMarketsWithEvents(@NotNull final SynchronizedMap<? super String, ? extends MarketCatalogueInterface> marketCataloguesMap) {
//        for (final ManagedMarket managedMarket : this.markets.valuesCopy()) {
//            managedMarket.getParentEvent( marketCataloguesMap); // this does the reciprocal association as well, by adding the markets in the managedEvent objects
//        }
//    }

//    @SuppressWarnings("UnusedReturnValue")
//    private synchronized boolean checkMarketsAreAssociatedWithEvents() {
//        boolean foundError = false;
//        for (final ManagedMarket managedMarket : this.markets.valuesCopy()) {
//            if (!managedMarket.parentEventIsSet() || !managedMarket.parentEventHasTheMarketAdded() || !managedMarket.parentEventHasTheMarketIdAdded()) { // error messages are printed inside the methods
//                managedMarket.getParentEvent(); // this should solve the problem
//                foundError = true;
//            }
//        }
//        return foundError;
//    }

//    public synchronized ManagedEvent getParentEvent(String eventId) {
//        ManagedEvent managedEvent = this.events.get(eventId);
//        if (managedEvent == null) {
//            managedEvent = new ManagedEvent(eventId);
//            this.events.put(eventId, managedEvent);
//            Statics.rulesManager.rulesHaveChanged.set(true);
//        } else { // I got the event and I'll return it, nothing else to be done
//        }
//
//        return managedEvent;
//    }

    public synchronized boolean addMarketToCheck(@NotNull final String marketId) {
        final boolean elementAdded;
        if (this.marketsToCheck.contains(marketId)) {
            elementAdded = false; // won't add same market again
        } else {
            this.marketsToCheck.add(marketId);
            elementAdded = true;
        }

        if (elementAdded) {
            this.marketsToCheckExist.set(true);
        } else { // nothing to be done on this branch
        }
        return elementAdded;
    }

    public synchronized Integer getTestMarker() {
        return this.testMarker;
    }

    public synchronized boolean setTestMarker(final Integer newValue) {
        final boolean modified;
        if (this.testMarker != null && this.testMarker.equals(newValue)) {
            modified = false;
        } else if (this.testMarker == null && newValue == null) {
            modified = false;
        } else {
            this.testMarker = newValue;
            modified = true;
        }

        return modified;
    }

    public synchronized boolean setMarketAmountLimit(@NotNull final String marketId, @NotNull final Double amountLimit) {
        final boolean success;
        final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket == null) {
            logger.error("null managedMarket in setMarketAmountLimit for: {} {}", marketId, amountLimit);
            success = false;
        } else {
            success = managedMarket.setAmountLimit(amountLimit, this);
        }

        return success;
    }

    public synchronized ManagedEvent removeManagedEvent(final String eventId) {
        this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedEvent, eventId));
        @Nullable final ManagedEvent managedEvent = this.events.remove(eventId);
        if (managedEvent != null) {
            @NotNull final HashSet<String> marketIds = managedEvent.marketIds.clear();
            for (final String marketId : marketIds) {
                removeManagedMarket(marketId);
            }
            this.rulesHaveChanged.set(true);
        } else {
            logger.error("trying to removeManagedEvent that doesn't exist: {}", eventId); // this also covers the case where the removed element is null, but this should never happen
        }
        return managedEvent;
    }

    private synchronized ManagedEvent addManagedEvent(final String eventId) {
        final ManagedEvent managedEvent;
        if (this.events.containsKey(eventId)) {
            managedEvent = this.events.get(eventId, this.rulesHaveChanged);
        } else {
            managedEvent = new ManagedEvent(eventId);
            this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedEvent, eventId, managedEvent));
            this.events.put(eventId, managedEvent);
            this.rulesHaveChanged.set(true);
        }
        return managedEvent;
    }

    public synchronized boolean addManagedEvent(final String eventId, final ManagedEvent managedEvent) {
        final boolean success;
        if (this.events.containsKey(eventId)) {
            final ManagedEvent existingManagedEvent = this.events.get(eventId, this.rulesHaveChanged);
            logger.error("trying to add managedEvent over existing one: {} {} {}", eventId, Generic.objectToString(existingManagedEvent), Generic.objectToString(managedEvent));
            success = false;
        } else if (managedEvent == null) {
            logger.error("trying to add null managedEvent: {}", eventId);
            success = false;
        } else {
            this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedEvent, eventId, managedEvent));
            this.events.put(eventId, managedEvent);
            this.rulesHaveChanged.set(true);
            success = true;
        }
        return success;
    }

    public synchronized ManagedEvent setEventAmountLimit(final String eventId, final Double newAmount, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache, @NotNull final SafetyLimitsInterface safetyLimits) {
        // newAmount == null resets the amount to default -1d value
        final ManagedEvent managedEvent = this.events.get(eventId);
        if (managedEvent != null) {
            managedEvent.setAmountLimit(newAmount == null ? -1d : newAmount, this, pendingOrdersThread, orderCache, safetyLimits);
        } else {
            logger.error("trying to setEventAmountLimit on an event that doesn't exist: {} {}", eventId, newAmount); // this also covers the case where the element is null, but this should never happen
        }
        return managedEvent;
    }

    private synchronized ManagedMarket addManagedMarket(final String marketId) {
        final ManagedMarket managedMarket;
        if (this.markets.containsKey(marketId)) {
            managedMarket = this.markets.get(marketId);
        } else {
            managedMarket = new ManagedMarket(marketId);
            this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedMarket, marketId, managedMarket));
            this.markets.put(marketId, managedMarket);
//            checkMarketsAreAssociatedWithEvents();
            this.rulesHaveChanged.set(true);
            this.marketsMapModified.set(true);

            addMarketToCheck(marketId);
        }
        return managedMarket;
    }

    public synchronized boolean addManagedMarket(final String marketId, final ManagedMarket managedMarket) {
        final boolean success;
        if (this.markets.containsKey(marketId)) {
            final ManagedMarket existingManagedMarket = this.markets.get(marketId);
            logger.error("trying to add managedMarket over existing one: {} {} {}", marketId, Generic.objectToString(existingManagedMarket), Generic.objectToString(managedMarket));
            success = false;
        } else {
            this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedMarket, marketId, managedMarket));
            this.markets.put(marketId, managedMarket);
//            checkMarketsAreAssociatedWithEvents();
            this.rulesHaveChanged.set(true);
            this.marketsMapModified.set(true);

            addMarketToCheck(marketId);
            success = true;
        }
        return success;
    }

    public synchronized boolean removeManagedRunner(@NotNull final String marketId, @NotNull final RunnerId runnerId) {
        final boolean success;
        final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket == null) {
            logger.error("null managedMarket in removeManagedRunner for: {} {}", marketId, Generic.objectToString(runnerId));
            success = false;
        } else {
            success = managedMarket.removeRunner(runnerId, this) != null;
        }

        return success;
    }

    public synchronized boolean addManagedRunner(@NotNull final ManagedRunner managedRunner) {
        final boolean success;
        final String marketId = managedRunner.getMarketId();
        final RunnerId runnerId = managedRunner.getRunnerId();
        if (marketId == null || runnerId == null) {
            logger.error("null ids in addManagedRunner: {} {} {}", marketId, Generic.objectToString(runnerId), Generic.objectToString(managedRunner));
            success = false;
        } else {
            final ManagedMarket managedMarket = this.addManagedMarket(marketId);
            if (managedMarket == null) {
                logger.error("null managedMarket in addManagedRunner for: {} {} {}", marketId, Generic.objectToString(runnerId), Generic.objectToString(managedRunner));
                success = false;
            } else {
                success = managedMarket.addRunner(runnerId, managedRunner, this);
            }
        }

        return success;
    }

    private synchronized void addManagedRunner(final String marketId, final long selectionId, final Double handicap, final double minBackOdds, final double maxLayOdds, final double backAmountLimit, final double layAmountLimit,
                                               final double marketAmountLimit, final double eventAmountLimit, @NotNull final SynchronizedMap<? super String, ? extends MarketCatalogueInterface> marketCataloguesMap,
                                               @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache, @NotNull final SafetyLimitsInterface safetyLimits) {
        final ManagedMarket managedMarket = this.addManagedMarket(marketId);
        if (managedMarket != null) {
            managedMarket.updateRunner(selectionId, handicap, minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit, this);
            managedMarket.setAmountLimit(marketAmountLimit, this);
            if (Double.isNaN(eventAmountLimit)) { // nothing to do, checked here for performance reason
            } else {
                final ManagedEvent managedEvent = managedMarket.getParentEvent(marketCataloguesMap, this);
                managedEvent.setAmountLimit(eventAmountLimit, this, pendingOrdersThread, orderCache, safetyLimits);
            }
        } else {
            logger.error("null managedMarket in addManagedRunner for: {} {} {} {} {} {} {} {} {}", marketId, selectionId, handicap, minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit, marketAmountLimit, eventAmountLimit);
        }
    }

    public synchronized boolean setRunnerBackAmountLimit(@NotNull final String marketId, @NotNull final RunnerId runnerId, @NotNull final Double amountLimit) {
        final boolean success;
        @Nullable final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket != null) {
            success = managedMarket.setRunnerBackAmountLimit(runnerId, amountLimit, this);
        } else {
            logger.error("trying to setRunnerBackAmountLimit on a market that doesn't exist: {} {} {}", marketId, Generic.objectToString(runnerId), amountLimit); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized boolean setRunnerLayAmountLimit(@NotNull final String marketId, @NotNull final RunnerId runnerId, @NotNull final Double amountLimit) {
        final boolean success;
        @Nullable final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket != null) {
            success = managedMarket.setRunnerLayAmountLimit(runnerId, amountLimit, this);
        } else {
            logger.error("trying to setRunnerLayAmountLimit on a market that doesn't exist: {} {} {}", marketId, Generic.objectToString(runnerId), amountLimit); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized boolean setRunnerMinBackOdds(@NotNull final String marketId, @NotNull final RunnerId runnerId, @NotNull final Double odds) {
        final boolean success;
        @Nullable final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket != null) {
            success = managedMarket.setRunnerMinBackOdds(runnerId, odds, this);
        } else {
            logger.error("trying to setRunnerMinBackOdds on a market that doesn't exist: {} {} {}", marketId, Generic.objectToString(runnerId), odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized boolean setRunnerMaxLayOdds(@NotNull final String marketId, @NotNull final RunnerId runnerId, @NotNull final Double odds) {
        final boolean success;
        @Nullable final ManagedMarket managedMarket = this.markets.get(marketId);
        if (managedMarket != null) {
            success = managedMarket.setRunnerMaxLayOdds(runnerId, odds, this);
        } else {
            logger.error("trying to setRunnerMaxLayOdds on a market that doesn't exist: {} {} {}", marketId, Generic.objectToString(runnerId), odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized ManagedMarket removeManagedMarket(final String marketId) {
        this.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedMarket, marketId));
        @Nullable final ManagedMarket managedMarket = this.markets.remove(marketId);
        if (managedMarket != null) {
            this.rulesHaveChanged.set(true);
            this.marketsMapModified.set(true);
        } else {
            logger.error("trying to removeManagedMarket that doesn't exist: {}", marketId); // this also covers the case where the removed element is null, but this should never happen
        }
        return managedMarket;
    }

    public synchronized void executeCommand(@NotNull final String commandString, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache, @NotNull final SafetyLimitsInterface safetyLimits) {
        if (commandString.startsWith("event ")) {
            final String eventString = commandString.substring("event ".length()).trim();
            if (eventString.contains(" ")) {
                final String eventId = eventString.substring(0, eventString.indexOf(' '));
                final String eventCommand = eventString.substring(eventString.indexOf(' ') + " ".length()).trim();
                if (eventCommand.startsWith("amountLimit")) {
                    String amountLimit = eventCommand.substring("amountLimit".length()).trim();
                    if (!amountLimit.isEmpty() && amountLimit.charAt(0) == '=') {
                        amountLimit = amountLimit.substring("=".length()).trim();
                    } else { // nothing to do on this branch
                    }
                    double doubleValue;
                    try {
                        doubleValue = Double.parseDouble(amountLimit);
                    } catch (NumberFormatException e) {
                        logger.error("NumberFormatException while getting doubleValue for amountLimit in executeCommand: {} {}", commandString, amountLimit, e);
                        doubleValue = Double.NaN;
                    }
                    if (Double.isNaN(doubleValue)) { // error message was already printed
                    } else {
                        final ManagedEvent managedEvent = addManagedEvent(eventId);
                        managedEvent.setAmountLimit(doubleValue, this, pendingOrdersThread, orderCache, safetyLimits);
                    }
                } else {
                    logger.error("unknown eventCommand in executeCommand: {} {}", eventCommand, commandString);
                }
            } else {
                logger.error("strange eventString in executeCommand: {} {}", eventString, commandString);
            }
        } else {
            logger.error("unknown command in executeCommand: {}", commandString);
        }
    }

//    private synchronized boolean isMarketsToCheckStampRecent() { // stamp is updated, but not checked for anything yet
//        return isMarketsToCheckStampRecent(500L); // default period
//    }
//
//    private synchronized boolean isMarketsToCheckStampRecent(final long recentPeriod) {
//        return timeSinceMarketsToCheckStamp() <= recentPeriod;
//    }

//    private synchronized long timeSinceMarketsToCheckStamp() {
//        final long currentTime = System.currentTimeMillis();
//        final long stampTime = this.marketsToCheckStamp.get();
//        return currentTime - stampTime;
//    }

    public synchronized void stampTimeLastFullCheck() {
        setTimeLastFullCheck(System.currentTimeMillis());
    }

    private synchronized void setTimeLastFullCheck(final long time) {
        this.timeLastFullCheck = time;
    }

    private synchronized long timeSinceFullRun() {
        final long currentTime = System.currentTimeMillis();
        return currentTime - this.timeLastFullCheck;
    }

    public synchronized long timeTillNextFullRun() {
        final long result;
//        if (this.timeLastFullCheck <= 0) {
//            result = 0;
//        } else {
        result = fullCheckPeriod - timeSinceFullRun(); // negative values are acceptable
//        }
        return result;
    }

    public synchronized void calculateMarketLimits(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache, @NotNull final SafetyLimitsInterface safetyLimits) {
        this.marketsMapModified.set(false);
        final double totalLimit = safetyLimits.getTotalLimit();
        Utils.calculateMarketLimits(totalLimit, this.markets.valuesCopy(), true, true, pendingOrdersThread, orderCache, safetyLimits);

        for (final ManagedEvent managedEvent : this.events.valuesCopy()) {
            managedEvent.calculateMarketLimits(this, pendingOrdersThread, orderCache, safetyLimits);
        }
    }

//    public synchronized void calculateMarketLimits(Collection<String> marketIds) {
//        final int nMarketIds = marketIds.size();
//        int nMarketCalculatedLimits = 0;
//        for (ManagedEvent managedEvent : this.events.valuesCopy()) {
//            boolean neededEvent = false;
//            for (String marketId : marketIds) {
//                if (managedEvent.marketIds.contains(marketId)) {
//                    neededEvent = true;
//                    nMarketCalculatedLimits++; // no break, as there might be more than one market attached to the same event and I should count that
//                } else { // marketId not contained in this event, nothing to be done
//                }
//            }
//            if (neededEvent) {
//                managedEvent.calculateMarketLimits();
//            } else { // I don't need to calculateMarketLimits for this event
//            }
//            if (nMarketCalculatedLimits >= nMarketIds) {
//                break;
//            }
//        }
//    }

    public synchronized void manageMarket(final ManagedMarket managedMarket, @NotNull final MarketCache marketCache, @NotNull final OrderCache orderCache, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate,
                                          @NotNull final BetFrequencyLimit speedLimit, @NotNull final SafetyLimitsInterface safetyLimits) {
        if (managedMarket == null) {
            logger.error("null managedMarket to check in RulesManager");
            this.markets.removeValueAll(null);
            this.listOfQueues.send(this.getCopy());
            this.rulesHaveChanged.set(true);
        } else {
            managedMarket.manage(marketCache, orderCache, pendingOrdersThread, currencyRate, speedLimit, safetyLimits, this);
        }
    }

    public synchronized void addManagedMarketsForExistingOrders(@NotNull final OrderCache orderCache) {
        final long currentTime = System.currentTimeMillis();
        final long stampTime = this.addManagedMarketsForExistingOrdersStamp.get();
        final long timeSinceStamp = currentTime - stampTime;
        if (this.newOrderMarketCreated.getAndSet(false) || timeSinceStamp > Generic.MINUTE_LENGTH_MILLISECONDS * 5L) {
            this.addManagedMarketsForExistingOrdersStamp.set(currentTime);
            final HashSet<String> orderMarkets = orderCache.getOrderMarketKeys();
            final Set<String> managedMarkets = this.markets.keySetCopy();
            orderMarkets.removeAll(managedMarkets);
            if (orderMarkets.isEmpty()) { // no new markets; nothing to be done, this should be the taken branch almost all the time
            } else {
                for (final String marketId : orderMarkets) {
                    logger.warn("adding new managed market in addManagedMarketsForExistingOrders: {}", marketId);
                    addManagedMarket(marketId);
                }
            }
        } else { // I won't run this method too often; nothing to be done
        }
    }

    public synchronized void resetOrderCacheObjects() {
        this.orderCacheHasReset.set(false);
        for (final ManagedMarket managedMarket : this.markets.valuesCopy()) {
            managedMarket.resetOrderCacheObjects();
        }
    }

    public synchronized void addManagedRunnerCommands(@NotNull final SynchronizedMap<? super String, ? extends MarketCatalogueInterface> marketCataloguesMap, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache,
                                                      @NotNull final SafetyLimitsInterface safetyLimits) {
        this.newAddManagedRunnerCommand.set(false);
        final HashSet<String> setCopy = this.addManagedRunnerCommands.clear();

        for (final String addManagedRunnerCommand : setCopy) {
            parseAddManagedRunnerCommand(addManagedRunnerCommand, marketCataloguesMap, pendingOrdersThread, orderCache, safetyLimits);
        }
    }

    @SuppressWarnings("OverlyNestedMethod")
    private synchronized void parseAddManagedRunnerCommand(final String addManagedRunnerCommand, @NotNull final SynchronizedMap<? super String, ? extends MarketCatalogueInterface> marketCataloguesMap,
                                                           @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache, @NotNull final SafetyLimitsInterface safetyLimits) {
        // String:marketId long:selectionId Double:handicap(default:null)
        // double optionals:minBackOdds maxLayOdds backAmountLimit layAmountLimit marketAmountLimit eventAmountLimit
        final String[] argumentsArray = Generic.splitStringAroundSpaces(addManagedRunnerCommand);
        final int arrayLength = argumentsArray.length;
        if (arrayLength >= 2 && arrayLength <= 9) {
            final String marketId = argumentsArray[0];
            final String selectionIdString = argumentsArray[1];
            long selectionId = -1L;
            try {
                selectionId = Long.parseLong(selectionIdString);
            } catch (NumberFormatException e) {
                logger.error("NumberFormatException while parsing selectionId in parseAddManagedRunnerCommand for: {} {}", selectionIdString, addManagedRunnerCommand);
            }

            if (selectionId < 0L) { // error message already printed, nothing to be done
            } else {
                @Nullable Double handicap = Double.NaN;
                double minBackOdds = Double.NaN, maxLayOdds = Double.NaN, backAmountLimit = Double.NaN, layAmountLimit = Double.NaN, marketAmountLimit = Double.NaN, eventAmountLimit = Double.NaN;
                boolean errorExists = false;
                for (int i = 2; i < arrayLength; i++) {
                    switch (i) {
                        case 2:
                            if ("null".equals(argumentsArray[i])) {
                                handicap = null;
                            } else {
                                handicap = Generic.parseDouble(argumentsArray[i]);
                                if (Double.isNaN(handicap)) {
                                    errorExists = true;
                                }
                            }
                            break;
                        case 3:
                            minBackOdds = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(minBackOdds)) {
                                errorExists = true;
                            }
                            break;
                        case 4:
                            maxLayOdds = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(maxLayOdds)) {
                                errorExists = true;
                            }
                            break;
                        case 5:
                            backAmountLimit = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(backAmountLimit)) {
                                errorExists = true;
                            }
                            break;
                        case 6:
                            layAmountLimit = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(layAmountLimit)) {
                                errorExists = true;
                            }
                            break;
                        case 7:
                            marketAmountLimit = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(marketAmountLimit)) {
                                errorExists = true;
                            }
                            break;
                        case 8:
                            eventAmountLimit = Generic.parseDouble(argumentsArray[i]);
                            if (Double.isNaN(eventAmountLimit)) {
                                errorExists = true;
                            }
                            break;
                        default:
                            logger.error("default switch branch entered inside parseAddManagedRunnerCommand for: {} {} {} {}", i, arrayLength, argumentsArray[i], addManagedRunnerCommand);
                            errorExists = true;
                            break;
                    }
                    if (errorExists) {
                        break;
                    }
                }
                if (errorExists) {
                    logger.error("bogus addManagedRunnerCommand: {} {}", arrayLength, addManagedRunnerCommand);
                } else {
                    addManagedRunner(marketId, selectionId, handicap, minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit, marketAmountLimit, eventAmountLimit, marketCataloguesMap, pendingOrdersThread, orderCache, safetyLimits);
                }
            }
        } else {
            logger.error("wrong arrayLength in parseAddManagedRunnerCommand for: {} {}", arrayLength, addManagedRunnerCommand);
        }
    }
}
