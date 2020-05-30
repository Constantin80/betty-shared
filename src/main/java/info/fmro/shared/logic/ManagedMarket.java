package info.fmro.shared.logic;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.enums.MarketBettingType;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.stream.cache.Utils;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.order.OrderMarket;
import info.fmro.shared.stream.cache.order.OrderMarketRunner;
import info.fmro.shared.stream.definitions.MarketDefinition;
import info.fmro.shared.stream.definitions.Order;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.ComparatorMarketPrices;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import info.fmro.shared.utility.SynchronizedSet;
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
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass", "WeakerAccess", "NonPrivateFieldAccessedInSynchronizedContext", "PackageVisibleField"})
public class ManagedMarket
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedMarket.class);
    private static final long serialVersionUID = -7958840665816144122L;
    public static final long recentCalculatedLimitPeriod = 30_000L;
    public static final long almostLivePeriod = Generic.HOUR_LENGTH_MILLISECONDS;
    public static final long veryRecentPeriod = 10_000L;
    public final AtomicBoolean cancelAllUnmatchedBets = new AtomicBoolean();
    final HashMap<RunnerId, ManagedRunner> runners = new HashMap<>(4); // this is the only place where managedRunners are stored permanently
    private final HashMap<RunnerId, Double> runnerMatchedExposure = new HashMap<>(4), runnerTotalExposure = new HashMap<>(4);
    final String marketId; // marketId
    private String parentEventId;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    String marketName;
    private double amountLimit = -1d; // only has effect if >= 0d
    double calculatedLimit;
    private double marketMatchedExposure = Double.NaN;
    private double marketTotalExposure = Double.NaN;
    private double matchedBackExposureSum;
    private double totalBackExposureSum;
    private long timeMarketGoesLive;
    private long calculatedLimitStamp;
    long manageMarketStamp;
    private boolean marketAlmostLive;
    private boolean enabledMarket = true;
    private final long creationTime;
    private long enabledTime;
    final AtomicBoolean isBeingManaged = new AtomicBoolean();

    @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
    private transient ManagedEvent parentEvent;
    @SuppressWarnings({"InstanceVariableMayNotBeInitializedByReadObject", "FieldAccessedSynchronizedAndUnsynchronized"})
    private transient Market market;
    @Nullable
    @SuppressWarnings("InstanceVariableMayNotBeInitializedByReadObject")
    private transient OrderMarket orderMarket;
//    private transient ArrayList<ManagedRunner> runnersOrderedList = new ArrayList<>(this.runners.values());

    @SuppressWarnings("ConstructorWithTooManyParameters")
    public ManagedMarket(@NotNull final String marketId, @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<String> marketsToCheck,
                         @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final AtomicBoolean rulesHaveChanged,
                         @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        this.marketId = marketId;
        this.creationTime = System.currentTimeMillis();
        this.enabledTime = this.creationTime;
//        this.parentEventId = info.fmro.shared.utility.Formulas.getEventIdOfMarketId(this.id, marketCataloguesMap);
//        this.runnersOrderedList.sort(Comparator.comparing(k -> k.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
        this.setMarketName(Formulas.getMarketCatalogueName(marketId, marketCataloguesMap), listOfQueues);
        attachMarket(marketCache, listOfQueues, marketsToCheck, events, markets, rulesHaveChanged, marketCataloguesMap, programStartTime, false);
    }

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
//        this.runnersOrderedList = new ArrayList<>(this.runners.values());
//        this.runnersOrderedList.sort(Comparator.comparing(k -> k.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
    }

    @NotNull
    synchronized ArrayList<ManagedRunner> createRunnersOrderedList(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache) {
        final ArrayList<ManagedRunner> runnersOrderedList = new ArrayList<>(this.runners.values());
        runnersOrderedList.sort(Comparator.comparing(k -> k.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
        return runnersOrderedList;
    }

    @NotNull
    public synchronized HashMap<RunnerId, ManagedRunner> getRunners() {
        return new HashMap<>(this.runners);
    }

//    private synchronized boolean exposureIsRecent() {
//        final long currentTime = System.currentTimeMillis();
//        return exposureIsRecent(currentTime);
//    }

    synchronized boolean exposureIsRecent(final long currentTime) {
        final boolean isRecent;
        int notRecentCounter = 0;
        for (final ManagedRunner managedRunner : this.runners.values()) {
            if (managedRunner.isRecent(currentTime)) { // no error, nothing to be done
            } else {
                notRecentCounter++;
            }
        }
        if (notRecentCounter == 0) {
            isRecent = true;
        } else {
            isRecent = false;
            logger.info("exposureIsNotRecent {} out of {} for: {}", notRecentCounter, this.runners.size(), this.marketId);
        }
        return isRecent;
    }

    private synchronized boolean isVeryRecent() {
        return isVeryRecent(System.currentTimeMillis());
    }

    private synchronized boolean isVeryRecent(final long currentTime) {
        return currentTime - Math.max(this.creationTime, this.enabledTime) <= veryRecentPeriod;
    }

    public synchronized boolean isEnabledMarket() {
        return this.enabledMarket;
    }

    public synchronized void setEnabledMarket(final boolean enabledMarket, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck,
                                              @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final AtomicBoolean marketsMapModified, @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck) {
        setEnabledMarket(enabledMarket, listOfQueues, marketsToCheck, marketsForOutsideCheck, rulesHaveChanged, marketsMapModified, newMarketsOrEventsForOutsideCheck, true); // default true sendModificationThroughStream
    }

    public synchronized void setEnabledMarket(final boolean enabledMarket, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck,
                                              @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final AtomicBoolean marketsMapModified, @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck, final boolean sendModificationThroughStream) {
        if (this.enabledMarket == enabledMarket) { // no update needed
        } else {
            this.enabledMarket = enabledMarket;
            this.enabledTime = System.currentTimeMillis();
            rulesHaveChanged.set(true);
            marketsMapModified.set(true);
            if (marketsForOutsideCheck.add(this.marketId)) {
                newMarketsOrEventsForOutsideCheck.set(true);
            }
            marketsToCheck.add(this.marketId);

            if (sendModificationThroughStream) {
                listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketEnabled, this.marketId, this.enabledMarket));
            } else { // no need to send this modification
            }
        }
    }

    public synchronized boolean isTwoWayMarket() {
        return getNRunners() == 2;
    }

    public synchronized int getNRunners() {
        return this.runners.size();
    }

    public synchronized String getMarketId() {
        return this.marketId;
    }

    public synchronized String simpleGetMarketName() {
        return this.marketName;
    }

    public synchronized String getMarketName(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                             @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final AtomicBoolean rulesHaveChanged,
                                             @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        if (this.marketName == null) {
            attachMarket(marketCache, listOfQueues, marketsToCheck, events, markets, rulesHaveChanged, marketCataloguesMap, programStartTime);
        } else { // I already have marketName, I'll just return it
        }
        return this.marketName;
    }

    @SuppressWarnings("InstanceVariableUsedBeforeInitialized")
    public final synchronized void setMarketName(final String marketName, @NotNull final ListOfQueues listOfQueues) {
        if (marketName != null && !Objects.equals(this.marketName, marketName)) {
            this.marketName = marketName;
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketName, this.marketId, this.marketName));
        } else { // new value null or same as old value
        }
    }

    private synchronized void setMarketName(final MarketDefinition marketDefinition, @NotNull final ListOfQueues listOfQueues) {
        if (marketDefinition == null) {
            logger.error("null marketDefinition in setMarketName for: {}", Generic.objectToString(this));
        } else {
            if (this.marketName == null) {
                this.marketName = marketDefinition.getMarketType();
                if (this.marketName == null) {
                    logger.error("null marketName from marketDefinition for: {} {}", Generic.objectToString(marketDefinition), Generic.objectToString(this));
                } else {
                    listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketName, this.marketId, this.marketName));
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
            if (marketCataloguesMap.containsKey(this.marketId)) {
                this.parentEventId = Formulas.getEventIdOfMarketId(this.marketId, marketCataloguesMap);
            } else { // no marketCatalogue available, I can't find parentEventId using getEventIdOfMarketId
            }
            if (this.parentEventId == null) {
                if (this.market != null) {
                    this.parentEventId = this.market.getEventId();
                } else { // no Market present either, nothing to be done
                }
            } else { // found it, nothing to be done on this branch
            }

            if (this.parentEventId == null) {
                logger.info("parentEventId not found for managedMarket: {} {}", this.marketId, this.marketName);
            } else {
                rulesHaveChanged.set(true);
            }
        } else { // I already have parentId, nothing to be done
        }

        return this.parentEventId;
    }

    public synchronized ManagedEvent getParentEvent(@NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> markets,
                                                    @NotNull final AtomicBoolean rulesHaveChanged) {
        if (this.parentEvent == null) {
            final String localParentEventId = this.getParentEventId(marketCataloguesMap, rulesHaveChanged);
            if (localParentEventId == null) {
                logger.info("parentEventId && parentEvent null in getParentEvent for: {} {}", this.marketId, this.marketName);
            } else {
                this.parentEvent = events.get(localParentEventId);
                if (this.parentEvent == null) { // I won't create the event here, but outside the method, in the caller, due to potential synchronization problems
                } else {
//            this.parentEvent.addManagedMarket(this);
//                    final String marketId = this.getMarketId();
                    this.parentEvent.marketsMap.putIfAbsent(this.marketId, this, markets);
                    if (this.parentEvent.marketIds.add(this.marketId)) {
                        rulesHaveChanged.set(true);
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
        result = this.parentEvent != null && this.parentEvent.marketIds.contains(this.marketId);

        if (result) { // no error, nothing to be done, will return result
        } else {
            logger.error("false result in parentEventHasTheMarketAdded for: {}", Generic.objectToString(this));
        }

        return result;
    }

    public synchronized boolean parentEventHasTheMarketIdAdded() { // used for testing, to detect bugs
        final boolean result;
        result = this.parentEvent != null && this.parentEvent.marketIds.contains(this.marketId);

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

    public synchronized long getTimeMarketGoesLive(@NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        if (this.marketAlmostLive) { // I won't recalculate
        } else {
            calculateTimeMarketGoesLive(marketsToCheck);
        }
        return this.timeMarketGoesLive;
    }

    public synchronized boolean setTimeMarketGoesLive(final long newTimeMarketGoesLive, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
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
            marketsToCheck.add(this.marketId);
        }
        return modified;
    }

    private synchronized void calculateTimeMarketGoesLive(@NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
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
                    logger.error("null marketTime in calculateTimeMarketGoesLive for: {} {}", this.marketId, Generic.objectToString(marketDefinition));
                    result = 0L;
                }
            }
        }
        this.setTimeMarketGoesLive(result, marketsToCheck);
    }

    private synchronized void calculateTimeMarketGoesLive(final MarketDefinition marketDefinition, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        final long result;
        if (marketDefinition == null) {
            logger.error("null marketDefinition in calculateTimeMarketGoesLive with parameter for: {}", Generic.objectToString(this.market));
            result = 0L;
        } else {
            final Date marketTime = marketDefinition.getMarketTime(); // I hope this is market start time, I'll test
            if (marketTime != null) {
                result = marketTime.getTime();
            } else {
                logger.error("null marketTime in calculateTimeMarketGoesLive with parameter for: {} {}", this.marketId, Generic.objectToString(marketDefinition));
                result = 0L;
            }
        }
        this.setTimeMarketGoesLive(result, marketsToCheck);
    }

    synchronized boolean setAmountLimit(final double newAmountLimit, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final AtomicBoolean rulesHaveChanged,
                                        @NotNull final ExistingFunds safetyLimits) {
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
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketAmountLimit, this.marketId, this.amountLimit));
            rulesHaveChanged.set(true);

            marketsToCheck.add(this.marketId);

            final double maxLimit = getMaxMarketLimit(safetyLimits);
            if (simpleGetCalculatedLimit() > maxLimit) {
                setCalculatedLimit(maxLimit, true, safetyLimits, listOfQueues);
            } else { // no need to update calculatedLimit
            }
        }
        return modified;
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    public synchronized double getSimpleAmountLimit() {
        return this.amountLimit;
    }

    private synchronized ManagedRunner getSecondManagedRunner(@NotNull final ManagedRunner managedRunner) {
        @Nullable final ManagedRunner returnValue;
        @NotNull final List<ManagedRunner> managedRunners = new ArrayList<>(this.runners.values());
        managedRunners.remove(managedRunner);
        if (managedRunners.size() == 1) {
            returnValue = managedRunners.get(0);
        } else {
            logger.error("wrong list size in getSecondManagedRunner: {} {} {} {}", managedRunners.size(), this.marketId, this.marketName, Generic.objectToString(managedRunner.getRunnerId()));
            returnValue = null;
        }
        return returnValue;
    }

    synchronized boolean setRunnerBackAmountLimit(@NotNull final RunnerId runnerId, @NotNull final Double runnerAmountLimit, @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            if (isTwoWayMarket()) {
                @Nullable final ManagedRunner secondManagedRunner = getSecondManagedRunner(managedRunner);
                success = secondManagedRunner != null && managedRunner.setBackAmountLimit(runnerAmountLimit, listOfQueues, rulesHaveChanged) && secondManagedRunner.setLayAmountLimit(runnerAmountLimit, listOfQueues, rulesHaveChanged);
            } else {
                success = managedRunner.setBackAmountLimit(runnerAmountLimit, listOfQueues, rulesHaveChanged);
            }
        } else { // this also covers the case where the element is null, but this should never happen
            logger.error("trying to setRunnerBackAmountLimit on a runner that doesn't exist: {} {} {}", this.marketId, Generic.objectToString(runnerId), runnerAmountLimit);
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerLayAmountLimit(@NotNull final RunnerId runnerId, @NotNull final Double runnerAmountLimit, @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            if (isTwoWayMarket()) {
                @Nullable final ManagedRunner secondManagedRunner = getSecondManagedRunner(managedRunner);
                success = secondManagedRunner != null && managedRunner.setLayAmountLimit(runnerAmountLimit, listOfQueues, rulesHaveChanged) && secondManagedRunner.setBackAmountLimit(runnerAmountLimit, listOfQueues, rulesHaveChanged);
            } else {
                success = managedRunner.setLayAmountLimit(runnerAmountLimit, listOfQueues, rulesHaveChanged);
            }
        } else {
            logger.error("trying to setRunnerLayAmountLimit on a runner that doesn't exist: {} {} {}", this.marketId, Generic.objectToString(runnerId), runnerAmountLimit); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerMaxLayOdds(@NotNull final RunnerId runnerId, @NotNull final Double odds, @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            if (isTwoWayMarket()) {
                @Nullable final ManagedRunner secondManagedRunner = getSecondManagedRunner(managedRunner);
                success = secondManagedRunner != null && managedRunner.setMaxLayOdds(odds, listOfQueues, rulesHaveChanged) && secondManagedRunner.setMinBackOdds(Formulas.inverseOdds(odds, Side.L), listOfQueues, rulesHaveChanged);
            } else {
                success = managedRunner.setMaxLayOdds(odds, listOfQueues, rulesHaveChanged);
            }
        } else {
            logger.error("trying to setRunnerMaxLayOdds on a runner that doesn't exist: {} {} {}", this.marketId, Generic.objectToString(runnerId), odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerMinBackOdds(@NotNull final RunnerId runnerId, @NotNull final Double odds, @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            if (isTwoWayMarket()) {
                @Nullable final ManagedRunner secondManagedRunner = getSecondManagedRunner(managedRunner);
                success = secondManagedRunner != null && managedRunner.setMinBackOdds(odds, listOfQueues, rulesHaveChanged) && secondManagedRunner.setMaxLayOdds(Formulas.inverseOdds(odds, Side.B), listOfQueues, rulesHaveChanged);
            } else {
                success = managedRunner.setMinBackOdds(odds, listOfQueues, rulesHaveChanged);
            }
        } else {
            logger.error("trying to setRunnerMinBackOdds on a runner that doesn't exist: {} {} {}", this.marketId, Generic.objectToString(runnerId), odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized ManagedRunner removeRunner(@NotNull final RunnerId runnerId, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final AtomicBoolean rulesHaveChanged) {
        listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedRunner, this.marketId, runnerId));
        @Nullable final ManagedRunner managedRunner = this.runners.remove(runnerId);
        if (managedRunner != null) {
            rulesHaveChanged.set(true);
            marketsToCheck.add(this.marketId);
        } else {
            logger.error("trying to removeManagedRunner that doesn't exist: {} {}", this.marketId, Generic.objectToString(runnerId)); // this also covers the case where the removed element is null, but this should never happen
        }
        return managedRunner;
    }

    public synchronized boolean addRunner(@NotNull final RunnerId runnerId, @NotNull final ManagedRunner managedRunner, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        return addRunner(runnerId, managedRunner, listOfQueues, marketsToCheck, true); // default sendRunnerThroughStream true
    }

    public synchronized boolean addRunner(@NotNull final RunnerId runnerId, @NotNull final ManagedRunner managedRunner, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                          final boolean sendRunnerThroughStream) {
        final boolean success;
        if (this.runners.containsKey(runnerId) || this.runners.containsValue(managedRunner)) { // already exists, nothing to be done
            final ManagedRunner existingManagedRunner = this.runners.get(runnerId);
            logger.error("trying to add managedRunner over existing one: {} {} {} {} {}", this.marketId, this.marketName, Generic.objectToString(runnerId), Generic.objectToString(existingManagedRunner), Generic.objectToString(managedRunner));
            success = false;
        } else {
            if (sendRunnerThroughStream) {
                listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedRunner, managedRunner));
            } else { // no need to send runner through stream, most likely the entire market object will be sent
            }
            this.runners.put(runnerId, managedRunner); // runners.put needs to be before runnersOrderedList.addAll
            managedRunner.attachRunner(this.market);

            marketsToCheck.add(this.marketId);
            success = true;
        }

        return success;
    }

    private synchronized ManagedRunner addRunner(@NotNull final RunnerId runnerId, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        return addRunner(runnerId, listOfQueues, marketsToCheck, true); // default sendRunnerThroughStream true
    }

    private synchronized ManagedRunner addRunner(@NotNull final RunnerId runnerId, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, final boolean sendRunnerThroughStream) {
        final ManagedRunner returnValue;
        if (this.runners.containsKey(runnerId)) { // already exists, nothing to be done
            returnValue = this.runners.get(runnerId);
        } else { // managedRunner does not exist, I'll generate it; this is done initially, but also later if runners are added
            returnValue = new ManagedRunner(this.marketId, runnerId);
            if (sendRunnerThroughStream) {
                listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedRunner, returnValue));
            } else { // no need to send runner through stream, most likely the entire market object will be sent
            }
            this.runners.put(runnerId, returnValue); // runners.put needs to be before runnersOrderedList.addAll
            returnValue.attachRunner(this.market);

            marketsToCheck.add(this.marketId);

//            this.runnersOrderedList.clear();
//            this.runnersOrderedList.addAll(this.runners.values());
//            this.runnersOrderedList.sort(Comparator.comparing(ManagedRunner.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
        }

        return returnValue;
    }

    public synchronized void updateRunner(final long selectionId, final Double handicap, final double minBackOdds, final double maxLayOdds, final double backAmountLimit, final double layAmountLimit, @NotNull final ListOfQueues listOfQueues,
                                          @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final AtomicBoolean rulesHaveChanged) {
        if (selectionId > 0L) {
            final RunnerId runnerId = new RunnerId(selectionId, handicap);
            final ManagedRunner managedRunner = this.addRunner(runnerId, listOfQueues, marketsToCheck);
            managedRunner.update(minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit, marketsToCheck, listOfQueues, rulesHaveChanged);
        } else {
            logger.error("bogus selectionId in updateRunner for: {} {} {} {} {} {} {}", this.marketId, selectionId, handicap, minBackOdds, maxLayOdds, backAmountLimit, layAmountLimit);
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

    public synchronized Market simpleGetMarket() {
        return this.market;
    }

    public synchronized Market getMarket(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                         @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final AtomicBoolean rulesHaveChanged,
                                         @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        attachMarket(marketCache, listOfQueues, marketsToCheck, events, markets, rulesHaveChanged, marketCataloguesMap, programStartTime);
        return this.market;
    }

    public final synchronized boolean attachMarket(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                                   @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final AtomicBoolean rulesHaveChanged,
                                                   @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        return attachMarket(marketCache, listOfQueues, marketsToCheck, events, markets, rulesHaveChanged, marketCataloguesMap, programStartTime, true); // default sendRunnerThroughStream true
    }

    public final synchronized boolean attachMarket(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                                   @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final AtomicBoolean rulesHaveChanged,
                                                   @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime, final boolean sendRunnerThroughStream) {
        // this is run periodically, as it's contained in the manage method, that is run periodically
        final boolean newMarketAttached;
        if (this.market == null) {
            this.market = marketCache.get(this.marketId);
            newMarketAttached = this.market != null;
        } else { // I already have the market, nothing to be done
            newMarketAttached = false;
        }
        if (this.market == null) {
            if (Formulas.programHasRecentlyStarted(System.currentTimeMillis(), programStartTime) || isVeryRecent()) { // normal
            } else {
                logger.info("no market found in MarketCache, probably old expired market, for: {} {}", this.marketId, this.marketName); // this happens for manager markets that are old and no longer exist on the site
            }
        } else {
            for (final ManagedRunner managedRunner : this.runners.values()) {
                managedRunner.attachRunner(this.market);
            }
            if (Generic.programName.get() == ProgramName.SERVER) {
                final HashSet<RunnerId> runnerIds = this.market.getRunnerIds();
                for (final RunnerId runnerId : runnerIds) {
                    if (runnerId != null) {
                        addRunner(runnerId, listOfQueues, marketsToCheck, sendRunnerThroughStream); // only adds if doesn't exist
                    } else {
                        logger.error("null runnerId for orderMarket: {}", Generic.objectToString(this.market));
                    }
                } // end for
            } else { // will only auto add runners on the server, and the server will send them to the client, else I end up adding them twice
            }
            setMarketName(this.market.getMarketDefinition(), listOfQueues);
        }
//        getParentEventId(marketCataloguesMap, rulesManager.rulesHaveChanged);
        getParentEvent(marketCataloguesMap, events, markets, rulesHaveChanged); // includes getParentEventId
        return newMarketAttached;
    }

    public synchronized void attachOrderMarket(@NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final ListOfQueues listOfQueues,
                                               @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> markets,
                                               @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final long programStartTime) {
        // this is run periodically, as it's contained in the manage method, that is run periodically
        if (this.orderMarket == null) {
            this.orderMarket = orderCache.get(this.marketId);
        } else { // I already have the market, nothing to be done on this branch
        }
        if (this.orderMarket == null) { // normal, it means no orders exist for this managedMarket, nothing else to be done
        } else {
            final long currentTime = System.currentTimeMillis();
            for (final ManagedRunner managedRunner : this.runners.values()) {
                managedRunner.attachOrderRunner(this.orderMarket, false, currentTime);
            }
            final HashSet<RunnerId> runnerIds = this.orderMarket.getRunnerIds();
            for (final RunnerId runnerId : runnerIds) {
                if (runnerId != null) {
                    if (this.runners.containsKey(runnerId)) { // already exists, nothing to be done
                    } else { // managedRunner does not exist
                        final boolean newMarketAttached = attachMarket(marketCache, listOfQueues, marketsToCheck, events, markets, rulesHaveChanged, marketCataloguesMap, programStartTime, true);
                        if (newMarketAttached) {
                            logger.info("managedRunner does not exist, but newMarketAttached, for existing orderMarketRunner: {} {}", this.marketId, Generic.objectToString(runnerId));
                        } else if (this.market == null) {
                            logger.info("managedRunner does not exist, and there's no attached market, for existing orderMarketRunner: {} {}", this.marketId, Generic.objectToString(runnerId));
                        } else { // this.market != null && !newMarketAttached
                            logger.error("managedRunner does not exist for existing orderMarketRunner: {} - {} - {}", Generic.objectToString(runnerId), Generic.objectToString(this.runners), Generic.objectToString(this));
                        }
                    }
                } else {
                    logger.error("null runnerId for orderMarket: {}", Generic.objectToString(this.orderMarket));
                }
            } // end for
        }
    }

    synchronized boolean isMarketAlmostLive(@NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        if (this.marketAlmostLive) { // already almostLive, won't recheck
        } else {
            final MarketDefinition marketDefinition = this.market.getMarketDefinition();
            final Boolean inPlay = marketDefinition.getInPlay();
            if (inPlay != null && inPlay) {
                this.marketAlmostLive = inPlay; // inPlay == true
                logger.info("managed market {} is almost live inPlay {}", this.marketId, inPlay);
            } else {
                calculateTimeMarketGoesLive(marketDefinition, marketsToCheck);
                final long currentTime = System.currentTimeMillis();
                final long timeGoesLive = getTimeMarketGoesLive(marketsToCheck);
                if (currentTime + almostLivePeriod >= timeGoesLive) {
                    this.marketAlmostLive = true;
                    logger.info("managed market {} is almost live: {} {} minimum:{}s {} current:{}s", this.marketId, this.marketAlmostLive, timeGoesLive, Generic.addCommas(almostLivePeriod / 1_000), currentTime,
                                Generic.addCommas((timeGoesLive - currentTime) / 1_000));
                }
            }
//            if (this.marketAlmostLive) {
//                logger.info("managed market is almost live: {} {}", this.marketId, this.marketName);
//            }
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

    private synchronized boolean checkTwoWayMarketLimitsValid(@NotNull final ArrayList<? extends ManagedRunner> runnersOrderedList) {
        // maxBackLimit on one runner needs to be equal to maxLayLimit on the other, and vice-versa; same with odds being usable or unusable; max odds on back roughly inversely proportional to lay on the other runner, and vice-versa
        final boolean isValid;
        if (runnersOrderedList.size() == 2) {
            final ManagedRunner firstRunner = runnersOrderedList.get(0);
            final ManagedRunner secondRunner = runnersOrderedList.get(1);
            if (firstRunner == null || secondRunner == null) {
                logger.error("null runner in checkTwoWayMarketLimitsValid for: {} {} {} {}", this.marketId, this.marketName, Generic.objectToString(firstRunner), Generic.objectToString(secondRunner));
                isValid = false;
            } else {
                final double firstBackAmountLimit = firstRunner.getBackAmountLimit(), secondBackAmountLimit = secondRunner.getBackAmountLimit(), firstLayAmountLimit = firstRunner.getLayAmountLimit(), secondLayAmountLimit = secondRunner.getLayAmountLimit();
                //noinspection FloatingPointEquality
                if (firstBackAmountLimit != secondLayAmountLimit || secondBackAmountLimit != firstLayAmountLimit) {
                    logger.error("not equal amountLimits in checkTwoWayMarketLimitsValid for: {} {} {} {} {} {}", this.marketId, this.marketName, firstBackAmountLimit, secondBackAmountLimit, firstLayAmountLimit, secondLayAmountLimit);
                    isValid = false;
                } else {
                    final double firstMinBackOdds = firstRunner.getMinBackOdds(), secondMinBackOdds = secondRunner.getMinBackOdds(), firstMaxLayOdds = firstRunner.getMaxLayOdds(), secondMaxLayOdds = secondRunner.getMaxLayOdds();
                    if (Formulas.oddsAreUsable(firstMinBackOdds) != Formulas.oddsAreUsable(secondMaxLayOdds) ||
                        Formulas.oddsAreUsable(secondMinBackOdds) != Formulas.oddsAreUsable(firstMaxLayOdds)) {
                        logger.error("not equal oddsAreUsable in checkTwoWayMarketLimitsValid for: {} {} {} {} {} {}", this.marketId, this.marketName, firstMinBackOdds, secondMinBackOdds, firstMaxLayOdds, secondMaxLayOdds);
                        isValid = false;
                    } else {
                        if (Formulas.orderedOddsAreInverse(firstMinBackOdds, secondMaxLayOdds) && Formulas.orderedOddsAreInverse(secondMinBackOdds, firstMaxLayOdds)) {
                            isValid = true;
                        } else {
                            logger.error("odds are not inverse in checkTwoWayMarketLimitsValid for: {} {} {} {} {} {}", this.marketId, this.marketName, firstMinBackOdds, secondMinBackOdds, firstMaxLayOdds, secondMaxLayOdds);
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

    private synchronized int balanceTwoRunnerMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final List<Side> sideList, final double excessMatchedExposure,
                                                    @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        int modifications = 0;
        if (sideList.size() != 2 || sideList.contains(null)) {
            logger.error("bogus sideList for balanceTwoRunnerMarket: {} {} {} {}", Generic.objectToString(sideList), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), excessMatchedExposure);
        } else {
            @NotNull final Side firstSide = sideList.get(0), secondSide = sideList.get(1);
            if (firstSide == Side.B && secondSide == Side.L) {
                modifications += firstRunner.cancelUnmatched(Side.L, pendingOrdersThread, orderCache);
                modifications += secondRunner.cancelUnmatched(Side.B, pendingOrdersThread, orderCache);
                @NotNull final List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sideList, excessMatchedExposure);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstRunner.cancelUnmatchedAmounts(-firstExposureToBePlaced, 0d, pendingOrdersThread, orderCache);
                } else {
                    if (firstRunner.placeOrder(Side.B, firstRunner.getToBeUsedBackOdds(), firstExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondRunner.cancelUnmatchedAmounts(0d, -secondExposureToBePlaced, pendingOrdersThread, orderCache);
                } else {
                    if (secondRunner.placeOrder(Side.L, secondRunner.getToBeUsedLayOdds(), secondExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
            } else if (firstSide == Side.L && secondSide == Side.B) {
                modifications += firstRunner.cancelUnmatched(Side.B, pendingOrdersThread, orderCache);
                modifications += secondRunner.cancelUnmatched(Side.L, pendingOrdersThread, orderCache);
                @NotNull final List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sideList, excessMatchedExposure);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstRunner.cancelUnmatchedAmounts(0d, -firstExposureToBePlaced, pendingOrdersThread, orderCache);
                } else {
                    if (firstRunner.placeOrder(Side.L, firstRunner.getToBeUsedLayOdds(), firstExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondRunner.cancelUnmatchedAmounts(-secondExposureToBePlaced, 0d, pendingOrdersThread, orderCache);
                } else {
                    if (secondRunner.placeOrder(Side.B, secondRunner.getToBeUsedBackOdds(), secondExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
            } else {
                logger.error("bogus sides for balanceTwoRunnerMarket: {} {} {} {}", Generic.objectToString(sideList), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), excessMatchedExposure);
            }
        }
        return modifications;
    }

    private synchronized int useNewLimitOnTwoRunnerMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final List<Side> sideList, final double availableLimit,
                                                          @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        int modifications = 0;
        if (sideList.size() == 2) {
            @NotNull final Side firstSide = sideList.get(0), secondSide = sideList.get(1);
            if (firstSide == Side.B && secondSide == Side.L) {
                @NotNull final List<Double> exposuresToBePlaced = Utils.getAmountsToBePlacedForTwoWayMarket(firstRunner, secondRunner, sideList, availableLimit);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstRunner.cancelUnmatchedAmounts(-firstExposureToBePlaced, 0d, pendingOrdersThread, orderCache);
                } else {
                    if (firstRunner.placeOrder(Side.B, firstRunner.getToBeUsedBackOdds(), firstExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondRunner.cancelUnmatchedAmounts(0d, -secondExposureToBePlaced, pendingOrdersThread, orderCache);
                } else {
                    if (secondRunner.placeOrder(Side.L, secondRunner.getToBeUsedLayOdds(), secondExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
            } else if (firstSide == Side.L && secondSide == Side.B) {
                @NotNull final List<Double> exposuresToBePlaced = Utils.getAmountsToBePlacedForTwoWayMarket(firstRunner, secondRunner, sideList, availableLimit);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstRunner.cancelUnmatchedAmounts(0d, -firstExposureToBePlaced, pendingOrdersThread, orderCache);
                } else {
                    if (firstRunner.placeOrder(Side.L, firstRunner.getToBeUsedLayOdds(), firstExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondRunner.cancelUnmatchedAmounts(-secondExposureToBePlaced, 0d, pendingOrdersThread, orderCache);
                } else {
                    if (secondRunner.placeOrder(Side.B, secondRunner.getToBeUsedBackOdds(), secondExposureToBePlaced, pendingOrdersThread) > 0d) {
                        modifications++;
                    }
                }
            } else {
                logger.error("bogus sides for useNewLimitOnTwoRunnerMarket: {} {} {} {}", Generic.objectToString(sideList), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), availableLimit);
            }
        } else {
            logger.error("bogus sideList for useNewLimitOnTwoRunnerMarket: {} {} {} {}", Generic.objectToString(sideList), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), availableLimit);
        }
        return modifications;
    }

    synchronized int removeExposure(@NotNull final ArrayList<? extends ManagedRunner> runnersOrderedList, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, @NotNull final OrdersThreadInterface pendingOrdersThread) {
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
//                    final long currentTime = System.currentTimeMillis();
                    final ManagedRunner firstRunner = runnersOrderedList.get(0), secondRunner = runnersOrderedList.get(1);
                    final double backLayMatchedExposure = firstRunner.getBackMatchedExposure() + secondRunner.getLayMatchedExposure(), layBackMatchedExposure = firstRunner.getLayMatchedExposure() + secondRunner.getBackMatchedExposure(),
                            excessMatchedExposure = Math.abs(backLayMatchedExposure - layBackMatchedExposure);
//                    final OrderMarketRunner firstOrderRunner = firstRunner.getOrderMarketRunner(orderCache, currentTime), secondOrderRunner = secondRunner.getOrderMarketRunner(orderCache, currentTime);
//                    if (firstOrderRunner == null || secondOrderRunner == null) {
//                        logger.error("null OrderMarketRunner during removeExposure for: {} {} {}", Generic.objectToString(firstOrderRunner), Generic.objectToString(secondOrderRunner), Generic.objectToString(this));
//                        modifications += cancelAllUnmatchedBets(pendingOrdersThread);
//                    } else
                    if (excessMatchedExposure < .1d) {
                        modifications += cancelAllUnmatchedBets(pendingOrdersThread);
                    } else if (backLayMatchedExposure > layBackMatchedExposure) {
                        // I'll use unmatched exposure, equal to excessMatchedExposure, on lay/back
                        modifications += balanceTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.L, Side.B), excessMatchedExposure, pendingOrdersThread, orderCache);
                    } else { // backLayMatchedExposure < layBackMatchedExposure
                        // I'll use unmatched exposure, equal to excessMatchedExposure, on back/lay
                        modifications += balanceTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.B, Side.L), excessMatchedExposure, pendingOrdersThread, orderCache);
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

    @SuppressWarnings("OverlyNestedMethod")
    synchronized int useTheNewLimit(@NotNull final ArrayList<? extends ManagedRunner> runnersOrderedList, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, @NotNull final OrdersThreadInterface pendingOrdersThread,
                                    @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final ListOfQueues listOfQueues, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck,
                                    @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final AtomicBoolean marketsMapModified, @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck,
                                    @NotNull final AtomicLong orderCacheInitializedFromStreamStamp, final long programStartTime) {
        int modifications = 0;
        if (Double.isNaN(this.marketTotalExposure)) {
            logger.error("marketTotalExposure not initialized in useTheNewLimit for: {}", Generic.objectToString(this));
        } else {
            // variant of removeExposure, or the other way around ... major difference is that in this case the overall calculatedLimit matters, this time it's not about individual runners
//            final long currentTime = System.currentTimeMillis();
            final int size = runnersOrderedList.size();
            if (size == 2) { // special case
                // this one is easy, most similar to the removeExposure situation
                // the factors for splitting the unmatched exposure between runners are the price of toBeUsedOdds, which of the toBeUsedOdds is more profitable, and the size of existing unmatched bets
                // also lay bets should be given slight priority over back bets, as other gamblers like to back rather than lay
                if (checkTwoWayMarketLimitsValid(runnersOrderedList)) {
                    final ManagedRunner firstRunner = runnersOrderedList.get(0), secondRunner = runnersOrderedList.get(1);
                    final double backLayExposure = firstRunner.getBackTotalExposure() + secondRunner.getLayTotalExposure(), layBackExposure = firstRunner.getLayTotalExposure() + secondRunner.getBackTotalExposure();
                    final double availableBackLayLimit = this.calculatedLimit - backLayExposure, availableLayBackLimit = this.calculatedLimit - layBackExposure;
//                    final OrderMarketRunner firstOrderRunner = firstRunner.getOrderMarketRunner(orderCache, currentTime), secondOrderRunner = secondRunner.getOrderMarketRunner(orderCache, currentTime);
//                    if (firstOrderRunner == null || secondOrderRunner == null) {
//                        logger.error("null OrderMarketRunner during useTheNewLimit for: {} {} {}", Generic.objectToString(firstOrderRunner), Generic.objectToString(secondOrderRunner), Generic.objectToString(this));
//                        modifications += cancelAllUnmatchedBets(pendingOrdersThread);
//                    } else {
                    if (availableBackLayLimit == 0d) { // availableLimit is 0d, nothing to be done
                    } else {
                        modifications += useNewLimitOnTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.L, Side.B), availableBackLayLimit, pendingOrdersThread, orderCache);
                    }
                    if (availableLayBackLimit == 0d) { // availableLimit is 0d, nothing to be done
                    } else {
                        modifications += useNewLimitOnTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.B, Side.L), availableLayBackLimit, pendingOrdersThread, orderCache);
                    }
//                    }
                } else { // if not valid, error message and take action, with all order canceling
                    logger.error("checkTwoWayMarketLimitsValid false in useTheNewLimit for: {} {}", this.marketId, this.marketName);
                    modifications += cancelAllUnmatchedBets(pendingOrdersThread);
                }
            } else {
                // calculate the splitting proportion for each runner: the factors for splitting the unmatched exposure between runners are the price of toBeUsedOdds, and the size of existing unmatched bets
                // total exposure only exists as total back exposure on each runner, and can be increased by lay on that runner and back on other runners
                // each runner has total back exposure, and ideally it should be equal to the calculatedLimit, but has to obey the per runner limits as well

                calculateIdealBackExposureList(runnersOrderedList, marketsToCheck);

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
                                    if (pendingOrdersThread.addPlaceOrder(this.marketId, managedRunner.getRunnerId(), Side.B, managedRunner.getToBeUsedBackOdds(), amountToPlaceOnBack) > 0d) {
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
//                                    final OrderMarketRunner orderMarketRunner = managedRunner.getOrderMarketRunner(orderCache, currentTime);
//                                    if (orderMarketRunner == null) {
//                                        logger.error("null orderMarketRunner while positive amountToRemoveFromBack for: {} {} {}", amountToRemoveFromBack, Generic.objectToString(managedRunner), Generic.objectToString(this));
//                                    } else {
                                    modifications += managedRunner.balanceTotalAmounts(amountToRemoveFromBack, 0d, pendingOrdersThread, orderCache);
//                                    }
                                } else { // amount negative, won't remove anything
                                }
                            } // end for
                        }
                    } else { // potential lay bets are removed after the conditional, nothing to be done here
                    }
                }
                if (modifications > 0) {
                    calculateExposure(pendingOrdersThread, orderCache, programStartTime, listOfQueues, marketsToCheck, marketsForOutsideCheck, rulesHaveChanged, marketsMapModified, newMarketsOrEventsForOutsideCheck, orderCacheInitializedFromStreamStamp);
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
                            if (pendingOrdersThread.addPlaceOrder(this.marketId, managedRunner.getRunnerId(), Side.L, toBeUsedLayOdds, amountToPlaceOnLay) > 0d) {
                                modifications++;
                            } else { // no modification made, nothing to be done
                            }
                        } else { // odds unusable, nothing to be done
                        }
                    } else if (minimumAvailableLimit < -.1d) {
//                        final OrderMarketRunner orderMarketRunner = managedRunner.getOrderMarketRunner(orderCache, currentTime);
//                        if (orderMarketRunner == null) {
//                            logger.error("null orderMarketRunner while negative minimumAvailableLimit for: {} {} {}", minimumAvailableLimit, Generic.objectToString(managedRunner), Generic.objectToString(this));
//                        } else {
                        modifications += managedRunner.balanceTotalAmounts(0d, -minimumAvailableLimit, pendingOrdersThread, orderCache);
//                        }
                    } else { // difference too small, nothing to be done
                    }
                }
            }
        }
        return modifications;
    }

    private synchronized double calculateAvailableIdealBackExposureSum(@NotNull final Iterable<? extends ManagedRunner> runnersOrderedList) { // I will only add the positive amounts
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

    private synchronized double calculateExcessiveBackExposureOverIdealSum(@NotNull final Iterable<? extends ManagedRunner> runnersOrderedList) { // I will only add the positive amounts
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

    private synchronized void calculateProportionOfMarketLimitPerRunnerList(@NotNull final Collection<? extends ManagedRunner> runnersOrderedList, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        // calculated proportions depend on the toBeUsedBackOdds
        final double sumOfStandardAmounts = runnersOrderedList.stream().filter(x -> info.fmro.shared.utility.Formulas.oddsAreUsable(x.getToBeUsedBackOdds())).mapToDouble(x -> 1d / (x.getToBeUsedBackOdds() - 1d)).sum();
        for (final ManagedRunner managedRunner : runnersOrderedList) { // sumOfStandardAmounts should always be != 0d if at least one oddsAreUsable
            final double proportion = info.fmro.shared.utility.Formulas.oddsAreUsable(managedRunner.getToBeUsedBackOdds()) ? 1d / (managedRunner.getToBeUsedBackOdds() - 1d) / sumOfStandardAmounts : 0d;
            managedRunner.setProportionOfMarketLimitPerRunner(proportion, marketsToCheck);
        }
    }

    private synchronized void calculateIdealBackExposureList(@NotNull final ArrayList<? extends ManagedRunner> runnersOrderedList, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        calculateProportionOfMarketLimitPerRunnerList(runnersOrderedList, marketsToCheck);
        // reset idealBackExposure
        for (final ManagedRunner managedRunner : runnersOrderedList) {
            managedRunner.setIdealBackExposure(0d, marketsToCheck);
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
                    final double assignedExposure = managedRunner.addIdealBackExposure(idealExposure, marketsToCheck);
                    exposureLeftToBeAssigned -= assignedExposure;
                    if (assignedExposure < idealExposure || assignedExposure == 0d) {
                        runnersToRemove.add(managedRunner);
                    } else { // all exposure has been added, this runner might still be usable in further iterations of the while loop
                    }
                }
            } else {//non positive totalProportionSumForRemainingRunners 0.0 for: 584.3413960020026 0 20 1.162659886 Winner
                if (totalProportionSumForRemainingRunners == 0d) { // normal when no managed runners exist on market
                } else {
                    logger.error("negative totalProportionSumForRemainingRunners {} for: {} {} {} {} {}", totalProportionSumForRemainingRunners, exposureLeftToBeAssigned, whileCounter, runnersThatCanStillBeAssignedExposure.size(), this.marketId,
                                 this.marketName);
                }
                runnersThatCanStillBeAssignedExposure.clear();
                break;
            }

            runnersThatCanStillBeAssignedExposure.removeAll(runnersToRemove);
            whileCounter++;
        }
        if (exposureLeftToBeAssigned >= .1d && !runnersThatCanStillBeAssignedExposure.isEmpty()) {
            logger.error("runnersThatCanStillBeAssignedExposure not empty: {} {} {} {} {}", whileCounter, exposureLeftToBeAssigned, runnersThatCanStillBeAssignedExposure.size(), this.marketId, this.marketName);
        } else { // no error, nothing to print
        }
//        updateIdealBackExposureSum();
    }

    public synchronized double getMaxMarketLimit(@NotNull final ExistingFunds safetyLimits) {
        final double result;
        final double safetyLimit = safetyLimits.getDefaultMarketLimit(this.marketId);
        result = this.amountLimit >= 0d ? Math.min(this.amountLimit, safetyLimit) : safetyLimit;
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
            logger.error("currentTime {} is not greater than calculatedLimitStamp {} and difference is: {} for: {}", currentTime, this.calculatedLimitStamp, currentTime - this.calculatedLimitStamp, this.marketId);
        }
    }

    public synchronized boolean setCalculatedLimit(final double newLimit, final boolean limitCanBeIncreased, @NotNull final ExistingFunds safetyLimits, @NotNull final ListOfQueues listOfQueues) {
        final boolean modified;
        modified = (limitCanBeIncreased || newLimit < this.calculatedLimit) && setCalculatedLimit(newLimit, safetyLimits, listOfQueues);
        return modified;
    }

    private synchronized boolean setCalculatedLimit(final double newLimit, @NotNull final ExistingFunds safetyLimits, @NotNull final ListOfQueues listOfQueues) {
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
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketCalculatedLimit, this.marketId, this.calculatedLimit));
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

    private synchronized boolean updateRunnerExposure(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        final boolean success;
        for (final ManagedRunner managedRunner : this.runners.values()) {
            managedRunner.resetExposure();
        }

//        final OrderMarket orderMarket = orderCache.getOrderMarket(this.id);
        if (this.orderMarket == null) { // this is a normal branch, no orders are placed on this market
            for (final ManagedRunner managedRunner : this.runners.values()) {
                managedRunner.getTempExposure(pendingOrdersThread);
            }
            success = true;
        } else {
            final ArrayList<OrderMarketRunner> orderMarketRunners = this.orderMarket.getOrderMarketRunners();
            if (orderMarketRunners == null) { // this should never happen
                success = false;
                logger.error("null orderMarketRunners in orderMarket during calculateExposure for: {}", Generic.objectToString(this.orderMarket));
            } else {
                @SuppressWarnings("BooleanVariableAlwaysNegated") boolean error = false;
                final HashMap<RunnerId, ManagedRunner> localRunners = new HashMap<>(this.runners);
                for (final OrderMarketRunner orderMarketRunner : orderMarketRunners) {
                    final RunnerId runnerId = orderMarketRunner.getRunnerId();
                    if (runnerId == null) {
                        logger.error("null runnerId in orderMarketRunner: {}", Generic.objectToString(orderMarketRunner));
                        error = true;
                        break;
                    } else {
                        final ManagedRunner managedRunner = localRunners.remove(runnerId);
                        if (managedRunner == null) {
                            logger.error("null managedRunner for runnerId {} in manageMarket: {}", Generic.objectToString(runnerId), Generic.objectToString(this));
                            error = true;
                            break;
                        } else {
                            managedRunner.processOrders(pendingOrdersThread, orderCache);
                        }
                    }
                } // end for
                if (localRunners.isEmpty()) { // normal case, nothing to be done
                } else {
                    logger.error("managedRunners without orderMarketRunner in updateRunnerExposure: {} {} {} {} {} {}", localRunners.size(), Generic.objectToString(localRunners.keySet()), orderMarketRunners.size(),
                                 Generic.objectToString(orderMarketRunners), this.runners.size(), Generic.objectToString(this.runners.keySet()));
                    for (final ManagedRunner managedRunner : localRunners.values()) {
                        managedRunner.getTempExposure(pendingOrdersThread);
                    }
                }
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

    synchronized boolean exposureCanBeCalculated(@NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck,
                                                 @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final AtomicBoolean marketsMapModified, @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck,
                                                 @NotNull final AtomicLong orderCacheInitializedFromStreamStamp, final long programStartTime) {
        final long orderCacheStamp = orderCacheInitializedFromStreamStamp.get();
        final long currentTime = System.currentTimeMillis();
        final boolean returnValue = this.market != null && orderCacheStamp > 0L && isSupported(listOfQueues, marketsToCheck, marketsForOutsideCheck, rulesHaveChanged, marketsMapModified, newMarketsOrEventsForOutsideCheck);

        if (returnValue) { // no error, no message to print
        } else if (Formulas.programHasRecentlyStarted(currentTime, programStartTime) || isVeryRecent() || !isEnabledMarket()) { // normal
        } else if (orderCacheStamp <= 0) {
            logger.info("trying to calculateExposure on orderCacheStamp {}, nothing will be done: {} {} - Market attached: {}", orderCacheStamp, this.marketId, this.marketName, this.market != null);
        } else if (this.market == null) {
            logger.warn("trying to calculateExposure on managedMarket with no Market attached, nothing will be done: {} {}", this.marketId, this.marketName);
        } else {
            logger.error("trying to calculateExposure on unSupported managedMarket, nothing will be done: {} {}", this.marketId, this.marketName);
        }
        return returnValue;
    }

    public synchronized void calculateExposure(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, final long programStartTime,
                                               @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck,
                                               @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final AtomicBoolean marketsMapModified, @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck,
                                               @NotNull final AtomicLong orderCacheInitializedFromStreamStamp) {
        if (exposureCanBeCalculated(listOfQueues, marketsToCheck, marketsForOutsideCheck, rulesHaveChanged, marketsMapModified, newMarketsOrEventsForOutsideCheck, orderCacheInitializedFromStreamStamp, programStartTime)) {
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
        } else { // for not supported I can't calculate the exposure; potential log messages have been written already in the exposureCanBeCalculated method
//            logger.error("trying to calculateExposure on unSupported managedMarket, nothing will be done: {}", Generic.objectToString(this));
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

    private synchronized double calculateRunnerMatchedExposure(@NotNull final ManagedRunner managedRunner) {
        final double exposure = managedRunner.getLayMatchedExposure() + this.matchedBackExposureSum - managedRunner.getBackMatchedExposure();
        final RunnerId runnerId = managedRunner.getRunnerId();
        this.runnerMatchedExposure.put(runnerId, exposure);

        return exposure;
    }

    private synchronized double calculateRunnerTotalExposure(@NotNull final ManagedRunner managedRunner) {
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

    synchronized boolean checkCancelAllUnmatchedBetsFlag(@NotNull final OrdersThreadInterface pendingOrdersThread) { // only runs if the AtomicBoolean flag is set, normally when due to an error I can't calculate exposure
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
            @NotNull final ArrayList<OrderMarketRunner> orderMarketRunners = this.orderMarket.getOrderMarketRunners();
            for (final OrderMarketRunner orderMarketRunner : orderMarketRunners) {
                if (orderMarketRunner == null) {
                    logger.error("null orderMarketRunner in cancelAllUnmatchedBets for: {}", Generic.objectToString(this.orderMarket));
                } else {
                    final RunnerId runnerId = orderMarketRunner.getRunnerId();
                    if (runnerId == null) {
                        logger.error("null runnerId in orderMarketRunner: {}", Generic.objectToString(orderMarketRunner));
                    } else {
                        @NotNull final HashMap<String, Order> unmatchedOrders = orderMarketRunner.getUnmatchedOrders();
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
                                    modifications += Generic.booleanToInt(order.cancelOrder(this.marketId, runnerId, pendingOrdersThread));
                                }
                            }
                        } // end for
                    }
                }
            } // end for
        }
        return modifications;
    }

    public synchronized boolean isSupported(@NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck,
                                            @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final AtomicBoolean marketsMapModified, @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck) {
        final boolean result;
        if (this.market == null) {
            result = false;
//            logger.error("trying to run managedMarket isSupported without attached market for: {}", Generic.objectToString(this));
            logger.info("trying to run managedMarket isSupported without attached market for: {} {}", this.marketId, this.marketName);
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
                        logger.info("disabling unsupported managedMarket: {} {} {} {}", this.marketId, this.marketName, marketBettingType, nWinners);
                        this.setEnabledMarket(false, listOfQueues, marketsToCheck, marketsForOutsideCheck, rulesHaveChanged, marketsMapModified, newMarketsOrEventsForOutsideCheck);
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

    synchronized void manageMarketStamp(final long currentTime) {
        this.manageMarketStamp = currentTime;
    }

    // check that the limit bets per hour is not reached; only place bets if it's not reached; error message if limit reached; depending on how close to the limit I am, only orders with certain priority will be placed
    // priority depends on the type of modification and on the amount; some urgent orders might be placed in any case
    // manage market timeStamp; recent is 5 seconds; some non urgent actions that add towards hourly order limit will only be done if non recent, and the stamp will only get updated on this branch
    // the solution I found was to set the manageMarketPeriod in the BetFrequencyLimit class, depending on how close to the hourly limit I am
    public void manage(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, @NotNull final OrdersThreadInterface pendingOrdersThread,
                       @NotNull final AtomicDouble currencyRate, @NotNull final BetFrequencyLimit speedLimit, @NotNull final ExistingFunds safetyLimits, @NotNull final ListOfQueues listOfQueues,
                       @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck, @NotNull final AtomicBoolean rulesHaveChanged,
                       @NotNull final AtomicBoolean marketsMapModified, @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck, @NotNull final ManagedEventsMap events, @NotNull final SynchronizedMap<String, ManagedMarket> markets,
                       @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final AtomicBoolean mustStop, @NotNull final AtomicLong orderCacheInitializedFromStreamStamp,
                       final long programStartTime) {
        // intentionally not synchronized; isBeingManaged AtomicBoolean will be used to make sure it only runs once
        if (this.isEnabledMarket()) {
            final boolean previousValue = this.isBeingManaged.getAndSet(true);
            if (previousValue) { // was already beingManaged, another thread manages the market right now, this one will exit
            } else { // market was not being managed, I'll manage it now
                final ManagedMarketThread managedMarketThread =
                        new ManagedMarketThread(this, marketCache, orderCache, pendingOrdersThread, currencyRate, speedLimit, safetyLimits, listOfQueues, marketsToCheck, marketsForOutsideCheck, rulesHaveChanged, marketsMapModified,
                                                newMarketsOrEventsForOutsideCheck, events, markets, marketCataloguesMap, mustStop, orderCacheInitializedFromStreamStamp, programStartTime);
                final Thread thread = new Thread(managedMarketThread);
                thread.start();
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
        return Objects.equals(this.marketId, that.marketId);
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(this.marketId);
    }
}
