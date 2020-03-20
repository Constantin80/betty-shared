package info.fmro.shared.logic;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.enums.MarketBettingType;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.stream.cache.Utils;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.market.MarketCache;
import info.fmro.shared.stream.cache.order.OrderCache;
import info.fmro.shared.stream.cache.order.OrderMarket;
import info.fmro.shared.stream.cache.order.OrderMarketRunner;
import info.fmro.shared.stream.definitions.MarketDefinition;
import info.fmro.shared.stream.definitions.Order;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.ComparatorMarketPrices;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass", "WeakerAccess", "OverlyCoupledClass"})
public class ManagedMarket
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedMarket.class);
    private static final long serialVersionUID = -7958840665816144122L;
    public static final long recentCalculatedLimitPeriod = 30_000L;
    public static final long almostLivePeriod = Generic.HOUR_LENGTH_MILLISECONDS;
    public static final long veryRecentPeriod = 10_000L;
    public final AtomicBoolean cancelAllUnmatchedBets = new AtomicBoolean();
    private final HashMap<RunnerId, ManagedRunner> runners = new HashMap<>(4); // this is the only place where managedRunners are stored permanently
    private final HashMap<RunnerId, Double> runnerMatchedExposure = new HashMap<>(4), runnerTotalExposure = new HashMap<>(4);
    private final String id; // marketId
    private String parentEventId;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private String marketName;
    private double amountLimit = -1d; // only has effect if >= 0d
    private double calculatedLimit;
    private double marketMatchedExposure = Double.NaN;
    private double marketTotalExposure = Double.NaN;
    private double matchedBackExposureSum;
    private double totalBackExposureSum;
    private long timeMarketGoesLive, calculatedLimitStamp, manageMarketStamp;
    private boolean marketAlmostLive;
    private boolean enabledMarket = true;
    private final long creationTime;

    @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
    private transient ManagedEvent parentEvent;
    @SuppressWarnings({"InstanceVariableMayNotBeInitializedByReadObject", "FieldAccessedSynchronizedAndUnsynchronized"})
    private transient Market market;
    @Nullable
    @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
    private transient OrderMarket orderMarket;
//    private transient ArrayList<ManagedRunner> runnersOrderedList = new ArrayList<>(this.runners.values());

    public ManagedMarket(@NotNull final String id, @NotNull final MarketCache marketCache, @NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                         final long programStartTime) {
        this(id, marketCache, rulesManager, marketCataloguesMap, programStartTime, true); // default sendRunnersThroughStream true
    }

    @SuppressWarnings("ConstructorWithTooManyParameters")
    public ManagedMarket(@NotNull final String id, @NotNull final MarketCache marketCache, @NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                         final long programStartTime, final boolean sendRunnersThroughStream) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
//        this.parentEventId = info.fmro.shared.utility.Formulas.getEventIdOfMarketId(this.id, marketCataloguesMap);
//        this.runnersOrderedList.sort(Comparator.comparing(k -> k.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
        this.setMarketName(Formulas.getMarketCatalogueName(id, marketCataloguesMap), rulesManager);
        attachMarket(marketCache, rulesManager, marketCataloguesMap, programStartTime, sendRunnersThroughStream);
    }

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
//        this.runnersOrderedList = new ArrayList<>(this.runners.values());
//        this.runnersOrderedList.sort(Comparator.comparing(k -> k.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
    }

    @NotNull
    private synchronized ArrayList<ManagedRunner> createRunnersOrderedList(@NotNull final MarketCache marketCache) {
        final ArrayList<ManagedRunner> runnersOrderedList = new ArrayList<>(this.runners.values());
        runnersOrderedList.sort(Comparator.comparing(k -> k.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
        return runnersOrderedList;
    }

    @NotNull
    public synchronized HashMap<RunnerId, ManagedRunner> getRunners() {
        return new HashMap<>(this.runners);
    }

    private synchronized boolean isVeryRecent() {
        return isVeryRecent(System.currentTimeMillis());
    }

    private synchronized boolean isVeryRecent(final long currentTime) {
        return currentTime - this.creationTime <= veryRecentPeriod;
    }

    public synchronized boolean isEnabledMarket() {
        return this.enabledMarket;
    }

    public synchronized void setEnabledMarket(final boolean enabledMarket, @NotNull final RulesManager rulesManager) {
        setEnabledMarket(enabledMarket, rulesManager, true); // default true sendModificationThroughStream
    }

    public synchronized void setEnabledMarket(final boolean enabledMarket, @NotNull final RulesManager rulesManager, final boolean sendModificationThroughStream) {
        if (this.enabledMarket == enabledMarket) { // no update needed
        } else {
            this.enabledMarket = enabledMarket;
            rulesManager.rulesHaveChanged.set(true);
            rulesManager.marketsMapModified.set(true);
            if (rulesManager.marketsForOutsideCheck.add(this.id)) {
                rulesManager.newMarketsOrEventsForOutsideCheck.set(true);
            }
            rulesManager.addMarketToCheck(this.id);

            if (sendModificationThroughStream) {
                rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketEnabled, this.id, this.enabledMarket));
            } else { // no need to send this modification
            }
        }
    }

    public synchronized int getNRunners() {
        return this.runners.size();
    }

    public synchronized String getId() {
        return this.id;
    }

    public synchronized String simpleGetMarketName() {
        return this.marketName;
    }

    public synchronized String getMarketName(@NotNull final MarketCache marketCache, @NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                             final long programStartTime) {
        if (this.marketName == null) {
            attachMarket(marketCache, rulesManager, marketCataloguesMap, programStartTime);
        } else { // I already have marketName, I'll just return it
        }
        return this.marketName;
    }

    @SuppressWarnings("InstanceVariableUsedBeforeInitialized")
    public final synchronized void setMarketName(final String marketName, @NotNull final RulesManager rulesManager) {
        if (marketName != null && !Objects.equals(this.marketName, marketName)) {
            this.marketName = marketName;
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketName, this.id, this.marketName));
        } else { // new value null or same as old value
        }
    }

    private synchronized void setMarketName(final MarketDefinition marketDefinition, @NotNull final RulesManager rulesManager) {
        if (marketDefinition == null) {
            logger.error("null marketDefinition in setMarketName for: {}", Generic.objectToString(this));
        } else {
            if (this.marketName == null) {
                this.marketName = marketDefinition.getMarketType();
                if (this.marketName == null) {
                    logger.error("null marketName from marketDefinition for: {} {}", Generic.objectToString(marketDefinition), Generic.objectToString(this));
                } else {
                    rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketName, this.id, this.marketName));
                }
            } else { // I'll keep the old name
            }
        }
    }

    public synchronized String simpleGetParentEventId() {
        return this.parentEventId;
    }

    public synchronized String getParentEventId(@NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final AtomicBoolean rulesHaveChanged) {
        if (this.parentEventId == null) {
            this.parentEventId = Formulas.getEventIdOfMarketId(this.id, marketCataloguesMap);
            if (this.parentEventId == null) {
                if (this.market != null) {
                    this.parentEventId = this.market.getEventId();
                } else { // no Market present either, nothing to be done
                }
            } else { // found it, nothing to be done on this branch
            }

            if (this.parentEventId == null) {
                logger.info("parentEventId not found for managedMarket: {} {}", this.id, this.marketName);
            } else {
                rulesHaveChanged.set(true);
            }
        } else { // I already have parentId, nothing to be done
        }

        return this.parentEventId;
    }

    public synchronized ManagedEvent getParentEvent(@NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final RulesManager rulesManager) {
        if (this.parentEvent == null) {
            final String localParentEventId = this.getParentEventId(marketCataloguesMap, rulesManager.rulesHaveChanged);
            if (localParentEventId == null) {
                logger.info("parentEventId && parentEvent null in getParentEvent for: {} {}", this.id, this.marketName);
            } else {
                this.parentEvent = rulesManager.events.get(localParentEventId);
                if (this.parentEvent == null) { // I won't create the event here, but outside the method, in the caller, due to potential synchronization problems
                } else {
//            this.parentEvent.addManagedMarket(this);
                    final String marketId = this.getId();
                    this.parentEvent.marketsMap.putIfAbsent(marketId, this, rulesManager);
                    if (this.parentEvent.marketIds.add(marketId)) {
                        rulesManager.rulesHaveChanged.set(true);
                    }
                }
            }
        } else { // I already have parentEvent, nothing to be done
        }
        return this.parentEvent;
    }

    public synchronized boolean parentEventIsSet() { // used for testing if the parentEvent has been set, to detect bugs
        final boolean result = this.parentEvent != null;

        if (result) { // no error, nothing to be done, will return result
        } else {
            logger.error("false result in parentEventIsSet for: {}", Generic.objectToString(this));
        }

        return result;
    }

    public synchronized boolean parentEventHasTheMarketAdded() { // used for testing, to detect bugs
        final boolean result;
        result = this.parentEvent != null && this.parentEvent.marketIds.contains(this.id);

        if (result) { // no error, nothing to be done, will return result
        } else {
            logger.error("false result in parentEventHasTheMarketAdded for: {}", Generic.objectToString(this));
        }

        return result;
    }

    public synchronized boolean parentEventHasTheMarketIdAdded() { // used for testing, to detect bugs
        final boolean result;
        result = this.parentEvent != null && this.parentEvent.marketIds.contains(this.id);

        if (result) { // no error, nothing to be done, will return result
        } else {
            logger.error("false result in parentEventHasTheMarketIdAdded for: {}", Generic.objectToString(this));
        }

        return result;
    }

//    private synchronized double getIdealBackExposureSum() {
//        return idealBackExposureSum;
//    }
//
//    private synchronized void setIdealBackExposureSum(final double idealBackExposureSum) {
//        this.idealBackExposureSum = idealBackExposureSum;
//    }

    @Contract(pure = true)
    private synchronized double getMatchedBackExposureSum() {
        return this.matchedBackExposureSum;
    }

    private synchronized void setMatchedBackExposureSum(final double matchedBackExposureSum) {
        this.matchedBackExposureSum = matchedBackExposureSum;
    }

    @Contract(pure = true)
    private synchronized double getTotalBackExposureSum() {
        return this.totalBackExposureSum;
    }

    private synchronized void setTotalBackExposureSum(final double totalBackExposureSum) {
        this.totalBackExposureSum = totalBackExposureSum;
    }

    public synchronized boolean defaultExposureValuesExist() {
        return Double.isNaN(this.marketMatchedExposure) || Double.isNaN(this.marketTotalExposure);
    }

    public synchronized double getMarketMatchedExposure() {
        return this.marketMatchedExposure;
    }

    public synchronized double getMarketTotalExposure() {
        return this.marketTotalExposure;
    }

    public synchronized void resetOrderCacheObjects() {
        this.orderMarket = null;
        for (final ManagedRunner managedRunner : this.runners.values()) {
            managedRunner.resetOrderMarketRunner();
        }
    }

    public synchronized long getTimeMarketGoesLive(@NotNull final RulesManager rulesManager) {
        if (this.marketAlmostLive) { // I won't recalculate
        } else {
            calculateTimeMarketGoesLive(rulesManager);
        }
        return this.timeMarketGoesLive;
    }

    public synchronized boolean setTimeMarketGoesLive(final long newTimeMarketGoesLive, @NotNull final RulesManager rulesManager) {
        final boolean modified;
        if (newTimeMarketGoesLive <= 0L) {
            logger.error("attempt to set strange timeMarketGoesLive value {} for: {}", newTimeMarketGoesLive, Generic.objectToString(this));
            modified = false;
        } else if (this.timeMarketGoesLive == newTimeMarketGoesLive) {
            modified = false;
        } else if (this.timeMarketGoesLive == 0L) {
            this.timeMarketGoesLive = newTimeMarketGoesLive;
            modified = true;
        } else {
            logger.error("setting different value for timeMarketGoesLive {} to {}, difference of {} minutes for: {}", this.timeMarketGoesLive, newTimeMarketGoesLive, (newTimeMarketGoesLive - this.timeMarketGoesLive) / Generic.MINUTE_LENGTH_MILLISECONDS,
                         Generic.objectToString(this));
            this.timeMarketGoesLive = newTimeMarketGoesLive;
            modified = true;
        }

        if (modified) {
            rulesManager.addMarketToCheck(this.id);
        }
        return modified;
    }

    private synchronized void calculateTimeMarketGoesLive(@NotNull final RulesManager rulesManager) {
        final long result;
        if (this.market == null) {
            logger.error("null market in calculateTimeMarketGoesLive for: {}", Generic.objectToString(this));
            result = 0L;
        } else {
            final MarketDefinition marketDefinition = this.market.getMarketDefinition();
            if (marketDefinition == null) {
                logger.error("null marketDefinition in calculateTimeMarketGoesLive for: {}", Generic.objectToString(this.market));
                result = 0L;
            } else {
                final Date marketTime = marketDefinition.getMarketTime(); // I hope this is market start time, I'll test
                if (marketTime != null) {
                    result = marketTime.getTime();
                } else {
                    logger.error("null marketTime in calculateTimeMarketGoesLive for: {} {}", this.id, Generic.objectToString(marketDefinition));
                    result = 0L;
                }
            }
        }
        this.setTimeMarketGoesLive(result, rulesManager);
    }

    private synchronized void calculateTimeMarketGoesLive(final MarketDefinition marketDefinition, @NotNull final RulesManager rulesManager) {
        final long result;
        if (marketDefinition == null) {
            logger.error("null marketDefinition in calculateTimeMarketGoesLive with parameter for: {}", Generic.objectToString(this.market));
            result = 0L;
        } else {
            final Date marketTime = marketDefinition.getMarketTime(); // I hope this is market start time, I'll test
            if (marketTime != null) {
                result = marketTime.getTime();
            } else {
                logger.error("null marketTime in calculateTimeMarketGoesLive with parameter for: {} {}", this.id, Generic.objectToString(marketDefinition));
                result = 0L;
            }
        }
        this.setTimeMarketGoesLive(result, rulesManager);
    }

    synchronized boolean setAmountLimit(final double newAmountLimit, @NotNull final RulesManager rulesManager, @NotNull final ExistingFunds safetyLimits) {
        final boolean modified;
        if (Double.isNaN(newAmountLimit)) {
            modified = false;
        } else if (DoubleMath.fuzzyEquals(this.amountLimit, newAmountLimit, Formulas.CENT_TOLERANCE)) {
            modified = false;
        } else {
            this.amountLimit = newAmountLimit;
            modified = true;
        }

        if (modified) {
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketAmountLimit, this.id, this.amountLimit));
            rulesManager.rulesHaveChanged.set(true);

            rulesManager.addMarketToCheck(this.id);

            final double maxLimit = getMaxMarketLimit(safetyLimits);
            if (simpleGetCalculatedLimit() > maxLimit) {
                setCalculatedLimit(maxLimit, true, safetyLimits, rulesManager);
            } else { // no need to update calculatedLimit
            }
        }
        return modified;
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    public synchronized double getSimpleAmountLimit() {
        return this.amountLimit;
    }

    synchronized boolean setRunnerBackAmountLimit(@NotNull final RunnerId runnerId, @NotNull final Double runnerAmountLimit, @NotNull final RulesManager rulesManager) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            success = managedRunner.setBackAmountLimit(runnerAmountLimit, rulesManager);
        } else {
            logger.error("trying to setRunnerBackAmountLimit on a runner that doesn't exist: {} {} {}", this.id, Generic.objectToString(runnerId), runnerAmountLimit); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerLayAmountLimit(@NotNull final RunnerId runnerId, @NotNull final Double runnerAmountLimit, @NotNull final RulesManager rulesManager) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            success = managedRunner.setLayAmountLimit(runnerAmountLimit, rulesManager);
        } else {
            logger.error("trying to setRunnerLayAmountLimit on a runner that doesn't exist: {} {} {}", this.id, Generic.objectToString(runnerId), runnerAmountLimit); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerMaxLayOdds(@NotNull final RunnerId runnerId, @NotNull final Double odds, @NotNull final RulesManager rulesManager) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            success = managedRunner.setMaxLayOdds(odds, rulesManager);
        } else {
            logger.error("trying to setRunnerMaxLayOdds on a runner that doesn't exist: {} {} {}", this.id, Generic.objectToString(runnerId), odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerMinBackOdds(@NotNull final RunnerId runnerId, @NotNull final Double odds, @NotNull final RulesManager rulesManager) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            success = managedRunner.setMinBackOdds(odds, rulesManager);
        } else {
            logger.error("trying to setRunnerMinBackOdds on a runner that doesn't exist: {} {} {}", this.id, Generic.objectToString(runnerId), odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized ManagedRunner removeRunner(@NotNull final RunnerId runnerId, @NotNull final RulesManager rulesManager) {
        rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedRunner, this.id, runnerId));
        @Nullable final ManagedRunner managedRunner = this.runners.remove(runnerId);
        if (managedRunner != null) {
            rulesManager.rulesHaveChanged.set(true);
            rulesManager.addMarketToCheck(this.id);
        } else {
            logger.error("trying to removeManagedRunner that doesn't exist: {} {}", this.id, Generic.objectToString(runnerId)); // this also covers the case where the removed element is null, but this should never happen
        }
        return managedRunner;
    }

    public synchronized boolean addRunner(@NotNull final RunnerId runnerId, @NotNull final ManagedRunner managedRunner, @NotNull final RulesManager rulesManager) {
        return addRunner(runnerId, managedRunner, rulesManager, true); // default sendRunnerThroughStream true
    }

    public synchronized boolean addRunner(@NotNull final RunnerId runnerId, @NotNull final ManagedRunner managedRunner, @NotNull final RulesManager rulesManager, final boolean sendRunnerThroughStream) {
        final boolean success;
        if (this.runners.containsKey(runnerId) || this.runners.containsValue(managedRunner)) { // already exists, nothing to be done
            final ManagedRunner existingManagedRunner = this.runners.get(runnerId);
            logger.error("trying to add managedRunner over existing one: {} {} {} {} {}", this.id, this.marketName, Generic.objectToString(runnerId), Generic.objectToString(existingManagedRunner), Generic.objectToString(managedRunner));
            success = false;
        } else {
            if (sendRunnerThroughStream) {
                rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedRunner, managedRunner));
            } else { // no need to send runner through stream, most likely the entire market object will be sent
            }
            this.runners.put(runnerId, managedRunner); // runners.put needs to be before runnersOrderedList.addAll
            managedRunner.attachRunner(this.market);

            rulesManager.addMarketToCheck(this.id);
            success = true;
        }

        return success;
    }

    private synchronized ManagedRunner addRunner(@NotNull final RunnerId runnerId, @NotNull final RulesManager rulesManager) {
        return addRunner(runnerId, rulesManager, true); // default sendRunnerThroughStream true
    }

    private synchronized ManagedRunner addRunner(@NotNull final RunnerId runnerId, @NotNull final RulesManager rulesManager, final boolean sendRunnerThroughStream) {
        final ManagedRunner returnValue;
        if (this.runners.containsKey(runnerId)) { // already exists, nothing to be done
            returnValue = this.runners.get(runnerId);
        } else { // managedRunner does not exist, I'll generate it; this is done initially, but also later if runners are added
            returnValue = new ManagedRunner(this.id, runnerId);
            if (sendRunnerThroughStream) {
                rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedRunner, returnValue));
            } else { // no need to send runner through stream, most likely the entire market object will be sent
            }
            this.runners.put(runnerId, returnValue); // runners.put needs to be before runnersOrderedList.addAll
            returnValue.attachRunner(this.market);

            rulesManager.addMarketToCheck(this.id);

//            this.runnersOrderedList.clear();
//            this.runnersOrderedList.addAll(this.runners.values());
//            this.runnersOrderedList.sort(Comparator.comparing(ManagedRunner.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
        }

        return returnValue;
    }

    public synchronized void updateRunner(final long selectionId, final Double handicap, final double minBackOdds, final double maxLayOdds, final double backAmountLimit, final double layAmountLimit, @NotNull final RulesManager rulesManager) {
        if (selectionId > 0L) {
            final RunnerId runnerId = new RunnerId(selectionId, handicap);
            final ManagedRunner managedRunner = this.addRunner(runnerId, rulesManager);
            managedRunner.update(minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit, rulesManager);
        } else {
            logger.error("bogus selectionId in updateRunner for: {} {} {} {} {} {} {}", this.id, selectionId, handicap, minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit);
        }
    }

//    private synchronized ManagedRunner addRunner(@NotNull final ManagedRunner managedRunner) {
//        final RunnerId runnerId = managedRunner.getRunnerId();
//        return addRunner(runnerId, managedRunner);
//    }

//    private synchronized ManagedRunner addRunner(final RunnerId runnerId, @NotNull final ManagedRunner managedRunner) {
//        final ManagedRunner previousValue = this.runners.put(runnerId, managedRunner); // runners.put needs to be before runnersOrderedList.addAll
//
//        managedRunner.attachRunner(this.market);
//
//        this.runnersOrderedList.clear();
//        this.runnersOrderedList.addAll(this.runners.values());
//        this.runnersOrderedList.sort(Comparator.comparing(ManagedRunner::getLastTradedPrice, new ComparatorMarketPrices()));
//
//        return previousValue;
//    }

    public synchronized Market getMarket(@NotNull final MarketCache marketCache, @NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        attachMarket(marketCache, rulesManager, marketCataloguesMap, programStartTime);
        return this.market;
    }

    public final synchronized void attachMarket(@NotNull final MarketCache marketCache, @NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                                final long programStartTime) {
        attachMarket(marketCache, rulesManager, marketCataloguesMap, programStartTime, true); // default sendRunnerThroughStream true
    }

    public final synchronized void attachMarket(@NotNull final MarketCache marketCache, @NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap,
                                                final long programStartTime, final boolean sendRunnerThroughStream) {
        // this is run periodically, as it's contained in the manage method, that is run periodically
        if (this.market == null) {
            this.market = marketCache.getMarket(this.id);
        } else { // I already have the market, nothing to be done
        }
        if (this.market == null) {
            if (Formulas.programHasRecentlyStarted(System.currentTimeMillis(), programStartTime) || isVeryRecent()) { // normal
            } else {
                logger.info("no market found in MarketCache, probably old expired market, for: {} {}", this.id, this.marketName); // this happens for manager markets that are old and no longer exist on the site
            }
        } else {
            for (final ManagedRunner managedRunner : this.runners.values()) {
                managedRunner.attachRunner(this.market);
            }
            if (Generic.programName.get() == ProgramName.SERVER) {
                final HashSet<RunnerId> runnerIds = this.market.getRunnerIds();
                for (final RunnerId runnerId : runnerIds) {
                    if (runnerId != null) {
                        addRunner(runnerId, rulesManager, sendRunnerThroughStream); // only adds if doesn't exist
                    } else {
                        logger.error("null runnerId for orderMarket: {}", Generic.objectToString(this.market));
                    }
                } // end for
            } else { // will only auto add runners on the server, and the server will send them to the client, else I end up adding them twice
            }
            setMarketName(this.market.getMarketDefinition(), rulesManager);
        }
//        getParentEventId(marketCataloguesMap, rulesManager.rulesHaveChanged);
        getParentEvent(marketCataloguesMap, rulesManager); // includes getParentEventId
    }

    private synchronized void attachOrderMarket(@NotNull final OrderCache orderCache) { // this is run periodically, as it's contained in the manage method, that is run periodically
        if (this.orderMarket == null) {
            this.orderMarket = orderCache.getOrderMarket(this.id);
        } else { // I already have the market, nothing to be done on this branch
        }
        if (this.orderMarket == null) { // normal, it means no orders exist for this managedMarket, nothing else to be done
        } else {
            for (final ManagedRunner managedRunner : this.runners.values()) {
                managedRunner.attachOrderRunner(this.orderMarket);
            }
            final HashSet<RunnerId> runnerIds = this.orderMarket.getRunnerIds();
            for (final RunnerId runnerId : runnerIds) {
                if (runnerId != null) {
                    if (this.runners.containsKey(runnerId)) { // already exists, nothing to be done
                    } else { // managedRunner does not exist, this is an error that shouldn't happen and can't be properly fixed on this branch
                        logger.error("managedRunner does not exist for existing orderMarketRunner: {} {} {}", Generic.objectToString(runnerId), Generic.objectToString(this.runners), Generic.objectToString(this));
                    }
                } else {
                    logger.error("null runnerId for orderMarket: {}", Generic.objectToString(this.orderMarket));
                }
            } // end for
        }
    }

    private synchronized boolean isMarketAlmostLive(@NotNull final RulesManager rulesManager) {
        if (this.marketAlmostLive) { // already almostLive, won't recheck
        } else {
            final MarketDefinition marketDefinition = this.market.getMarketDefinition();
            final Boolean inPlay = marketDefinition.getInPlay();
            if (inPlay != null && inPlay) {
                this.marketAlmostLive = inPlay;
            } else {
                calculateTimeMarketGoesLive(marketDefinition, rulesManager);
                final long currentTime = System.currentTimeMillis();
                if (this.getTimeMarketGoesLive(rulesManager) + almostLivePeriod >= currentTime) {
                    this.marketAlmostLive = true;
                }
            }
            if (this.marketAlmostLive) {
                logger.info("managed market {} is almost live", this.id);
            }
        }
        return this.marketAlmostLive;
    }

    public synchronized double getTotalValue(@NotNull final AtomicDouble currencyRate) {
        final double result;
        if (this.market != null) {
            result = this.market.getTvEUR(currencyRate);
        } else {
            logger.error("no market present in getTotalValue for: {}", Generic.objectToString(this));
            result = 0d;
        }
        return result;
    }

    private synchronized boolean checkTwoWayMarketLimitsValid(final @NotNull ArrayList<? extends ManagedRunner> runnersOrderedList) {
        // maxBackLimit on one runner needs to be equal to maxLayLimit on the other, and vice-versa; same with odds being usable or unusable; max odds on back roughly inversely proportional to lay on the other runner, and vice-versa
        final boolean isValid;
        if (runnersOrderedList.size() == 2) {
            final ManagedRunner firstRunner = runnersOrderedList.get(0);
            final ManagedRunner secondRunner = runnersOrderedList.get(1);
            if (firstRunner == null || secondRunner == null) {
                logger.error("null runner in checkTwoWayMarketLimitsValid for: {} {} {} {}", Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), Generic.objectToString(runnersOrderedList), Generic.objectToString(this));
                isValid = false;
            } else {
                final double firstBackAmountLimit = firstRunner.getBackAmountLimit(), secondBackAmountLimit = secondRunner.getBackAmountLimit(), firstLayAmountLimit = firstRunner.getLayAmountLimit(), secondLayAmountLimit = secondRunner.getLayAmountLimit();
                //noinspection FloatingPointEquality
                if (firstBackAmountLimit != secondLayAmountLimit || secondBackAmountLimit != firstLayAmountLimit) {
                    logger.error("not equal amountLimits in checkTwoWayMarketLimitsValid for: {} {} {} {} {} {} {} {}", firstBackAmountLimit, secondBackAmountLimit, firstLayAmountLimit, secondLayAmountLimit, Generic.objectToString(firstRunner),
                                 Generic.objectToString(secondRunner), Generic.objectToString(runnersOrderedList), Generic.objectToString(this));
                    isValid = false;
                } else {
                    final double firstMinBackOdds = firstRunner.getMinBackOdds(), secondMinBackOdds = secondRunner.getMinBackOdds(), firstMaxLayOdds = firstRunner.getMaxLayOdds(), secondMaxLayOdds = secondRunner.getMaxLayOdds();
                    if (Formulas.oddsAreUsable(firstMinBackOdds) != Formulas.oddsAreUsable(secondMaxLayOdds) ||
                        Formulas.oddsAreUsable(secondMinBackOdds) != Formulas
                                .oddsAreUsable(firstMaxLayOdds)) {
                        logger.error("not equal oddsAreUsable in checkTwoWayMarketLimitsValid for: {} {} {} {} {} {} {} {}", firstMinBackOdds, secondMinBackOdds, firstMaxLayOdds, secondMaxLayOdds, Generic.objectToString(firstRunner),
                                     Generic.objectToString(secondRunner), Generic.objectToString(runnersOrderedList), Generic.objectToString(this));
                        isValid = false;
                    } else {
                        if (Formulas.oddsAreInverse(firstMinBackOdds, secondMaxLayOdds) && Formulas.oddsAreInverse(firstMaxLayOdds, secondMinBackOdds)) {
                            isValid = true;
                        } else {
                            logger.error("odds are not inverse in checkTwoWayMarketLimitsValid for: {} {} {} {} {} {} {} {}", firstMinBackOdds, secondMinBackOdds, firstMaxLayOdds, secondMaxLayOdds, Generic.objectToString(firstRunner),
                                         Generic.objectToString(secondRunner), Generic.objectToString(runnersOrderedList), Generic.objectToString(this));
                            isValid = false;
                        }
                    }
                }
            }
        } else {
            logger.error("wrong size runnersOrderedList in checkTwoWayMarketLimitsValid for: {} {}", Generic.objectToString(runnersOrderedList), Generic.objectToString(this));
            isValid = false;
        }
        return isValid;
    }

    private synchronized int balanceTwoRunnerMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final OrderMarketRunner firstOrderRunner, @NotNull final OrderMarketRunner secondOrderRunner,
                                                    @NotNull final List<Side> sideList, final double excessMatchedExposure, @NotNull final OrdersThreadInterface pendingOrdersThread) {
        int modifications = 0;
        if (sideList.size() != 2 || sideList.contains(null)) {
            logger.error("bogus sideList for balanceTwoRunnerMarket: {} {} {} {} {} {}", Generic.objectToString(sideList), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), Generic.objectToString(firstOrderRunner),
                         Generic.objectToString(secondOrderRunner), excessMatchedExposure);
        } else {
            final @NotNull Side firstSide = sideList.get(0), secondSide = sideList.get(1);
            if (firstSide == Side.B && secondSide == Side.L) {
                modifications += firstOrderRunner.cancelUnmatched(Side.L, pendingOrdersThread);
                modifications += secondOrderRunner.cancelUnmatched(Side.B, pendingOrdersThread);
                final @NotNull List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, firstOrderRunner, secondOrderRunner, sideList, excessMatchedExposure);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstOrderRunner.cancelUnmatchedAmounts(-firstExposureToBePlaced, 0d, pendingOrdersThread);
                } else {
                    if (firstOrderRunner.placeOrder(firstRunner.getBackAmountLimit(), firstRunner.getLayAmountLimit(), Side.B, firstRunner.getToBeUsedBackOdds(), firstExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondOrderRunner.cancelUnmatchedAmounts(0d, -secondExposureToBePlaced, pendingOrdersThread);
                } else {
                    if (secondOrderRunner.placeOrder(secondRunner.getBackAmountLimit(), secondRunner.getLayAmountLimit(), Side.L, secondRunner.getToBeUsedLayOdds(), secondExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
            } else if (firstSide == Side.L && secondSide == Side.B) {
                modifications += firstOrderRunner.cancelUnmatched(Side.B, pendingOrdersThread);
                modifications += secondOrderRunner.cancelUnmatched(Side.L, pendingOrdersThread);
                final @NotNull List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, firstOrderRunner, secondOrderRunner, sideList, excessMatchedExposure);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstOrderRunner.cancelUnmatchedAmounts(0d, -firstExposureToBePlaced, pendingOrdersThread);
                } else {
                    if (firstOrderRunner.placeOrder(firstRunner.getBackAmountLimit(), firstRunner.getLayAmountLimit(), Side.L, firstRunner.getToBeUsedLayOdds(), firstExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondOrderRunner.cancelUnmatchedAmounts(-secondExposureToBePlaced, 0d, pendingOrdersThread);
                } else {
                    if (secondOrderRunner.placeOrder(secondRunner.getBackAmountLimit(), secondRunner.getLayAmountLimit(), Side.B, secondRunner.getToBeUsedBackOdds(), secondExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
            } else {
                logger.error("bogus sides for balanceTwoRunnerMarket: {} {} {} {} {} {}", Generic.objectToString(sideList), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), Generic.objectToString(firstOrderRunner),
                             Generic.objectToString(secondOrderRunner), excessMatchedExposure);
            }
        }
        return modifications;
    }

    private synchronized int useNewLimitOnTwoRunnerMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final OrderMarketRunner firstOrderRunner, @NotNull final OrderMarketRunner secondOrderRunner,
                                                          @NotNull final List<Side> sideList, final double availableLimit, @NotNull final OrdersThreadInterface pendingOrdersThread) {
        int modifications = 0;
        if (sideList.size() != 2 || sideList.contains(null)) {
            logger.error("bogus sideList for useNewLimitOnTwoRunnerMarket: {} {} {} {} {} {}", Generic.objectToString(sideList), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), Generic.objectToString(firstOrderRunner),
                         Generic.objectToString(secondOrderRunner), availableLimit);
        } else {
            final @NotNull Side firstSide = sideList.get(0), secondSide = sideList.get(1);
            if (firstSide == Side.B && secondSide == Side.L) {
                final @NotNull List<Double> exposuresToBePlaced = Utils.getAmountsToBePlacedForTwoWayMarket(firstRunner, secondRunner, firstOrderRunner, secondOrderRunner, sideList, availableLimit);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstOrderRunner.cancelUnmatchedAmounts(-firstExposureToBePlaced, 0d, pendingOrdersThread);
                } else {
                    if (firstOrderRunner.placeOrder(firstRunner.getBackAmountLimit(), firstRunner.getLayAmountLimit(), Side.B, firstRunner.getToBeUsedBackOdds(), firstExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondOrderRunner.cancelUnmatchedAmounts(0d, -secondExposureToBePlaced, pendingOrdersThread);
                } else {
                    if (secondOrderRunner.placeOrder(secondRunner.getBackAmountLimit(), secondRunner.getLayAmountLimit(), Side.L, secondRunner.getToBeUsedLayOdds(), secondExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
            } else if (firstSide == Side.L && secondSide == Side.B) {
                final @NotNull List<Double> exposuresToBePlaced = Utils.getAmountsToBePlacedForTwoWayMarket(firstRunner, secondRunner, firstOrderRunner, secondOrderRunner, sideList, availableLimit);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstOrderRunner.cancelUnmatchedAmounts(0d, -firstExposureToBePlaced, pendingOrdersThread);
                } else {
                    if (firstOrderRunner.placeOrder(firstRunner.getBackAmountLimit(), firstRunner.getLayAmountLimit(), Side.L, firstRunner.getToBeUsedLayOdds(), firstExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondOrderRunner.cancelUnmatchedAmounts(-secondExposureToBePlaced, 0d, pendingOrdersThread);
                } else {
                    if (secondOrderRunner.placeOrder(secondRunner.getBackAmountLimit(), secondRunner.getLayAmountLimit(), Side.B, secondRunner.getToBeUsedBackOdds(), secondExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
            } else {
                logger.error("bogus sides for useNewLimitOnTwoRunnerMarket: {} {} {} {} {} {}", Generic.objectToString(sideList), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), Generic.objectToString(firstOrderRunner),
                             Generic.objectToString(secondOrderRunner), availableLimit);
            }
        }
        return modifications;
    }

    private synchronized int removeExposure(final @NotNull ArrayList<? extends ManagedRunner> runnersOrderedList, @NotNull final OrderCache orderCache, @NotNull final OrdersThreadInterface pendingOrdersThread) {
        // assumes market and runners exposure has been updated
        int modifications = 0;
        if (Double.isNaN(this.marketTotalExposure)) {
            logger.error("marketTotalExposure not initialized in removeExposure for: {}", Generic.objectToString(this));
        } else if (this.marketTotalExposure < .1) { // exposure too small, nothing to be done
        } else {
            final int size = runnersOrderedList.size();
            // in all cases, existing unmatched bets on the wrong side are canceled, bets on the right side are kept within the necessary amount, and if extra is needed it is added
            if (size == 2) { // special case
                if (checkTwoWayMarketLimitsValid(runnersOrderedList)) {
                    // if valid, balance both runners at the same time, with back on one and lay on the other, with different factors used in calculating the amounts
                    // the factors are the price of toBeUsedOdds, and which of the toBeUsedOdds is more profitable; those two should be enough for now; lay bets should be given slight priority over back bets, as other gamblers like to back rather than lay
                    // in the special case, market wide exposure is used, rather than the one on each runner
                    final ManagedRunner firstRunner = runnersOrderedList.get(0), secondRunner = runnersOrderedList.get(1);
                    final double backLayMatchedExposure = firstRunner.getBackMatchedExposure() + secondRunner.getLayMatchedExposure(), layBackMatchedExposure = firstRunner.getLayMatchedExposure() + secondRunner.getBackMatchedExposure(),
                            excessMatchedExposure = Math.abs(backLayMatchedExposure - layBackMatchedExposure);
                    final OrderMarketRunner firstOrderRunner = firstRunner.getOrderMarketRunner(orderCache), secondOrderRunner = secondRunner.getOrderMarketRunner(orderCache);
                    if (firstOrderRunner == null || secondOrderRunner == null) {
                        logger.error("null OrderMarketRunner during removeExposure for: {} {} {}", Generic.objectToString(firstOrderRunner), Generic.objectToString(secondOrderRunner), Generic.objectToString(this));
                        modifications += cancelAllUnmatchedBets(pendingOrdersThread);
                    } else if (excessMatchedExposure < .1d) {
                        modifications += cancelAllUnmatchedBets(pendingOrdersThread);
                    } else if (backLayMatchedExposure > layBackMatchedExposure) {
                        // I'll use unmatched exposure, equal to excessMatchedExposure, on lay/back
                        modifications += balanceTwoRunnerMarket(firstRunner, secondRunner, firstOrderRunner, secondOrderRunner, List.of(Side.L, Side.B), excessMatchedExposure, pendingOrdersThread);
                    } else { // backLayMatchedExposure < layBackMatchedExposure
                        // I'll use unmatched exposure, equal to excessMatchedExposure, on back/lay
                        modifications += balanceTwoRunnerMarket(firstRunner, secondRunner, firstOrderRunner, secondOrderRunner, List.of(Side.B, Side.L), excessMatchedExposure, pendingOrdersThread);
                    }
                } else { // if not valid, error message and take action, with all order canceling
                    logger.error("checkTwoWayMarketLimitsValid false in removeExposure for: {}", Generic.objectToString(this));
                    modifications += cancelAllUnmatchedBets(pendingOrdersThread);
                }
            } else {
                for (final ManagedRunner managedRunner : runnersOrderedList) { // only the exposure on the runner is considered, not the market wide exposure
                    modifications += managedRunner.removeExposure(pendingOrdersThread, orderCache);
                } // end for
            }
        }
        return modifications;
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyNestedMethod"})
    private synchronized int useTheNewLimit(final @NotNull ArrayList<? extends ManagedRunner> runnersOrderedList, @NotNull final OrderCache orderCache, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final RulesManager rulesManager,
                                            final long programStartTime) {
        int modifications = 0;
        if (Double.isNaN(this.marketTotalExposure)) {
            logger.error("marketTotalExposure not initialized in useTheNewLimit for: {}", Generic.objectToString(this));
        } else {
            // variant of removeExposure, or the other way around ... major difference is that in this case the overall calculatedLimit matters, this time it's not about individual runners
            final int size = runnersOrderedList.size();
            if (size == 2) { // special case
                // this one is easy, most similar to the removeExposure situation
                // the factors for splitting the unmatched exposure between runners are the price of toBeUsedOdds, which of the toBeUsedOdds is more profitable, and the size of existing unmatched bets
                // also lay bets should be given slight priority over back bets, as other gamblers like to back rather than lay
                if (checkTwoWayMarketLimitsValid(runnersOrderedList)) {
                    final ManagedRunner firstRunner = runnersOrderedList.get(0), secondRunner = runnersOrderedList.get(1);
                    final double backLayExposure = firstRunner.getBackTotalExposure() + secondRunner.getLayTotalExposure(), layBackExposure = firstRunner.getLayTotalExposure() + secondRunner.getBackTotalExposure();
                    final double availableBackLayLimit = this.calculatedLimit - backLayExposure, availableLayBackLimit = this.calculatedLimit - layBackExposure;
                    final OrderMarketRunner firstOrderRunner = firstRunner.getOrderMarketRunner(orderCache), secondOrderRunner = secondRunner.getOrderMarketRunner(orderCache);
                    if (firstOrderRunner == null || secondOrderRunner == null) {
                        logger.error("null OrderMarketRunner during useTheNewLimit for: {} {} {}", Generic.objectToString(firstOrderRunner), Generic.objectToString(secondOrderRunner), Generic.objectToString(this));
                        modifications += cancelAllUnmatchedBets(pendingOrdersThread);
                    } else {
                        if (availableBackLayLimit == 0d) { // availableLimit is 0d, nothing to be done
                        } else {
                            modifications += useNewLimitOnTwoRunnerMarket(firstRunner, secondRunner, firstOrderRunner, secondOrderRunner, List.of(Side.L, Side.B), availableBackLayLimit, pendingOrdersThread);
                        }
                        if (availableLayBackLimit == 0d) { // availableLimit is 0d, nothing to be done
                        } else {
                            modifications += useNewLimitOnTwoRunnerMarket(firstRunner, secondRunner, firstOrderRunner, secondOrderRunner, List.of(Side.B, Side.L), availableLayBackLimit, pendingOrdersThread);
                        }
                    }
                } else { // if not valid, error message and take action, with all order canceling
                    logger.error("checkTwoWayMarketLimitsValid false in useTheNewLimit for: {}", Generic.objectToString(this));
                    modifications += cancelAllUnmatchedBets(pendingOrdersThread);
                }
            } else {
                // calculate the splitting proportion for each runner: the factors for splitting the unmatched exposure between runners are the price of toBeUsedOdds, and the size of existing unmatched bets
                // total exposure only exists as total back exposure on each runner, and can be increased by lay on that runner and back on other runners
                // each runner has total back exposure, and ideally it should be equal to the calculatedLimit, but has to obey the per runner limits as well

                calculateIdealBackExposureList(runnersOrderedList, rulesManager);

                // new limit for lay exposure on the runner will be calculated by proportion * calculatedLimit; runner limits are also considered
                // calculate the amounts that need to be added or subtracted on the back side of that runner, for the new lay exposure limit
                // place the needed orders and recalculate exposure
                if (Math.abs(this.calculatedLimit - this.marketTotalExposure) < .1d) { // potential extra lay bets are placed after the conditional, nothing to be done here
                } else if (this.calculatedLimit > this.marketTotalExposure) { // placing extra bets, starting with back
                    if (this.calculatedLimit >= this.totalBackExposureSum + .1d) { // placing extra back bets
                        final double extraBackBetsToBePlaced = Math.min(this.calculatedLimit - this.marketTotalExposure, this.calculatedLimit - this.totalBackExposureSum);
                        final double availableIdealBackExposure = calculateAvailableIdealBackExposureSum(runnersOrderedList);
                        if (availableIdealBackExposure == 0d) { // this is normal when no managed runners exist on market
//                            logger.info("zero availableIdealBackExposure: {} {} {} {} {} {} {}", availableIdealBackExposure, extraBackBetsToBePlaced, this.totalBackExposureSum, this.calculatedLimit, this.marketTotalExposure, this.id, this.marketName);
                        } else if (availableIdealBackExposure < 0d) {
                            logger.error("negative availableIdealBackExposure: {} {} {} {} {} {}", availableIdealBackExposure, extraBackBetsToBePlaced, this.totalBackExposureSum, this.calculatedLimit, this.marketTotalExposure,
                                         Generic.objectToString(this, "parentEvent"));
                        } else {
                            final double proportionOfAvailableIdealBackExposureToBeUsed = Math.min(1d, extraBackBetsToBePlaced / availableIdealBackExposure);
                            for (final ManagedRunner managedRunner : runnersOrderedList) {
                                final double amountToPlaceOnBack = (managedRunner.getIdealBackExposure() - managedRunner.getBackTotalExposure()) * proportionOfAvailableIdealBackExposureToBeUsed;
                                if (amountToPlaceOnBack > .1d) {
                                    if (pendingOrdersThread.addPlaceOrder(this.id, managedRunner.getRunnerId(), Side.B, managedRunner.getToBeUsedBackOdds(), amountToPlaceOnBack) > 0d) {
                                        modifications++;
                                    } else { // no modification made, nothing to be done
                                    }
                                } else { // amount negative or too small, won't place anything
                                }
                            } // end for
                        }
                    } else { // potential extra lay bets are placed after the conditional, nothing to be done here
                    }
                } else { // this.calculatedLimit < this.marketTotalExposure; removing bets
                    if (this.totalBackExposureSum >= this.calculatedLimit + .1d) { // removing back bets
                        final double backBetsToBeRemoved = Math.min(this.marketTotalExposure - this.calculatedLimit, this.totalBackExposureSum - this.calculatedLimit);
                        final double excessiveBackExposureOverIdeal = calculateExcessiveBackExposureOverIdealSum(runnersOrderedList);
                        if (excessiveBackExposureOverIdeal <= 0d) {
                            logger.error("negative or zero excessiveBackExposureOverIdeal: {} {} {} {} {} {}", excessiveBackExposureOverIdeal, backBetsToBeRemoved, this.totalBackExposureSum, this.calculatedLimit, this.marketTotalExposure,
                                         Generic.objectToString(this));
                        } else {
                            final double proportionOfExcessiveExposureToBeRemoved = Math.min(1d, backBetsToBeRemoved / excessiveBackExposureOverIdeal);
                            for (final ManagedRunner managedRunner : runnersOrderedList) {
                                final double amountToRemoveFromBack = (managedRunner.getBackTotalExposure() - managedRunner.getIdealBackExposure()) * proportionOfExcessiveExposureToBeRemoved;
                                if (amountToRemoveFromBack > 0d) {
                                    final OrderMarketRunner orderMarketRunner = managedRunner.getOrderMarketRunner(orderCache);
                                    if (orderMarketRunner == null) {
                                        logger.error("null orderMarketRunner while positive amountToRemoveFromBack for: {} {} {}", amountToRemoveFromBack, Generic.objectToString(managedRunner), Generic.objectToString(this));
                                    } else {
                                        modifications += orderMarketRunner.balanceTotalAmounts(managedRunner.getBackAmountLimit(), managedRunner.getLayAmountLimit(), managedRunner.getToBeUsedBackOdds(), managedRunner.getToBeUsedLayOdds(),
                                                                                               amountToRemoveFromBack, 0d, pendingOrdersThread);
                                    }
                                } else { // amount negative, won't remove anything
                                }
                            } // end for
                        }
                    } else { // potential lay bets are removed after the conditional, nothing to be done here
                    }
                }
                if (modifications > 0) {
                    calculateExposure(pendingOrdersThread, orderCache, programStartTime, rulesManager);
                    modifications = 0;
                } else { // no need to calculateExposure
                }

                // I can still place or remove some lay bets
                // use the calculatedLimit and modify the bets on the lay side of the runner, considering the runner limit as well
                // place the orders, recalculating exposure now shouldn't be necessary
                for (final ManagedRunner managedRunner : runnersOrderedList) {
                    final double availableLayLimit = managedRunner.getLayAmountLimit() - managedRunner.getLayTotalExposure();
                    final double availableMarketLimit = this.calculatedLimit - this.totalBackExposureSum + managedRunner.getBackTotalExposure() - managedRunner.getLayTotalExposure();
                    final double minimumAvailableLimit = Math.min(availableLayLimit, availableMarketLimit);
                    if (minimumAvailableLimit > .1d) {
                        final double toBeUsedLayOdds = managedRunner.getToBeUsedLayOdds();
                        if (info.fmro.shared.utility.Formulas.oddsAreUsable(toBeUsedLayOdds)) {
                            final double amountToPlaceOnLay = minimumAvailableLimit / (toBeUsedLayOdds - 1d);
                            if (pendingOrdersThread.addPlaceOrder(this.id, managedRunner.getRunnerId(), Side.L, toBeUsedLayOdds, amountToPlaceOnLay) > 0d) {
                                modifications++;
                            } else { // no modification made, nothing to be done
                            }
                        } else { // odds unusable, nothing to be done
                        }
                    } else if (minimumAvailableLimit < -.1d) {
                        final OrderMarketRunner orderMarketRunner = managedRunner.getOrderMarketRunner(orderCache);
                        if (orderMarketRunner == null) {
                            logger.error("null orderMarketRunner while negative minimumAvailableLimit for: {} {} {}", minimumAvailableLimit, Generic.objectToString(managedRunner), Generic.objectToString(this));
                        } else {
                            modifications += orderMarketRunner.balanceTotalAmounts(managedRunner.getBackAmountLimit(), managedRunner.getLayAmountLimit(), managedRunner.getToBeUsedBackOdds(), managedRunner.getToBeUsedLayOdds(), 0d,
                                                                                   -minimumAvailableLimit, pendingOrdersThread);
                        }
                    } else { // difference too small, nothing to be done
                    }
                }
            }
        }
        return modifications;
    }

    private synchronized double calculateAvailableIdealBackExposureSum(final @NotNull Iterable<? extends ManagedRunner> runnersOrderedList) { // I will only add the positive amounts
        double availableIdealBackExposureSum = 0d;
        for (final ManagedRunner managedRunner : runnersOrderedList) {
            final double availableIdealBackExposure = managedRunner.getIdealBackExposure() - managedRunner.getBackTotalExposure();
            if (availableIdealBackExposure > 0d) {
                availableIdealBackExposureSum += availableIdealBackExposure;
            } else { // I won't add negative amounts, nothing to be done
            }
        }
        return availableIdealBackExposureSum;
    }

    private synchronized double calculateExcessiveBackExposureOverIdealSum(final @NotNull Iterable<? extends ManagedRunner> runnersOrderedList) { // I will only add the positive amounts
        double excessiveBackExposureOverIdealSum = 0d;
        for (final ManagedRunner managedRunner : runnersOrderedList) {
            final double excessiveBackExposureOverIdeal = managedRunner.getBackTotalExposure() - managedRunner.getIdealBackExposure();
            if (excessiveBackExposureOverIdeal > 0d) {
                excessiveBackExposureOverIdealSum += excessiveBackExposureOverIdeal;
            } else { // I won't add negative amounts, nothing to be done
            }
        }
        return excessiveBackExposureOverIdealSum;
    }

    private synchronized void calculateProportionOfMarketLimitPerRunnerList(final @NotNull Collection<? extends ManagedRunner> runnersOrderedList, @NotNull final RulesManager rulesManager) { // calculated proportions depend on the toBeUsedBackOdds
        final double sumOfStandardAmounts = runnersOrderedList.stream().filter(x -> info.fmro.shared.utility.Formulas.oddsAreUsable(x.getToBeUsedBackOdds())).mapToDouble(x -> 1d / (x.getToBeUsedBackOdds() - 1d)).sum();
        for (final ManagedRunner managedRunner : runnersOrderedList) { // sumOfStandardAmounts should always be != 0d if at least one oddsAreUsable
            final double proportion = info.fmro.shared.utility.Formulas.oddsAreUsable(managedRunner.getToBeUsedBackOdds()) ? 1d / (managedRunner.getToBeUsedBackOdds() - 1d) / sumOfStandardAmounts : 0d;
            managedRunner.setProportionOfMarketLimitPerRunner(proportion, rulesManager);
        }
    }

    private synchronized void calculateIdealBackExposureList(final @NotNull ArrayList<? extends ManagedRunner> runnersOrderedList, @NotNull final RulesManager rulesManager) {
        calculateProportionOfMarketLimitPerRunnerList(runnersOrderedList, rulesManager);
        // reset idealBackExposure
        for (final ManagedRunner managedRunner : runnersOrderedList) {
            managedRunner.setIdealBackExposure(0d, rulesManager);
        }
        double exposureLeftToBeAssigned = this.calculatedLimit;
        final Collection<ManagedRunner> runnersThatCanStillBeAssignedExposure = new ArrayList<>(runnersOrderedList);
        int whileCounter = 0;
        while (exposureLeftToBeAssigned >= .1d && !runnersThatCanStillBeAssignedExposure.isEmpty() && whileCounter < 100) {
            final double exposureLeftToBeAssignedAtBeginningOfLoopIteration = exposureLeftToBeAssigned;
            final Collection<ManagedRunner> runnersToRemove = new ArrayList<>(2);
            // calculate total proportion of remaining runners; initially it should be 1d
            double totalProportionSumForRemainingRunners = 0d;
            for (final ManagedRunner managedRunner : runnersThatCanStillBeAssignedExposure) {
                totalProportionSumForRemainingRunners += managedRunner.getProportionOfMarketLimitPerRunner();
            }
            if (totalProportionSumForRemainingRunners > 0d) {
                for (final ManagedRunner managedRunner : runnersThatCanStillBeAssignedExposure) {
                    final double idealExposure = exposureLeftToBeAssignedAtBeginningOfLoopIteration * managedRunner.getProportionOfMarketLimitPerRunner() / totalProportionSumForRemainingRunners;
                    final double assignedExposure = managedRunner.addIdealBackExposure(idealExposure, rulesManager);
                    exposureLeftToBeAssigned -= assignedExposure;
                    if (assignedExposure < idealExposure || assignedExposure == 0d) {
                        runnersToRemove.add(managedRunner);
                    } else { // all exposure has been added, this runner might still be usable in further iterations of the while loop
                    }
                }
            } else {//non positive totalProportionSumForRemainingRunners 0.0 for: 584.3413960020026 0 20 1.162659886 Winner
                if (totalProportionSumForRemainingRunners == 0d) { // normal when no managed runners exist on market
                } else {
                    logger.error("negative totalProportionSumForRemainingRunners {} for: {} {} {} {} {}", totalProportionSumForRemainingRunners, exposureLeftToBeAssigned, whileCounter, runnersThatCanStillBeAssignedExposure.size(), this.id,
                                 this.marketName);
                }
                runnersThatCanStillBeAssignedExposure.clear();
                break;
            }

            runnersThatCanStillBeAssignedExposure.removeAll(runnersToRemove);
            whileCounter++;
        }
        if (exposureLeftToBeAssigned >= .1d && !runnersThatCanStillBeAssignedExposure.isEmpty()) {
            logger.error("runnersThatCanStillBeAssignedExposure not empty: {} {} {} {} {}", whileCounter, exposureLeftToBeAssigned, runnersThatCanStillBeAssignedExposure.size(), this.id, this.marketName);
        } else { // no error, nothing to print
        }
//        updateIdealBackExposureSum();
    }

    public synchronized double getMaxMarketLimit(@NotNull final ExistingFunds safetyLimits) {
        final double result;
        final double safetyLimit = safetyLimits.getDefaultMarketLimit(this.id);
        result = this.amountLimit >= 0 ? Math.min(this.amountLimit, safetyLimit) : safetyLimit;
        return result;
    }

    private synchronized boolean isCalculatedLimitRecent() {
        final long currentTime = System.currentTimeMillis();
        final long timeSinceStamp = currentTime - this.calculatedLimitStamp;
        return timeSinceStamp <= recentCalculatedLimitPeriod;
    }

    private synchronized void calculatedLimitStamp() {
        final long currentTime = System.currentTimeMillis();
        if (currentTime > this.calculatedLimitStamp) {
            this.calculatedLimitStamp = currentTime;
        } else if (currentTime == this.calculatedLimitStamp) { // happens
        } else {
            logger.error("currentTime {} is not greater than calculatedLimitStamp {} and difference is: {} for: {}", currentTime, this.calculatedLimitStamp, currentTime - this.calculatedLimitStamp, this.id);
        }
    }

    public synchronized boolean setCalculatedLimit(final double newLimit, final boolean limitCanBeIncreased, @NotNull final ExistingFunds safetyLimits, @NotNull final RulesManager rulesManager) {
        final boolean modified;
        modified = (limitCanBeIncreased || newLimit < this.calculatedLimit) && setCalculatedLimit(newLimit, safetyLimits, rulesManager);
        return modified;
    }

    private synchronized boolean setCalculatedLimit(final double newLimit, @NotNull final ExistingFunds safetyLimits, @NotNull final RulesManager rulesManager) {
        final boolean gettingModified;
//        logger.info("setCalculatedLimit: {} {}", newLimit, this.calculatedLimit);

        // both are zero
        if (this.calculatedLimit == 0) {
            //noinspection FloatingPointEquality
            gettingModified = newLimit != this.calculatedLimit;
        } else {
            final double difference = Math.abs(newLimit - this.calculatedLimit);
            final double differenceProportion = difference / this.calculatedLimit;
            gettingModified = difference >= 2d || differenceProportion >= .02d;
        }

        if (gettingModified) {
            this.calculatedLimit = newLimit;
            final double maxLimit = this.getMaxMarketLimit(safetyLimits);
//            this.calculatedLimit = Math.min(this.calculatedLimit, maxLimit);
            if (this.calculatedLimit > maxLimit) {
                this.calculatedLimit = maxLimit;
            }
            if (this.calculatedLimit < 0d) {
                logger.error("trying to set negative calculated limit {} in setCalculatedLimit for: {}", this.calculatedLimit, Generic.objectToString(this));
                this.calculatedLimit = 0d;
            }
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketCalculatedLimit, this.id, this.calculatedLimit));
        } else { // nothing to do, won't modify the value
        }
        calculatedLimitStamp(); // I'll stamp even in the 2 cases where modified is false, because the limit has been recalculated and is valid, there's just no reason to update the value

        return gettingModified;
    }

    public synchronized double simpleGetCalculatedLimit() {
        return this.calculatedLimit;
    }

    public synchronized double getCalculatedLimit() {
        final double result;

        if (isCalculatedLimitRecent()) {
            result = this.calculatedLimit;
        } else {
            // I calculated this before, in the managedEvent, during rulesManager loop
            logger.error("failure to calculate limits in getCalculatedLimit for: {}", Generic.objectToString(this));
            result = 0d;
        }

        return result;
    }

    private synchronized boolean updateRunnerExposure(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache) {
        final boolean success;
        for (final ManagedRunner managedRunner : this.runners.values()) {
            managedRunner.resetExposure();
        }

//        final OrderMarket orderMarket = orderCache.getOrderMarket(this.id);
        if (this.orderMarket == null) { // this is a normal branch, no orders are placed on this market
            success = true;
        } else {
            final ArrayList<OrderMarketRunner> orderMarketRunners = this.orderMarket.getOrderMarketRunners();
            if (orderMarketRunners == null) { // this should never happen
                success = false;
                logger.error("null orderMarketRunners in orderMarket during calculateExposure for: {}", Generic.objectToString(this.orderMarket));
            } else {
                @SuppressWarnings("BooleanVariableAlwaysNegated") boolean error = false;
                for (final OrderMarketRunner orderMarketRunner : orderMarketRunners) {
                    final RunnerId runnerId = orderMarketRunner.getRunnerId();
                    if (runnerId == null) {
                        logger.error("null runnerId in orderMarketRunner: {}", Generic.objectToString(orderMarketRunner));
                        error = true;
                        break;
                    } else {
                        final ManagedRunner managedRunner = this.runners.get(runnerId);
                        if (managedRunner == null) {
                            logger.error("null managedRunner for runnerId {} in manageMarket: {}", Generic.objectToString(runnerId), Generic.objectToString(this));
                            error = true;
                            break;
                        } else {
                            managedRunner.processOrders(pendingOrdersThread, orderCache);
                        }
                    }
                } // end for
                // I won't calculate exposure in this method, so nothing to be done on this branch
                success = !error;
            }
        }
        if (success) {
            for (final ManagedRunner managedRunner : this.runners.values()) {
                managedRunner.timeStamp();
            }
        } else { // I probably shouldn't timestamp if the operation was not successful
        }
        return success;
    }

    public synchronized void calculateExposure(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache, final long programStartTime, @NotNull final RulesManager rulesManager) {
        if (this.market != null && isSupported(rulesManager)) {
            final boolean success = updateRunnerExposure(pendingOrdersThread, orderCache);
            this.marketMatchedExposure = Double.NaN;
            this.marketTotalExposure = Double.NaN;
            if (success) {
                this.updateExposureSums();

                for (final ManagedRunner managedRunner : this.runners.values()) {
                    this.marketMatchedExposure = Double.isNaN(this.marketMatchedExposure) ? calculateRunnerMatchedExposure(managedRunner) : Math.max(this.marketMatchedExposure, calculateRunnerMatchedExposure(managedRunner));
                    this.marketTotalExposure = Double.isNaN(this.marketTotalExposure) ? calculateRunnerTotalExposure(managedRunner) : Math.max(this.marketTotalExposure, calculateRunnerTotalExposure(managedRunner));
                } // end for
                if (Double.isNaN(this.marketMatchedExposure)) {
                    this.marketMatchedExposure = 0d;
                }
                if (Double.isNaN(this.marketTotalExposure)) {
                    this.marketTotalExposure = 0d;
                }
            } else { // nothing to do, default Double.NaN values will be retained
            }
        } else { // for not supported I can't calculate the exposure
//            logger.error("trying to calculateExposure on unSupported managedMarket, nothing will be done: {}", Generic.objectToString(this));
            if (Formulas.programHasRecentlyStarted(System.currentTimeMillis(), programStartTime) || isVeryRecent() || !isEnabledMarket()) { // normal
            } else {
                logger.error("trying to calculateExposure on unSupported managedMarket, nothing will be done: {} {} - Market attached: {}", this.id, this.marketName, this.market != null);
            }
        }
    }

    private synchronized void updateExposureSums() { // updates matchedBackExposureSum & totalBackExposureSum
        double localMatchedBackExposureSum = 0d, localTotalBackExposureSum = 0d;
        for (final ManagedRunner managedRunner : this.runners.values()) {
            localMatchedBackExposureSum += managedRunner.getBackMatchedExposure();
            localTotalBackExposureSum += managedRunner.getBackTotalExposure();
        }
        this.setMatchedBackExposureSum(localMatchedBackExposureSum);
        this.setTotalBackExposureSum(localTotalBackExposureSum);
    }

//    private synchronized void updateIdealBackExposureSum() { // updates idealBackExposureSum
//        double idealBackExposureSum = 0d;
//        for (ManagedRunner managedRunner : this.runners.values()) {
//            idealBackExposureSum += managedRunner.getIdealBackExposure();
//        }
//        this.setIdealBackExposureSum(idealBackExposureSum);
//    }

    private synchronized double calculateRunnerMatchedExposure(final @NotNull ManagedRunner managedRunner) {
        final double exposure = managedRunner.getLayMatchedExposure() + this.matchedBackExposureSum - managedRunner.getBackMatchedExposure();
        final RunnerId runnerId = managedRunner.getRunnerId();
        this.runnerMatchedExposure.put(runnerId, exposure);

        return exposure;
    }

    private synchronized double calculateRunnerTotalExposure(final @NotNull ManagedRunner managedRunner) {
        final double exposure = managedRunner.getLayTotalExposure() + this.totalBackExposureSum - managedRunner.getBackTotalExposure();
        final RunnerId runnerId = managedRunner.getRunnerId();
        this.runnerTotalExposure.put(runnerId, exposure);

        return exposure;
    }

    @SuppressWarnings("unused")
    private synchronized double getRunnerMatchedExposure(final RunnerId runnerId) {
        final Double exposureObject = this.runnerMatchedExposure.get(runnerId);
        final double exposure;
        if (exposureObject == null) {
            logger.error("null exposure during getRunnerMatchedExposure for {} in {}", Generic.objectToString(runnerId), Generic.objectToString(this));
            exposure = 0d;
        } else {
            exposure = exposureObject;
        }

        return exposure;
    }

    @SuppressWarnings("unused")
    private synchronized double getRunnerTotalExposure(final RunnerId runnerId) {
        final Double exposureObject = this.runnerTotalExposure.get(runnerId);
        final double exposure;
        if (exposureObject == null) {
            logger.error("null exposure during getRunnerTotalExposure for {} in {}", Generic.objectToString(runnerId), Generic.objectToString(this));
            exposure = 0d;
        } else {
            exposure = exposureObject;
        }

        return exposure;
    }

    private synchronized boolean checkCancelAllUnmatchedBetsFlag(@NotNull final OrdersThreadInterface pendingOrdersThread) { // only runs if the AtomicBoolean flag is set, normally when due to an error I can't calculate exposure
        final boolean shouldRun = this.cancelAllUnmatchedBets.getAndSet(false);
        if (shouldRun) {
            cancelAllUnmatchedBets(pendingOrdersThread);
        } else { // nothing to be done, flag for cancelling is not set
        }
        return shouldRun;
    }

    @SuppressWarnings("OverlyNestedMethod")
    private synchronized int cancelAllUnmatchedBets(@NotNull final OrdersThreadInterface pendingOrdersThread) { // cancel all unmatched bets, don't worry about exposure; generally used when, because of some error, I can't calculate exposure
        int modifications = 0;
        this.cancelAllUnmatchedBets.set(false);
        if (this.orderMarket == null) { // this is a normal branch, no orders are placed on this market, so nothing to be done in this branch
        } else {
            final @NotNull ArrayList<OrderMarketRunner> orderMarketRunners = this.orderMarket.getOrderMarketRunners();
            for (final OrderMarketRunner orderMarketRunner : orderMarketRunners) {
                if (orderMarketRunner == null) {
                    logger.error("null orderMarketRunner in cancelAllUnmatchedBets for: {}", Generic.objectToString(this.orderMarket));
                } else {
                    final RunnerId runnerId = orderMarketRunner.getRunnerId();
                    if (runnerId == null) {
                        logger.error("null runnerId in orderMarketRunner: {}", Generic.objectToString(orderMarketRunner));
                    } else {
                        final @NotNull HashMap<String, Order> unmatchedOrders = orderMarketRunner.getUnmatchedOrders();
                        for (final Order order : unmatchedOrders.values()) {
                            if (order == null) {
                                logger.error("null order in cancelAllUnmatchedBets for: {}", Generic.objectToString(this.orderMarket));
                            } else {
                                final Side side = order.getSide();
                                final Double price = order.getP();
                                final Double size = order.getSr();
                                final String betId = order.getId();
                                if (side == null || price == null || size == null || betId == null) {
                                    logger.error("null order attributes in cancelAllUnmatchedBets for: {} {} {} {} {}", side, price, size, betId, Generic.objectToString(order));
                                } else {
                                    modifications += Generic.booleanToInt(order.cancelOrder(this.id, runnerId, pendingOrdersThread));
                                }
                            }
                        } // end for
                    }
                }
            } // end for
        }
        return modifications;
    }

    public synchronized boolean isSupported(@NotNull final RulesManager rulesManager) {
        final boolean result;
        if (this.market == null) {
            result = false;
//            logger.error("trying to run managedMarket isSupported without attached market for: {}", Generic.objectToString(this));
            logger.info("trying to run managedMarket isSupported without attached market for: {} {}", this.id, this.marketName);
        } else {
            final MarketDefinition marketDefinition = this.market.getMarketDefinition();
            if (marketDefinition == null) {
                result = false;
                logger.error("marketDefinition null while run managedMarket isSupported for: {}", Generic.objectToString(this));
            } else {
                final MarketBettingType marketBettingType = marketDefinition.getBettingType();
                final Integer nWinners = marketDefinition.getNumberOfWinners();
                if (marketBettingType == MarketBettingType.ODDS && nWinners != null && nWinners == 1) {
                    result = true;
                } else {
                    result = false;
                    if (isEnabledMarket()) {
                        logger.info("disabling unsupported managedMarket: {} {} {} {}", this.id, this.marketName, marketBettingType, nWinners);
                        this.setEnabledMarket(false, rulesManager);
                    } else { // normal
                    }
                }
            }
        }
        return result;
    }

//    private synchronized boolean hasMarketBeenManagedRecently() {
//        final long currentTime = System.currentTimeMillis();
//        return currentTime - this.manageMarketStamp > 5_000L;
//    }

    @SuppressWarnings("unused")
    private synchronized void manageMarketStamp() {
        final long currentTime = System.currentTimeMillis();
        manageMarketStamp(currentTime);
    }

    private synchronized void manageMarketStamp(final long currentTime) {
        this.manageMarketStamp = currentTime;
    }

    // check that the limit bets per hour is not reached; only place bets if it's not reached; error message if limit reached; depending on how close to the limit I am, only orders with certain priority will be placed
    // priority depends on the type of modification and on the amount; some urgent orders might be placed in any case
    // manage market timeStamp; recent is 5 seconds; some non urgent actions that add towards hourly order limit will only be done if non recent, and the stamp will only get updated on this branch
    // the solution I found was to set the manageMarketPeriod in the BetFrequencyLimit class, depending on how close to the hourly limit I am
    @SuppressWarnings("OverlyNestedMethod")
    public synchronized void manage(@NotNull final MarketCache marketCache, @NotNull final OrderCache orderCache, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate,
                                    @NotNull final BetFrequencyLimit speedLimit, @NotNull final ExistingFunds safetyLimits, @NotNull final RulesManager rulesManager,
                                    @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        if (this.isEnabledMarket()) {
            final long currentTime = System.currentTimeMillis();
            final long timeSinceLastManageMarketStamp = currentTime - this.manageMarketStamp;
            if (timeSinceLastManageMarketStamp >= speedLimit.getManageMarketPeriod(this.calculatedLimit, safetyLimits)) {
                manageMarketStamp(currentTime);
                attachMarket(marketCache, rulesManager, marketCataloguesMap, programStartTime);
                if (this.market != null) {
                    attachOrderMarket(orderCache);
                    if (isSupported(rulesManager)) {
                        if (checkCancelAllUnmatchedBetsFlag(pendingOrdersThread)) { // all unmatched bets have been canceled already, not much more to be done
                        } else {
//                    final double calculatedLimit = this.getCalculatedLimit();
                            int exposureHasBeenModified = 0;
                            for (final ManagedRunner runner : this.runners.values()) {
                                exposureHasBeenModified += runner.calculateOdds(this.calculatedLimit, pendingOrdersThread, currencyRate, orderCache, marketCache); // also removes unmatched orders at worse odds, and hardToReachOrders
                            }
                            if (exposureHasBeenModified > 0) {
                                calculateExposure(pendingOrdersThread, orderCache, programStartTime, rulesManager);
                                exposureHasBeenModified = 0;
                            } else { // no need to calculateExposure
                            }

                            @NotNull final ArrayList<ManagedRunner> runnersOrderedList = createRunnersOrderedList(marketCache);
                            if (isMarketAlmostLive(rulesManager)) {
                                //noinspection UnusedAssignment
                                exposureHasBeenModified += removeExposure(runnersOrderedList, orderCache, pendingOrdersThread);
                            } else {
                                for (final ManagedRunner runner : this.runners.values()) {
                                    exposureHasBeenModified += runner.checkRunnerLimits(pendingOrdersThread, orderCache);
                                }
                                if (exposureHasBeenModified > 0) {
                                    calculateExposure(pendingOrdersThread, orderCache, programStartTime, rulesManager);
                                    exposureHasBeenModified = 0;
                                } else { // no need to calculateExposure
                                }

                                //noinspection UnusedAssignment
                                exposureHasBeenModified += useTheNewLimit(runnersOrderedList, orderCache, pendingOrdersThread, rulesManager, programStartTime);
                            }
                        }
                    } else { // for not supported I can't calculate the limit
                        logger.error("trying to manage unSupported managedMarket, nothing will be done: {} {}", this.id, this.marketName);
                    }
                } else { // error message was logged elsewhere, nothing to be done
                }
            } else { // not enough time has passed since last manage, nothing to be done
            }
        } else { // not enabled, won't be managed
        }
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
        final ManagedMarket that = (ManagedMarket) obj;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(this.id);
    }
}
