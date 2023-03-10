package info.fmro.shared.logic;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.enums.MarketBettingType;
import info.fmro.shared.enums.PrefSide;
import info.fmro.shared.enums.PriceLadderType;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.Utils;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.definitions.MarketDefinition;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.ComparatorMarketPrices;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass", "WeakerAccess", "PackageVisibleField", "OverlyCoupledClass"})
public class ManagedMarket
        implements Serializable {
    public static final long recentCalculatedLimitPeriod = 30_000L;
    public static final long almostLivePeriod = Generic.HOUR_LENGTH_MILLISECONDS;
    public static final long veryRecentPeriod = 10_000L;
    private static final Logger logger = LoggerFactory.getLogger(ManagedMarket.class);
    @Serial
    private static final long serialVersionUID = -7958840665816144122L;
    public final AtomicBoolean cancelAllUnmatchedBets = new AtomicBoolean();
    public final HighestLongContainer lastCheckMarketRequestStamp = new HighestLongContainer();
    final String marketId; // marketId
    private final HashMap<RunnerId, ManagedRunner> runners = new HashMap<>(4); // this is the only place where managedRunners are stored permanently
    private final HashMap<RunnerId, Double> runnerMatchedExposureMap = new HashMap<>(4), runnerTotalExposureMap = new HashMap<>(4), runnerTotalExposureConsideringCanceledMap = new HashMap<>(4);
    private final AtomicBoolean enabledMarket = new AtomicBoolean(true), mandatoryPlace = new AtomicBoolean(), keepAtInPlay = new AtomicBoolean();
    private final long creationTime;
    transient AtomicBoolean isBeingManaged = new AtomicBoolean();
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private String parentEventId;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private String marketName;
    private double amountLimit = -1d; // only has effect if >= 0d
    private double calculatedLimit;
    private double marketMatchedExposure = Double.NaN, marketTotalExposure = Double.NaN, marketTotalExposureConsideringCanceled = Double.NaN;
    private double matchedBackExposureSum, unmatchedBackExposureSum, totalBackExposureSum, totalBackExposureSumConsideringCanceled;
    private long timeMarketGoesLive;
    private long calculatedLimitStamp;
    private long manageMarketStamp;
    private boolean marketLiveOrAlmostLive;
    private long enabledTime;
    @Nullable
    private transient ManagedMarketThread currentManageThread;
    @Nullable
    private transient ManagedEvent parentEvent;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    @Nullable
    private transient Market market;
//    @Nullable
//    private transient OrderMarket orderMarket;
//    private transient ArrayList<ManagedRunner> runnersOrderedList = new ArrayList<>(this.runners.values());

    public ManagedMarket(@NotNull final String marketId, @NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        this.marketId = marketId;
        this.creationTime = System.currentTimeMillis();
        this.enabledTime = this.creationTime;
//        this.parentEventId = Formulas.getEventIdOfMarketId(this.id, marketCataloguesMap);
//        this.runnersOrderedList.sort(Comparator.comparing(k -> k.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
//        this.setMarketName(Formulas.getMarketCatalogueName(marketId, marketCataloguesMap), rulesManager.listOfQueues);
        this.marketName = Formulas.getMarketCatalogueName(marketId, marketCataloguesMap);
        attachMarket(rulesManager, marketCataloguesMap, false);
    }

    private static int getSortPriorityForEntry(@NotNull final HashMap<RunnerId, Integer> sortPriorityMap, @NotNull final Map.Entry<RunnerId, ManagedRunner> entry) {
        final Integer sortPriority = sortPriorityMap.get(entry.getKey());
        return sortPriority == null ? Integer.MAX_VALUE : sortPriority;
    }

    @Serial
    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.isBeingManaged = new AtomicBoolean();
        this.parentEvent = null;
        this.market = null;
//        this.orderMarket = null;
        this.currentManageThread = null;
//        this.runnersOrderedList = new ArrayList<>(this.runners.values());
//        this.runnersOrderedList.sort(Comparator.comparing(k -> k.getLastTradedPrice(marketCache), new ComparatorMarketPrices()));
    }

    @Nullable
    synchronized ManagedRunner getManagedRunner(@Nullable final RunnerId runnerId) {
        return this.runners.get(runnerId);
    }

    @NotNull
    synchronized ArrayList<ManagedRunner> createRunnersOrderedList() {
        final ArrayList<ManagedRunner> runnersOrderedList = new ArrayList<>(this.runners.values());
        runnersOrderedList.sort(Comparator.comparing(ManagedRunner::getLastTradedPrice, new ComparatorMarketPrices()));
        return runnersOrderedList;
    }

    synchronized ArrayList<ManagedRunner> simpleGetRunners() {
        return new ArrayList<>(this.runners.values());
    }

    public synchronized HashMap<RunnerId, ManagedRunner> simpleGetRunnersMap() {
        return new HashMap<>(this.runners);
    }

    @NotNull
    public synchronized LinkedHashMap<RunnerId, ManagedRunner> getRunners(@NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        attachMarket(rulesManager, marketCataloguesMap);
        final HashMap<RunnerId, Integer> sortPriorityMap = this.market == null ? null : this.market.getRunnerSortPriorityMap();
        final LinkedHashMap<RunnerId, ManagedRunner> returnList;
        if (sortPriorityMap == null) {
            returnList = new LinkedHashMap<>(this.runners);
        } else {
            returnList = this.runners.entrySet()
                                     .stream()
                                     .sorted(Comparator.comparingInt(o -> getSortPriorityForEntry(sortPriorityMap, o)))
                                     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        }
        return returnList;
    }

//    private synchronized boolean exposureIsRecent() {
//        final long currentTime = System.currentTimeMillis();
//        return exposureIsRecent(currentTime);
//    }

//    synchronized boolean exposureIsRecent(final long currentTime) {
//        final boolean isRecent;
//        int notRecentCounter = 0;
//        for (final ManagedRunner managedRunner : this.runners.values()) {
//            if (managedRunner.isRecent(currentTime)) { // no error, nothing to be done
//            } else {
//                notRecentCounter++;
//            }
//        }
//        if (notRecentCounter == 0) {
//            isRecent = true;
//        } else {
//            isRecent = false;
//            logger.info("exposureIsNotRecent {} out of {} for: {}", notRecentCounter, this.runners.size(), this.marketId);
//        }
//        return isRecent;
//    }

    private synchronized boolean isVeryRecent() {
        return isVeryRecent(System.currentTimeMillis());
    }

    private synchronized boolean isVeryRecent(final long currentTime) {
        return currentTime - Math.max(this.creationTime, this.enabledTime) <= veryRecentPeriod;
    }

    public boolean isEnabledMarket() { // not synchronized
        return this.enabledMarket.get();
    }

    public boolean isMandatoryPlace() { // not synchronized
        return this.mandatoryPlace.get();
    }

    public boolean isKeepAtInPlay() { // not synchronized
        return this.keepAtInPlay.get();
    }

    public synchronized void setEnabledMarket(final boolean enabledMarket, @NotNull final RulesManager rulesManager) {
        setEnabledMarket(enabledMarket, rulesManager, true); // default true sendModificationThroughStream
    }

    public synchronized void setEnabledMarket(final boolean enabledMarket, @NotNull final RulesManager rulesManager, final boolean sendModificationThroughStream) {
        if (this.enabledMarket.get() == enabledMarket) { // no update needed
        } else {
            this.enabledMarket.set(enabledMarket);
            this.enabledTime = System.currentTimeMillis();
            rulesManager.rulesHaveChanged.set(true);
            rulesManager.marketsMapModified.set(true);
            if (rulesManager.marketsForOutsideCheck.add(this.marketId)) {
                rulesManager.newMarketsOrEventsForOutsideCheck.set(true);
            }
            rulesManager.marketsToCheck.put(this.marketId, this.enabledTime);

            if (sendModificationThroughStream) {
                rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketEnabled, this.marketId, this.enabledMarket.get()));
            } else { // no need to send this modification
            }
        }
    }

    public synchronized void setMandatoryPlace(final boolean mandatoryPlace, @NotNull final RulesManager rulesManager) {
        if (this.mandatoryPlace.get() == mandatoryPlace) { // no update needed
        } else {
            this.mandatoryPlace.set(mandatoryPlace);
            rulesManager.rulesHaveChanged.set(true);
            rulesManager.marketsMapModified.set(true);
            if (rulesManager.marketsForOutsideCheck.add(this.marketId)) {
                rulesManager.newMarketsOrEventsForOutsideCheck.set(true);
            }
            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());

            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketMandatoryPlace, this.marketId, this.mandatoryPlace.get()));
        }
    }

    public synchronized void setRunnerMandatoryPlace(@NotNull final RunnerId runnerId, final boolean mandatoryPlace, @NotNull final RulesManager rulesManager) {
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            final boolean isTwoRunnerMarket = this.runners.size() == 2;
            if (isTwoRunnerMarket) {
                for (final ManagedRunner runner : this.runners.values()) {
                    runner.setMandatoryPlace(mandatoryPlace, rulesManager);
                }
            } else {
                managedRunner.setMandatoryPlace(mandatoryPlace, rulesManager);
            }
        } else {
            logger.error("trying to setRunnerMandatoryPlace on a runner that doesn't exist: {} {} {}", this.marketId, runnerId, mandatoryPlace); // this also covers the case where the element is null, but this should never happen
        }
    }

    public synchronized void setKeepAtInPlay(final boolean keepAtInPlay, @NotNull final RulesManager rulesManager) {
        if (this.keepAtInPlay.get() == keepAtInPlay) { // no update needed
        } else {
            if (keepAtInPlay) {
                logger.error("denying attempt to set keepAtInPlay true for: {} {}", this.marketId, this.marketName);
            } else {
                this.keepAtInPlay.set(keepAtInPlay);
                rulesManager.rulesHaveChanged.set(true);
                rulesManager.marketsMapModified.set(true);
                if (rulesManager.marketsForOutsideCheck.add(this.marketId)) {
                    rulesManager.newMarketsOrEventsForOutsideCheck.set(true);
                }
                rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());

                rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketKeepAtInPlay, this.marketId, this.keepAtInPlay.get()));
            }
        }
    }

    public synchronized void setRunnerPrefSide(@NotNull final RunnerId runnerId, final PrefSide prefSide, @NotNull final RulesManager rulesManager) {
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            managedRunner.setPrefSide(prefSide, rulesManager);
        } else {
            logger.error("trying to setRunnerPrefSide on a runner that doesn't exist: {} {} {}", this.marketId, runnerId, prefSide); // this also covers the case where the element is null, but this should never happen
        }
    }

    public synchronized boolean isTwoWayMarket() {
        return getNRunners() == 2;
    }

    public synchronized int getNRunners() {
        return this.runners.size();
    }

    public String getMarketId() {
        return this.marketId;
    }

    public synchronized String simpleGetMarketName() {
        return this.marketName;
    }

    public synchronized String getMarketName(@NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        if (this.marketName == null) {
            attachMarket(rulesManager, marketCataloguesMap);
        } else { // I already have marketName, I'll just return it
        }
        return this.marketName;
    }

    public final synchronized void setMarketName(final String marketName, @NotNull final ListOfQueues listOfQueues) {
        if (marketName != null && !Objects.equals(this.marketName, marketName)) {
            this.marketName = marketName;
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketName, this.marketId, this.marketName));
        } else { // new value null or same as old value
        }
    }

    private synchronized void setMarketName(final MarketDefinition marketDefinition, @NotNull final ListOfQueues listOfQueues) {
        if (marketDefinition == null) {
            logger.error("null marketDefinition in setMarketName for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
        } else {
            if (this.marketName == null) {
                this.marketName = marketDefinition.getMarketType();
                if (this.marketName == null) {
                    logger.error("null marketName from marketDefinition for: {} {}", Generic.objectToString(marketDefinition), Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
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

    public synchronized ManagedEvent getParentEvent(@NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final RulesManager rulesManager) {
        if (this.parentEvent == null) {
            final String localParentEventId = this.getParentEventId(marketCataloguesMap, rulesManager.rulesHaveChanged);
            if (localParentEventId == null) {
                logger.info("parentEventId && parentEvent null in getParentEvent for: {} {}", this.marketId, this.marketName);
            } else {
                this.parentEvent = rulesManager.events.get(localParentEventId);
                if (this.parentEvent == null) { // I won't create the event here, but outside the method, in the caller, due to potential synchronization problems
                } else {
//            this.parentEvent.addManagedMarket(this);
//                    final String marketId = this.getMarketId();
                    this.parentEvent.marketsMap.putIfAbsent(this.marketId, this, rulesManager.markets);
                    if (this.parentEvent.marketIds.add(this.marketId)) {
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
            logger.error("false result in parentEventIsSet for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
        }

        return result;
    }

    public synchronized boolean parentEventHasTheMarketAdded() { // used for testing, to detect bugs
        final boolean result;
        result = this.parentEvent != null && this.parentEvent.marketIds.contains(this.marketId);

        if (result) { // no error, nothing to be done, will return result
        } else {
            logger.error("false result in parentEventHasTheMarketAdded for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
        }

        return result;
    }

    public synchronized boolean parentEventHasTheMarketIdAdded() { // used for testing, to detect bugs
        final boolean result;
        result = this.parentEvent != null && this.parentEvent.marketIds.contains(this.marketId);

        if (result) { // no error, nothing to be done, will return result
        } else {
            logger.error("false result in parentEventHasTheMarketIdAdded for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
        }

        return result;
    }

    @Contract(pure = true)
    private synchronized double getMatchedBackExposureSum() {
        return this.matchedBackExposureSum;
    }

    private synchronized void setMatchedBackExposureSum(final double matchedBackExposureSum) {
        this.matchedBackExposureSum = matchedBackExposureSum;
    }

    private synchronized double getUnmatchedBackExposureSum() {
        return this.unmatchedBackExposureSum;
    }

    private synchronized void setUnmatchedBackExposureSum(final double unmatchedBackExposureSum) {
        this.unmatchedBackExposureSum = unmatchedBackExposureSum;
    }

    @Contract(pure = true)
    private synchronized double getTotalBackExposureSum() {
        return this.totalBackExposureSum;
    }

    private synchronized void setTotalBackExposureSum(final double totalBackExposureSum) {
        this.totalBackExposureSum = totalBackExposureSum;
    }

    @Contract(pure = true)
    private synchronized double getTotalBackExposureSumConsideringCanceled() {
        return this.totalBackExposureSumConsideringCanceled;
    }

    private synchronized void setTotalBackExposureSumConsideringCanceled(final double totalBackExposureSumConsideringCanceled) {
        this.totalBackExposureSumConsideringCanceled = totalBackExposureSumConsideringCanceled;
    }

    public synchronized boolean defaultExposureValuesExist() {
        return Double.isNaN(this.marketMatchedExposure) || Double.isNaN(this.marketTotalExposure) || Double.isNaN(this.marketTotalExposureConsideringCanceled);
    }

    public synchronized double getMarketMatchedExposure() {
        if (Double.isNaN(this.marketMatchedExposure)) {
            logger.error("returning defaultMarketMatchedExposure for: {}", this.marketId);
        } else { // no error, normal case
        }
        return this.marketMatchedExposure;
    }

    public synchronized double getMarketTotalExposure() {
        if (Double.isNaN(this.marketTotalExposure)) {
            logger.error("returning defaultMarketTotalExposure for: {}", this.marketId);
        } else { // no error, normal case
        }
        return this.marketTotalExposure;
    }

    public synchronized double getMarketTotalExposureConsideringCanceled() {
        if (Double.isNaN(this.marketTotalExposureConsideringCanceled)) {
            logger.error("returning defaultMarketTotalExposureConsideringCanceled for: {}", this.marketId);
        } else { // no error, normal case
        }
        return this.marketTotalExposureConsideringCanceled;
    }
//    public synchronized void resetOrderCacheObjects() {
//        this.orderMarket = null;
//        for (final ManagedRunner managedRunner : this.runners.values()) {
//            managedRunner.resetOrderMarketRunner();
//        }
//    }

    public synchronized long getTimeMarketGoesLive(@NotNull final MarketsToCheckMap marketsToCheck) {
        if (this.marketLiveOrAlmostLive) { // I won't recalculate
        } else {
            calculateTimeMarketGoesLive(marketsToCheck);
        }
        return this.timeMarketGoesLive;
    }

    public synchronized boolean setTimeMarketGoesLive(final long newTimeMarketGoesLive, @NotNull final MarketsToCheckMap marketsToCheck) {
        final boolean modified;
        if (newTimeMarketGoesLive <= 0L) {
            logger.error("attempt to set strange timeMarketGoesLive value {} for: {}", newTimeMarketGoesLive, Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
            modified = false;
        } else if (this.timeMarketGoesLive == newTimeMarketGoesLive) {
            modified = false;
        } else if (this.timeMarketGoesLive == 0L) {
            this.timeMarketGoesLive = newTimeMarketGoesLive;
            modified = true;
        } else {
            logger.info("setting different value for timeMarketGoesLive {} to {}, difference of {} minutes for: {} {}", this.timeMarketGoesLive, newTimeMarketGoesLive, (newTimeMarketGoesLive - this.timeMarketGoesLive) / Generic.MINUTE_LENGTH_MILLISECONDS,
                        this.marketId, this.marketName);
            this.timeMarketGoesLive = newTimeMarketGoesLive;
            modified = true;
        }

        if (modified) {
            marketsToCheck.put(this.marketId, System.currentTimeMillis());
        }
        return modified;
    }

    private synchronized void calculateTimeMarketGoesLive(@NotNull final MarketsToCheckMap marketsToCheck) {
        final long result;
        if (this.market == null) {
            logger.error("null market in calculateTimeMarketGoesLive for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
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

    private synchronized void calculateTimeMarketGoesLive(final MarketDefinition marketDefinition, @NotNull final MarketsToCheckMap marketsToCheck) {
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
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMarketAmountLimit, this.marketId, this.amountLimit));
            rulesManager.rulesHaveChanged.set(true);

            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());

            final double maxLimit = getMaxMarketLimit(safetyLimits);
            if (simpleGetCalculatedLimit() > maxLimit) {
                setCalculatedLimit(maxLimit, true, safetyLimits, rulesManager.listOfQueues);
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

    synchronized boolean setRunnerBackAmountLimit(@NotNull final RunnerId runnerId, @NotNull final Double runnerAmountLimit, @NotNull final RulesManager rulesManager) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            if (isTwoWayMarket()) {
                @Nullable final ManagedRunner secondManagedRunner = getSecondManagedRunner(managedRunner);
                success = secondManagedRunner != null && managedRunner.setBackAmountLimit(runnerAmountLimit, rulesManager) && secondManagedRunner.setLayAmountLimit(runnerAmountLimit, rulesManager);
            } else {
                success = managedRunner.setBackAmountLimit(runnerAmountLimit, rulesManager);
            }
        } else { // this also covers the case where the element is null, but this should never happen
            logger.error("trying to setRunnerBackAmountLimit on a runner that doesn't exist: {} {} {}", this.marketId, runnerId, runnerAmountLimit);
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerLayAmountLimit(@NotNull final RunnerId runnerId, @NotNull final Double runnerAmountLimit, @NotNull final RulesManager rulesManager) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            if (isTwoWayMarket()) {
                @Nullable final ManagedRunner secondManagedRunner = getSecondManagedRunner(managedRunner);
                success = secondManagedRunner != null && managedRunner.setLayAmountLimit(runnerAmountLimit, rulesManager) && secondManagedRunner.setBackAmountLimit(runnerAmountLimit, rulesManager);
            } else {
                success = managedRunner.setLayAmountLimit(runnerAmountLimit, rulesManager);
            }
        } else {
            logger.error("trying to setRunnerLayAmountLimit on a runner that doesn't exist: {} {} {}", this.marketId, runnerId, runnerAmountLimit); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerMaxLayOdds(@NotNull final RunnerId runnerId, @NotNull final Double odds, @NotNull final RulesManager rulesManager) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            if (isTwoWayMarket()) {
                @Nullable final ManagedRunner secondManagedRunner = getSecondManagedRunner(managedRunner);
                success = secondManagedRunner != null && managedRunner.setMaxLayOdds(odds, rulesManager) && secondManagedRunner.setMinBackOdds(Formulas.inverseOdds(odds, Side.L), rulesManager);
            } else {
                success = managedRunner.setMaxLayOdds(odds, rulesManager);
            }
        } else {
            logger.error("trying to setRunnerMaxLayOdds on a runner that doesn't exist: {} {} {}", this.marketId, runnerId, odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    synchronized boolean setRunnerMinBackOdds(@NotNull final RunnerId runnerId, @NotNull final Double odds, @NotNull final RulesManager rulesManager) {
        final boolean success;
        @Nullable final ManagedRunner managedRunner = this.runners.get(runnerId);
        if (managedRunner != null) {
            if (isTwoWayMarket()) {
                @Nullable final ManagedRunner secondManagedRunner = getSecondManagedRunner(managedRunner);
                success = secondManagedRunner != null && managedRunner.setMinBackOdds(odds, rulesManager) && secondManagedRunner.setMaxLayOdds(Formulas.inverseOdds(odds, Side.B), rulesManager);
            } else {
                success = managedRunner.setMinBackOdds(odds, rulesManager);
            }
        } else {
            logger.error("trying to setRunnerMinBackOdds on a runner that doesn't exist: {} {} {}", this.marketId, runnerId, odds); // this also covers the case where the element is null, but this should never happen
            success = false;
        }

        return success;
    }

    public synchronized ManagedRunner removeRunner(@NotNull final RunnerId runnerId, @NotNull final RulesManager rulesManager) {
        rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.removeManagedRunner, this.marketId, runnerId));
        @Nullable final ManagedRunner managedRunner = this.runners.remove(runnerId);
        if (managedRunner != null) {
            rulesManager.rulesHaveChanged.set(true);
            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());
        } else {
            logger.error("trying to removeManagedRunner that doesn't exist: {} {}", this.marketId, runnerId); // this also covers the case where the removed element is null, but this should never happen
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
            logger.error("trying to add managedRunner over existing one: {} {} {} {} {}", this.marketId, this.marketName, runnerId, Generic.objectToString(existingManagedRunner), Generic.objectToString(managedRunner));
            success = false;
        } else {
            managedRunner.hardSetMarketMandatoryPlace(this.mandatoryPlace, rulesManager);
            if (sendRunnerThroughStream) {
                rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedRunner, managedRunner));
            } else { // no need to send runner through stream, most likely the entire market object will be sent
            }
            this.runners.put(runnerId, managedRunner); // runners.put needs to be before runnersOrderedList.addAll
            managedRunner.attachRunner(this.market);

            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());
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
            returnValue = new ManagedRunner(this.marketId, runnerId, this.mandatoryPlace, this.keepAtInPlay);
            if (sendRunnerThroughStream) {
                rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.addManagedRunner, returnValue));
            } else { // no need to send runner through stream, most likely the entire market object will be sent
            }
            this.runners.put(runnerId, returnValue); // runners.put needs to be before runnersOrderedList.addAll
            returnValue.attachRunner(this.market);

            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());

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

    public synchronized Market getMarket(@NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        attachMarket(rulesManager, marketCataloguesMap);
        return this.market;
    }

    public final synchronized boolean attachMarket(@NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        // default sendRunnerThroughStream true; it's false when the method is used in the constructor, so it doesn't send the runners before the market is created
        return attachMarket(rulesManager, marketCataloguesMap, true);
    }

    public final synchronized boolean attachMarket(@NotNull final RulesManager rulesManager, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final boolean sendRunnerThroughStream) {
        // this is run periodically, as it's contained in the manage method, that is run periodically
        final boolean newMarketAttached;
        if (this.market == null) {
            this.market = SharedStatics.marketCache.markets.get(this.marketId);
            newMarketAttached = this.market != null;
        } else { // I already have the market, nothing to be done
            newMarketAttached = false;
        }
        if (this.market == null) {
            if (Formulas.programHasRecentlyStarted(System.currentTimeMillis()) || isVeryRecent()) { // normal
            } else {
                logger.info("no market found in MarketCache, probably old expired market, for: {} {}", this.marketId, this.marketName); // this happens for manager markets that are old and no longer exist on the site
            }
        } else {
            for (final ManagedRunner managedRunner : this.runners.values()) {
                managedRunner.attachRunner(this.market);
            }
            if (SharedStatics.programName.get() == ProgramName.SERVER) {
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
            setMarketName(this.market.getMarketDefinition(), rulesManager.listOfQueues);
        }
//        getParentEventId(marketCataloguesMap, rulesManager.rulesHaveChanged);
        getParentEvent(marketCataloguesMap, rulesManager); // includes getParentEventId
        return newMarketAttached;
    }

    synchronized boolean isMarketLiveOrAlmostLive(@NotNull final MarketsToCheckMap marketsToCheck) {
        if (this.marketLiveOrAlmostLive) { // already almostLive, won't recheck
        } else if (this.market == null) {
            logger.error("running isMarketLiveOrAlmostLive on managedMarket with no attached market for: {} {} {}", this.marketId, this.marketName, this.parentEventId);
        } else {
            final MarketDefinition marketDefinition = this.market.getMarketDefinition();
            final Boolean inPlay = marketDefinition.getInPlay();
            if (inPlay != null && inPlay) {
                this.marketLiveOrAlmostLive = inPlay; // inPlay == true
                logger.info("managed market {} is live inPlay:{}", this.marketId, inPlay);
            } else {
                calculateTimeMarketGoesLive(marketDefinition, marketsToCheck);
                final long currentTime = System.currentTimeMillis();
                final long timeGoesLive = getTimeMarketGoesLive(marketsToCheck);
                if (currentTime + almostLivePeriod >= timeGoesLive) {
                    this.marketLiveOrAlmostLive = true;
                    logger.info("managed market {} is almost live: {} {} minimum:{}s {} current:{}s", this.marketId, this.marketLiveOrAlmostLive, timeGoesLive, Generic.millisecondsToSecondsString(almostLivePeriod), currentTime,
                                Generic.millisecondsToSecondsString(timeGoesLive - currentTime));
                }
            }
        }
//            if (this.marketAlmostLive) {
//                logger.info("managed market is almost live: {} {}", this.marketId, this.marketName);
//            }

        return this.marketLiveOrAlmostLive;
    }

    public synchronized double getTotalValue(@NotNull final AtomicDouble currencyRate) {
        final double result;
        if (this.market != null) {
            result = this.market.getTvEUR(currencyRate);
        } else {
            logger.error("no market present in getTotalValue for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
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
                } else if (firstRunner.errorBackSmallerThanLayOdds() || secondRunner.errorBackSmallerThanLayOdds()) { // no need to print error message, it was printed already
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
            logger.error("wrong size runnersOrderedList in checkTwoWayMarketLimitsValid for: {} {}", Generic.objectToString(runnersOrderedList), Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
            isValid = false;
        }
        return isValid;
    }

    private synchronized int excessMatchedExposureBalanceTwoRunnerMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final List<Side> sidesToPlaceExposureOn, final double excessMatchedExposure,
                                                                         @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod, @NotNull final BetFrequencyLimit speedLimit, final String reason) {
        int modifications = 0;
        if (sidesToPlaceExposureOn.size() == 2) {
            @NotNull final Side firstSide = sidesToPlaceExposureOn.get(0), secondSide = sidesToPlaceExposureOn.get(1);
            final double totalTempExposure = firstRunner.totalTempExposure() + secondRunner.totalTempExposure();
            if (firstSide == Side.B && secondSide == Side.L) {
                @NotNull final List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstRunner.cancelUnmatchedAmounts(-firstExposureToBePlaced, 0d, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
                } else {
                    if (firstRunner.placeOrder(firstSide, firstExposureToBePlaced - totalTempExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, true, reason) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondRunner.cancelUnmatchedAmounts(0d, -secondExposureToBePlaced, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
                } else {
                    if (secondRunner.placeOrder(secondSide, secondExposureToBePlaced - totalTempExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, true, reason) > 0d) {
                        modifications++;
                    }
                }
            } else if (firstSide == Side.L && secondSide == Side.B) {
                @NotNull final List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure);
                final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                if (firstExposureToBePlaced <= 0d) {
                    modifications += firstRunner.cancelUnmatchedAmounts(0d, -firstExposureToBePlaced, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
                } else {
                    if (firstRunner.placeOrder(firstSide, firstExposureToBePlaced - totalTempExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, true, reason) > 0d) {
                        modifications++;
                    }
                }
                if (secondExposureToBePlaced <= 0d) {
                    modifications += secondRunner.cancelUnmatchedAmounts(-secondExposureToBePlaced, 0d, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
                } else {
                    if (secondRunner.placeOrder(secondSide, secondExposureToBePlaced - totalTempExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, true, reason) > 0d) {
                        modifications++;
                    }
                }
            } else {
                logger.error("bogus sides for excessMatchedExposureBalanceTwoRunnerMarket: {} {} {} {} {}", Generic.objectToString(sidesToPlaceExposureOn), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), excessMatchedExposure,
                             reason);
            }
        } else {
            logger.error("bogus sideList for excessMatchedExposureBalanceTwoRunnerMarket: {} {} {} {} {}", Generic.objectToString(sidesToPlaceExposureOn), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), excessMatchedExposure,
                         reason);
        }
        return modifications;
    }

    private synchronized int removeExposureBalanceTwoRunnerMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final List<Side> sidesToPlaceExposureOn, final double excessMatchedExposure,
                                                                  @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod, @NotNull final BetFrequencyLimit speedLimit, final String reason) {
        int modifications = 0;
        if (sidesToPlaceExposureOn.size() == 2) {
            @NotNull final Side firstSide = sidesToPlaceExposureOn.get(0), secondSide = sidesToPlaceExposureOn.get(1);
//            final double totalTempExposure = firstRunner.totalTempExposure() + secondRunner.totalTempExposure();
            if ((firstSide == Side.B && secondSide == Side.L) || (firstSide == Side.L && secondSide == Side.B)) {
                modifications += SharedStatics.orderCache.cancelUnmatched(firstRunner.getMarketId(), firstRunner.getRunnerId(), secondSide, firstRunner, sendPostRequestRescriptMethod, "removeExposureBalanceTwoRunnerMarket");
                modifications += SharedStatics.orderCache.cancelUnmatched(secondRunner.getMarketId(), secondRunner.getRunnerId(), firstSide, secondRunner, sendPostRequestRescriptMethod, "removeExposureBalanceTwoRunnerMarket");
                modifications += excessMatchedExposureBalanceTwoRunnerMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, reason);
//            } else if (firstSide == Side.L && secondSide == Side.B) {
//                modifications += SharedStatics.orderCache.cancelUnmatched(firstRunner.getMarketId(), firstRunner.getRunnerId(), secondSide, firstRunner, sendPostRequestRescriptMethod, "removeExposureBalanceTwoRunnerMarket");
//                modifications += SharedStatics.orderCache.cancelUnmatched(secondRunner.getMarketId(), secondRunner.getRunnerId(), firstSide, secondRunner, sendPostRequestRescriptMethod, "removeExposureBalanceTwoRunnerMarket");
//                modifications += excessMatchedExposureBalanceTwoRunnerMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, reason);
            } else {
                logger.error("bogus sides for balanceTwoRunnerMarket: {} {} {} {} {}", Generic.objectToString(sidesToPlaceExposureOn), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), excessMatchedExposure, reason);
            }
        } else {
            logger.error("bogus sideList for balanceTwoRunnerMarket: {} {} {} {} {}", Generic.objectToString(sidesToPlaceExposureOn), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), excessMatchedExposure, reason);
        }
        return modifications;
    }

    private synchronized int placeExposureOnTwoRunnerMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final List<Side> sidesToPlaceExposureOn, final double availableLimit,
                                                            @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod, @NotNull final BetFrequencyLimit speedLimit, final String reason) {
        int modifications = 0;
        if (availableLimit > 0d) {
            if (sidesToPlaceExposureOn.size() == 2) {
                @NotNull final Side firstSide = sidesToPlaceExposureOn.get(0), secondSide = sidesToPlaceExposureOn.get(1);
                if (firstSide == Side.B && secondSide == Side.L) {
                    @NotNull final List<Double> exposuresToBePlaced =
                            Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, availableLimit);
                    final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);

//                    logger.info("BL availableLimit={} firstExposureToBePlaced={} secondExposureToBePlaced={}", availableLimit, firstExposureToBePlaced, secondExposureToBePlaced);

                    if (firstExposureToBePlaced > 0d) {
                        if (firstRunner.placeOrder(Side.B, firstExposureToBePlaced, existingFunds, sendPostRequestRescriptMethod, speedLimit, false, reason) > 0d) {
                            modifications++;
                        }
                    } else { // nothing to place
                    }
                    if (secondExposureToBePlaced > 0d) {
                        if (secondRunner.placeOrder(Side.L, secondExposureToBePlaced, existingFunds, sendPostRequestRescriptMethod, speedLimit, false, reason) > 0d) {
                            modifications++;
                        }
                    } else { // nothing to place
                    }
                } else if (firstSide == Side.L && secondSide == Side.B) {
                    @NotNull final List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, availableLimit);
                    final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);

//                    logger.info("LB availableLimit={} firstExposureToBePlaced={} secondExposureToBePlaced={}", availableLimit, firstExposureToBePlaced, secondExposureToBePlaced);

                    if (firstExposureToBePlaced > 0d) {
                        if (firstRunner.placeOrder(Side.L, firstExposureToBePlaced, existingFunds, sendPostRequestRescriptMethod, speedLimit, false, reason) > 0d) {
                            modifications++;
                        }
                    } else { // nothing to place
                    }
                    if (secondExposureToBePlaced > 0d) {
                        if (secondRunner.placeOrder(Side.B, secondExposureToBePlaced, existingFunds, sendPostRequestRescriptMethod, speedLimit, false, reason) > 0d) {
                            modifications++;
                        }
                    } else { // nothing to place
                    }
                } else {
                    logger.error("bogus sides for placeExposureOnTwoRunnerMarket: {} {} {} {} {}", Generic.objectToString(sidesToPlaceExposureOn), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), availableLimit, reason);
                }
            } else {
                logger.error("bogus sideList for placeExposureOnTwoRunnerMarket: {} {} {} {} {}", Generic.objectToString(sidesToPlaceExposureOn), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), availableLimit, reason);
            }
        } else { // normal, nothing to do
        }
        return modifications;
    }

    private synchronized int cancelExposureOnTwoRunnerMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final List<Side> sidesWithExcessExposure, final double availableLimit,
                                                             @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        int modifications = 0;
        if (availableLimit < 0d) {
            if (sidesWithExcessExposure.size() == 2) {
                @NotNull final Side firstSide = sidesWithExcessExposure.get(0), secondSide = sidesWithExcessExposure.get(1);
                if (firstSide == Side.B && secondSide == Side.L) {
                    @NotNull final List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesWithExcessExposure, availableLimit);
                    final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                    if (firstExposureToBePlaced < 0d) {
                        modifications += firstRunner.cancelUnmatchedAmounts(-firstExposureToBePlaced, 0d, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
                    } else { // nothing to cancel
                    }
                    if (secondExposureToBePlaced < 0d) {
                        modifications += secondRunner.cancelUnmatchedAmounts(0d, -secondExposureToBePlaced, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
                    } else { // nothing to cancel
                    }
                } else if (firstSide == Side.L && secondSide == Side.B) {
                    @NotNull final List<Double> exposuresToBePlaced = Utils.getExposureToBePlacedForTwoWayMarket(firstRunner, secondRunner, sidesWithExcessExposure, availableLimit);
                    final double firstExposureToBePlaced = exposuresToBePlaced.get(0), secondExposureToBePlaced = exposuresToBePlaced.get(1);
                    if (firstExposureToBePlaced < 0d) {
                        modifications += firstRunner.cancelUnmatchedAmounts(0d, -firstExposureToBePlaced, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
                    } else { // nothing to cancel
                    }
                    if (secondExposureToBePlaced < 0d) {
                        modifications += secondRunner.cancelUnmatchedAmounts(-secondExposureToBePlaced, 0d, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
                    } else { // nothing to cancel
                    }
                } else {
                    logger.error("bogus sides for cancelExposureOnTwoRunnerMarket: {} {} {} {} {}", Generic.objectToString(sidesWithExcessExposure), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), availableLimit, reason);
                }
            } else {
                logger.error("bogus sideList for cancelExposureOnTwoRunnerMarket: {} {} {} {} {}", Generic.objectToString(sidesWithExcessExposure), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner), availableLimit, reason);
            }
        } else { // normal, nothing to do
        }
        return modifications;
    }

    synchronized int removeExposureGettingOut(@NotNull final ArrayList<? extends ManagedRunner> runnersOrderedList, @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod,
                                              @NotNull final BetFrequencyLimit speedLimit) {
        // assumes market and runners exposure has been updated
        int modifications = 0;
        if (Double.isNaN(this.marketTotalExposureConsideringCanceled)) {
            logger.error("marketTotalExposure not initialized in removeExposure for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
        } else if (this.marketTotalExposureConsideringCanceled < .1) { // exposure too small, nothing to be done
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

                    final double backLayTempExposure = firstRunner.rawBackTempExposure() + secondRunner.rawLayTempExposure(), layBackTempExposure = firstRunner.rawLayTempExposure() + secondRunner.rawBackTempExposure();
                    final double backLayMatchedExposure = firstRunner.getBackMatchedExposure() + secondRunner.getLayMatchedExposure() + backLayTempExposure,
                            layBackMatchedExposure = firstRunner.getLayMatchedExposure() + secondRunner.getBackMatchedExposure() + layBackTempExposure,
                            excessMatchedExposure = Math.abs(backLayMatchedExposure - layBackMatchedExposure);
                    if (excessMatchedExposure < .1d) {
                        modifications += cancelAllUnmatchedBets(sendPostRequestRescriptMethod, "cancelAllUnmatchedBetsGettingOut");
                    } else {
                        final List<Side> sidesToPlaceExposureOn;
                        if (layBackMatchedExposure < backLayMatchedExposure) {
                            // I'll use unmatched exposure, equal to excessMatchedExposure, on lay/back
                            sidesToPlaceExposureOn = List.of(Side.L, Side.B);
                        } else { // backLayMatchedExposure < layBackMatchedExposure
                            // I'll use unmatched exposure, equal to excessMatchedExposure, on back/lay
                            sidesToPlaceExposureOn = List.of(Side.B, Side.L);
                        }
                        modifications += removeExposureBalanceTwoRunnerMarket(firstRunner, secondRunner, sidesToPlaceExposureOn, excessMatchedExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit,
                                                                              "removeExposureGettingOut balanceTwoRunner");
                    }
                } else { // if not valid, error message and take action, with all order canceling
                    logger.error("checkTwoWayMarketLimitsValid false in removeExposure for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
                    modifications += cancelAllUnmatchedBets(sendPostRequestRescriptMethod, "cancelAllUnmatchedBetsGettingOutInvalidLimits");
                }
            } else {
                for (final ManagedRunner managedRunner : runnersOrderedList) { // only the exposure on the runner is considered, not the market wide exposure
                    modifications += managedRunner.removeExposureGettingOut(existingFunds, sendPostRequestRescriptMethod, speedLimit, "removeExposureGettingOut multipleRunner");
                } // end for
            }
        }
        return modifications;
    }

    @SuppressWarnings({"OverlyNestedMethod", "OverlyLongMethod", "OverlyComplexMethod"})
    synchronized int useTheNewLimit(@NotNull final ArrayList<? extends ManagedRunner> runnersOrderedList, @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod, @NotNull final BetFrequencyLimit speedLimit) {
        // the exposure for placing new orders does not consider tempCancel orders, but for calculating if limit is breached it will consider tempCancel
        int modifications = 0;
        if (defaultExposureValuesExist()) {
            logger.error("marketExposure not initialized in useTheNewLimit for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
        } else {
            // variant of removeExposure, or the other way around ... major difference is that in this case the overall calculatedLimit matters, this time it's not about individual runners
//            final long currentTime = System.currentTimeMillis();

            // calculate the splitting proportion for each runner: the factors for splitting the unmatched exposure between runners are the price of toBeUsedOdds, and the size of existing unmatched bets
            // total exposure only exists as total back exposure on each runner, and can be increased by lay on that runner and back on other runners
            // each runner has total back exposure, and ideally it should be equal to the calculatedLimit, but has to obey the per runner limits as well
            calculateIdealBackExposureList(runnersOrderedList);

            final int size = runnersOrderedList.size();
            if (size == 2) { // special case
                // this one is easy, most similar to the removeExposure situation
                // the factors for splitting the unmatched exposure between runners are the price of toBeUsedOdds, which of the toBeUsedOdds is more profitable, and the size of existing unmatched bets
                // also lay bets should be given slight priority over back bets, as other gamblers like to back rather than lay
                if (checkTwoWayMarketLimitsValid(runnersOrderedList)) {
                    final ManagedRunner firstRunner = runnersOrderedList.get(0), secondRunner = runnersOrderedList.get(1);
                    final double backLayExposure = firstRunner.getBackTotalExposure() + secondRunner.getLayTotalExposure(), layBackExposure = firstRunner.getLayTotalExposure() + secondRunner.getBackTotalExposure();
                    final double backLayMatchedExposure = firstRunner.getBackMatchedExposure() + secondRunner.getLayMatchedExposure(), layBackMatchedExposure = firstRunner.getLayMatchedExposure() + secondRunner.getBackMatchedExposure();
                    final double backLayExposureConsideringCanceled = firstRunner.getBackTotalExposureConsideringCanceled() + secondRunner.getLayTotalExposureConsideringCanceled(),
                            layBackExposureConsideringCanceled = firstRunner.getLayTotalExposureConsideringCanceled() + secondRunner.getBackTotalExposureConsideringCanceled();
                    final double backLayLimit = Math.min(this.calculatedLimit, firstRunner.getBackAmountLimit() + secondRunner.getLayAmountLimit()),
                            layBackLimit = Math.min(this.calculatedLimit, firstRunner.getLayAmountLimit() + secondRunner.getBackAmountLimit());
                    final double availableBackLayLimit = backLayLimit - backLayExposure, availableLayBackLimit = layBackLimit - layBackExposure;
                    final double availableMatchedBackLayLimit = backLayLimit - backLayMatchedExposure, availableMatchedLayBackLimit = layBackLimit - layBackMatchedExposure;
                    final double availableBackLayLimitConsideringCanceled = backLayLimit - backLayExposureConsideringCanceled, availableLayBackLimitConsideringCanceled = layBackLimit - layBackExposureConsideringCanceled;

//                    logger.info("availableBackLayLimitConsideringCanceled={} backLayLimit={} backLayExposureConsideringCanceled={}({}+{})", availableBackLayLimitConsideringCanceled, backLayLimit, backLayExposureConsideringCanceled,
//                                firstRunner.getBackTotalExposureConsideringCanceled(), secondRunner.getLayTotalExposureConsideringCanceled());
//                    logger.info("availableLBLimitConsideringCanceled={} LBLimit={} LBExposureConsideringCanceled={}({}+{})", availableLayBackLimitConsideringCanceled, layBackLimit, layBackExposureConsideringCanceled,
//                                firstRunner.getLayTotalExposureConsideringCanceled(), secondRunner.getBackTotalExposureConsideringCanceled());

                    modifications += cancelExposureOnTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.B, Side.L), availableBackLayLimitConsideringCanceled, sendPostRequestRescriptMethod, "cancelExposure onTwoRunner BL");
                    modifications += cancelExposureOnTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.L, Side.B), availableLayBackLimitConsideringCanceled, sendPostRequestRescriptMethod, "cancelExposure onTwoRunner LB");
                    if (availableMatchedBackLayLimit <= -0.1d) {
                        excessMatchedExposureBalanceTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.L, Side.B), -availableMatchedBackLayLimit, existingFunds, sendPostRequestRescriptMethod, speedLimit,
                                                                    "useTheNewLimit excessMatchedExposureBalanceTwoRunnerMarket LB");
                    } else { // matched exposure doesn't breach limit
                    }
                    if (availableMatchedLayBackLimit <= -0.1d) {
                        excessMatchedExposureBalanceTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.B, Side.L), -availableMatchedLayBackLimit, existingFunds, sendPostRequestRescriptMethod, speedLimit,
                                                                    "useTheNewLimit excessMatchedExposureBalanceTwoRunnerMarket BL");
                    } else { // matched exposure doesn't breach limit
                    }

//                    logger.info("calculatedLimit={} availableBackLayLimit={} backLayLimit={} backLayExposure={} availableLayBackLimit={} layBackLimit={} layBackExposure={}", this.calculatedLimit, availableBackLayLimit, backLayLimit, backLayExposure,
//                                availableLayBackLimit, layBackLimit, layBackExposure);

//                    if (availableBackLayLimit == 0d) { // availableLimit is 0d, nothing to be done
//                    } else {
                    modifications += placeExposureOnTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.B, Side.L), availableBackLayLimit, existingFunds, sendPostRequestRescriptMethod, speedLimit, "placeExposure onTwoRunner BL");
//                    }
//                    if (availableLayBackLimit == 0d) { // availableLimit is 0d, nothing to be done
//                    } else {
                    modifications += placeExposureOnTwoRunnerMarket(firstRunner, secondRunner, List.of(Side.L, Side.B), availableLayBackLimit, existingFunds, sendPostRequestRescriptMethod, speedLimit, "placeExposure onTwoRunner LB");
//                    }
//                    }
                } else { // if not valid, error message and take action, with all order canceling
                    logger.error("checkTwoWayMarketLimitsValid false in useTheNewLimit for: {} {}", this.marketId, this.marketName);
                    modifications += cancelAllUnmatchedBets(sendPostRequestRescriptMethod, "invalidTwoWayLimit");
                }
            } else {
                // new limit for lay exposure on the runner will be calculated by proportion * calculatedLimit; runner limits are also considered
                // calculate the amounts that need to be added or subtracted on the back side of that runner, for the new lay exposure limit
                // place the needed orders and recalculate exposure

                // removal of excess exposure done first, for each runner
                // first removal of unmatched on back, starting with highest odds, then unmatched on lay
                // then balanceMatched on lay and then on back
                if (this.calculatedLimit + .1d <= this.marketTotalExposureConsideringCanceled) { // I need to remove unmatched exposure
                    for (int i = 0; i < runnersOrderedList.size(); i++) {
                        final ManagedRunner managedRunner = runnersOrderedList.get(i);
                        final double localRunnerTotalExposureConsideringCanceled = getRunnerTotalExposureConsideringCanceled(managedRunner);
                        double excessExposureConsideringCanceled = localRunnerTotalExposureConsideringCanceled - this.calculatedLimit;
                        if (excessExposureConsideringCanceled >= .1d) {
                            for (int j = runnersOrderedList.size() - 1; j >= 0 && excessExposureConsideringCanceled >= .1d; j--) {
                                if (i == j) { // same managedRunner, won't cancel backExposure
                                } else {
                                    final ManagedRunner managedRunnerToRemoveExposure = runnersOrderedList.get(j);
                                    if (managedRunnerToRemoveExposure.rawBackUnmatchedExposure() > 0d) {
                                        excessExposureConsideringCanceled -= managedRunnerToRemoveExposure.cancelUnmatchedAmounts(excessExposureConsideringCanceled, 0d, sendPostRequestRescriptMethod,
                                                                                                                                  "useTheNewLimit remove backExcessExposure");
                                    } else { // no unmatched exposure, nothing to cancel
                                    }
                                }
                            }
                            if (excessExposureConsideringCanceled >= .1d) {
                                if (managedRunner.rawLayUnmatchedExposure() > 0d) {
                                    managedRunner.cancelUnmatchedAmounts(0d, excessExposureConsideringCanceled, sendPostRequestRescriptMethod, "useTheNewLimit remove layExcessExposure");
                                } else { // no unmatched exposure, nothing to cancel
                                }
                            } else { // no excessExposure left
                            }

                            this.updateOverallMarketAndRunnersExposureAndSums();
                        } else { // runner exposure is fine, nothing to be done on this branch
                        }
                    }
                } else { // no need to remove exposure, nothing to be done on this branch
                }
                if (this.calculatedLimit + .1d <= this.marketTotalExposureConsideringCanceled) { // I need to balance matched exposure
                    for (int i = 0; i < runnersOrderedList.size(); i++) {
                        final ManagedRunner managedRunner = runnersOrderedList.get(i);
                        final double localRunnerTotalExposureConsideringCanceled = getRunnerTotalExposureConsideringCanceled(managedRunner);
                        double excessExposure = localRunnerTotalExposureConsideringCanceled - this.calculatedLimit;
                        if (excessExposure >= .1d) {
                            final double excessLayMatchedExposureOnRunner = managedRunner.getLayMatchedExposure() - managedRunner.getBackMatchedExposure();
                            if (excessLayMatchedExposureOnRunner > .1d) {
                                excessExposure -= managedRunner.balanceMatchedAmounts(0d, Math.max(0d, Math.min(excessExposure, excessLayMatchedExposureOnRunner)), existingFunds, sendPostRequestRescriptMethod, speedLimit,
                                                                                      "useTheNewLimit balance layExcessExposure");
                            } else { // no unmatched exposure, nothing to cancel
                            }
                            if (excessExposure >= .1d) {
                                for (int j = runnersOrderedList.size() - 1; j >= 0 && excessExposure >= .1d; j--) {
                                    if (i == j) { // same managedRunner, won't balance exposure
                                    } else {
                                        final ManagedRunner managedRunnerToRemoveExposure = runnersOrderedList.get(j);
                                        final double excessBackMatchedExposureOnRunner = managedRunnerToRemoveExposure.getBackMatchedExposure() - managedRunnerToRemoveExposure.getLayMatchedExposure();
                                        if (excessBackMatchedExposureOnRunner > .1d) {
                                            excessExposure -= managedRunnerToRemoveExposure.balanceMatchedAmounts(Math.max(0d, Math.min(excessExposure, excessBackMatchedExposureOnRunner)), 0d, existingFunds, sendPostRequestRescriptMethod,
                                                                                                                  speedLimit, "useTheNewLimit balance backExcessExposure");
                                        } else { // no unmatched exposure, nothing to cancel
                                        }
                                    }
                                }
                            } else { // no excessExposure left
                            }

                            this.updateOverallMarketAndRunnersExposureAndSums();
                        } else { // runner exposure is fine, nothing to be done on this branch
                        }
                    }
                } else { // no need to remove exposure, nothing to be done on this branch
                }

                boolean layExposureWasPlaced = false;
                for (final ManagedRunner managedRunner : runnersOrderedList) { // place exposure on lay
                    final double availableLayLimit = managedRunner.getIdealLayExposure() - managedRunner.getLayTotalExposure();
                    final double availableMarketLimit = this.calculatedLimit - this.totalBackExposureSum + managedRunner.getBackTotalExposure() - managedRunner.getLayTotalExposure();
                    final double minimumAvailableLimit = Math.min(availableLayLimit, availableMarketLimit);
                    if (minimumAvailableLimit >= .1d) {
                        if (managedRunner.placeOrder(Side.L, minimumAvailableLimit, existingFunds, sendPostRequestRescriptMethod, speedLimit, false, "useTheNewLimit placeLay multipleRunner") > 0d) {
                            modifications++;
                            layExposureWasPlaced = true;
                        } else { // no modification made, nothing to be done
                        }
                    } else { // difference too small, nothing to be done
                    }
                }
                if (layExposureWasPlaced) {
                    this.updateOverallMarketAndRunnersExposureAndSums();
                } else { // no reason to update the marketAndRunnersExposureAndSums
                }
                for (final ManagedRunner managedRunner : runnersOrderedList) { // place exposure on back, upto min(layTotalExposure, layLimit)
                    final double marketTotalExposureWithoutRunner = this.calculateMarketTotalExposureWithoutRunner(managedRunner);
                    final double availableMarketExposureForBackBetsOnThisRunner = this.calculatedLimit - marketTotalExposureWithoutRunner;
                    final double backTotalExposure = managedRunner.getBackTotalExposure();
                    final double availableIdealBackExposure = Math.min(managedRunner.getIdealBackExposure() - backTotalExposure, availableMarketExposureForBackBetsOnThisRunner);
                    final double extraExposureExistingOnLay = Math.min(managedRunner.getLayTotalExposure(), managedRunner.getIdealLayExposure()) - backTotalExposure;
                    final double amountToPlaceOnBack = Math.min(availableIdealBackExposure, extraExposureExistingOnLay);
                    if (amountToPlaceOnBack >= .1d) {
                        if (managedRunner.placeOrder(Side.B, amountToPlaceOnBack, existingFunds, sendPostRequestRescriptMethod, speedLimit, false, "useTheNewLimit placeBack multipleRunner") > 0d) {
                            modifications++;
                            this.updateOverallMarketAndRunnersExposureAndSums();
                        } else { // no modification made, nothing to be done
                        }
                    } else { // amount negative or too small, won't place anything
                    }
                }
                for (final ManagedRunner managedRunner : runnersOrderedList) { // place exposure on back, upto maxLimit
                    final double marketTotalExposureWithoutRunner = this.calculateMarketTotalExposureWithoutRunner(managedRunner);
                    final double availableMarketExposureForBackBetsOnThisRunner = this.calculatedLimit - marketTotalExposureWithoutRunner;
                    final double backTotalExposure = managedRunner.getBackTotalExposure();
                    final double availableIdealBackExposure = Math.min(managedRunner.getIdealBackExposure() - backTotalExposure, availableMarketExposureForBackBetsOnThisRunner);
                    if (availableIdealBackExposure >= .1d) {
                        if (managedRunner.placeOrder(Side.B, availableIdealBackExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, false, "useTheNewLimit placeBackMax multipleRunner") > 0d) {
                            modifications++;
                            this.updateOverallMarketAndRunnersExposureAndSums();
                        } else { // no modification made, nothing to be done
                        }
                    } else { // amount negative or too small, won't place anything
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

    private synchronized void calculateProportionOfMarketLimitPerRunnerList(@NotNull final Collection<? extends ManagedRunner> runnersOrderedList) {
        // calculated proportions depend on the toBeUsedBackOdds
        final double sumOfStandardAmounts = runnersOrderedList.stream().filter(x -> Formulas.oddsAreUsable(x.getMinBackOdds()) && !x.errorBackSmallerThanLayOdds()).mapToDouble(x -> 1d / (x.getMinBackOdds() - 1d)).sum();
        for (final ManagedRunner managedRunner : runnersOrderedList) { // sumOfStandardAmounts should always be != 0d if at least one oddsAreUsable
            final double proportion = Formulas.oddsAreUsable(managedRunner.getMinBackOdds()) && !managedRunner.errorBackSmallerThanLayOdds() ? 1d / (managedRunner.getMinBackOdds() - 1d) / sumOfStandardAmounts : 0d;
            managedRunner.setProportionOfMarketLimitPerRunner(proportion);
        }
    }

    private synchronized void calculateIdealBackExposureList(@NotNull final ArrayList<? extends ManagedRunner> runnersOrderedList) {
        calculateProportionOfMarketLimitPerRunnerList(runnersOrderedList);
        // reset idealBackExposure
        for (final ManagedRunner managedRunner : runnersOrderedList) {
            managedRunner.setIdealBackExposure(0d);
            managedRunner.setIdealLayExposure(Formulas.oddsAreUsable(managedRunner.getMaxLayOdds()) && !managedRunner.errorBackSmallerThanLayOdds() ? this.calculatedLimit : 0d);
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
                    final double assignedExposure = managedRunner.addIdealBackExposure(idealExposure);
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

//        final StringBuilder stringBuilder = new StringBuilder("calculatedLimit=");
//        stringBuilder.append(this.calculatedLimit);
//        for (final ManagedRunner managedRunner : runnersOrderedList) {
//            stringBuilder.append(" B=").append(managedRunner.getIdealBackExposure()).append("/L=").append(managedRunner.getIdealLayExposure());
//        }
//        logger.info(stringBuilder.toString());
    }

    public synchronized double getMaxMarketLimit(@NotNull final ExistingFunds safetyLimits) {
        final double result;
        final double safetyLimit = safetyLimits.getDefaultMarketLimit(this.marketId);
        result = this.amountLimit >= 0d ? Math.min(this.amountLimit, safetyLimit) : safetyLimit;
        return result;
    }

    public synchronized boolean hasLimitEverBeenCalculated() {
        return this.calculatedLimitStamp != 0L;
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
                logger.error("trying to set negative calculated limit {} in setCalculatedLimit for: {}", this.calculatedLimit, Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
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
            logger.error("failure to calculate limits in getCalculatedLimit for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
            result = 0d;
        }

        return result;
    }

    private synchronized void updateRunnersExposure() {
        for (final ManagedRunner managedRunner : this.runners.values()) {
            managedRunner.updateExposure();
        }
    }

    synchronized boolean exposureCanBeCalculated(@NotNull final RulesManager rulesManager) {
        final long orderCacheStamp = SharedStatics.orderCache.initializedStamp.get();
        final long currentTime = System.currentTimeMillis();
        final boolean returnValue = this.market != null && orderCacheStamp > 0L && isSupported(rulesManager);
        if (returnValue) { // no error, no message to print
        } else if (Formulas.programHasRecentlyStarted(currentTime) || isVeryRecent() || !isEnabledMarket()) { // normal
        } else if (orderCacheStamp <= 0) {
            logger.info("trying to calculateExposure on orderCacheStamp {}, nothing will be done: {} {} - Market attached: {}", orderCacheStamp, this.marketId, this.marketName, this.market != null);
        } else if (this.market == null) {
            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.DEBUG, "trying to calculateExposure on managedMarket with no Market attached, possibly expired, nothing will be done: {} {}", this.marketId, this.marketName);
//            logger.info("trying to calculateExposure on managedMarket with no Market attached, possibly expired, nothing will be done: {} {}", this.marketId, this.marketName);
        } else {
            logger.error("trying to calculateExposure on unSupported managedMarket, nothing will be done: {} {}", this.marketId, this.marketName);
        }
        return returnValue;
    }

    public synchronized void calculateExposure(@NotNull final RulesManager rulesManager) {
        calculateExposure(rulesManager, false);
    }

    public synchronized void calculateExposure(@NotNull final RulesManager rulesManager, final boolean calculateAnyway) {
        if (calculateAnyway || exposureCanBeCalculated(rulesManager)) {
            updateRunnersExposure();
//            final boolean success = updateRunnersExposure();
//            this.marketMatchedExposure = Double.NaN;
//            this.marketTotalExposure = Double.NaN;
//            if (success) {
//            this.updateExposureSums();
            this.updateOverallMarketAndRunnersExposureAndSums();

//            } else { // nothing to do, default Double.NaN values will be retained
//            }
        } else { // for not supported I can't calculate the exposure; potential log messages have been written already in the exposureCanBeCalculated method
        }
    }

    private synchronized void updateExposureSums() { // updates matchedBackExposureSum & totalBackExposureSum
        double localMatchedBackExposureSum = 0d, localUnmatchedBackExposureSum = 0d, localTotalBackExposureSum = 0d, localTotalBackExposureSumConsideringCanceled = 0d;
        for (final ManagedRunner managedRunner : this.runners.values()) {
            localMatchedBackExposureSum += managedRunner.getBackMatchedExposure();
            localUnmatchedBackExposureSum += managedRunner.getBackUnmatchedExposure();
            localTotalBackExposureSum += managedRunner.getBackTotalExposure();
            localTotalBackExposureSumConsideringCanceled += managedRunner.getBackTotalExposureConsideringCanceled();
        }
        this.setMatchedBackExposureSum(localMatchedBackExposureSum);
        this.setUnmatchedBackExposureSum(localUnmatchedBackExposureSum);
        this.setTotalBackExposureSum(localTotalBackExposureSum);
        this.setTotalBackExposureSumConsideringCanceled(localTotalBackExposureSumConsideringCanceled);
    }

    private synchronized void updateOverallMarketAndRunnersExposureAndSums() {
        this.updateExposureSums();
        this.marketMatchedExposure = Double.NaN;
        this.marketTotalExposure = Double.NaN;
        this.marketTotalExposureConsideringCanceled = Double.NaN;
        for (final ManagedRunner managedRunner : this.runners.values()) {
            this.marketMatchedExposure = Double.isNaN(this.marketMatchedExposure) ? calculateRunnerMatchedExposure(managedRunner) : Math.max(this.marketMatchedExposure, calculateRunnerMatchedExposure(managedRunner));
            this.marketTotalExposure = Double.isNaN(this.marketTotalExposure) ? calculateRunnerTotalExposure(managedRunner) : Math.max(this.marketTotalExposure, calculateRunnerTotalExposure(managedRunner));
            this.marketTotalExposureConsideringCanceled = Double.isNaN(this.marketTotalExposureConsideringCanceled) ?
                                                          calculateRunnerTotalExposureConsideringCanceled(managedRunner) : Math.max(this.marketTotalExposureConsideringCanceled, calculateRunnerTotalExposureConsideringCanceled(managedRunner));
        } // end for
        if (Double.isNaN(this.marketMatchedExposure)) {
            this.marketMatchedExposure = 0d;
        }
        if (Double.isNaN(this.marketTotalExposure)) {
            this.marketTotalExposure = 0d;
        }
        if (Double.isNaN(this.marketTotalExposureConsideringCanceled)) {
            this.marketTotalExposureConsideringCanceled = 0d;
        }
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
        this.runnerMatchedExposureMap.put(runnerId, exposure);

        return exposure;
    }

    private synchronized double calculateRunnerTotalExposure(@NotNull final ManagedRunner managedRunner) {
        final double exposure = managedRunner.getLayTotalExposure() + this.totalBackExposureSum - managedRunner.getBackTotalExposure();
        final RunnerId runnerId = managedRunner.getRunnerId();
        this.runnerTotalExposureMap.put(runnerId, exposure);

        return exposure;
    }

    private synchronized double calculateRunnerTotalExposureConsideringCanceled(@NotNull final ManagedRunner managedRunner) {
        final double exposure = managedRunner.getLayTotalExposureConsideringCanceled() + this.totalBackExposureSumConsideringCanceled - managedRunner.getBackTotalExposureConsideringCanceled();
        final RunnerId runnerId = managedRunner.getRunnerId();
        this.runnerTotalExposureConsideringCanceledMap.put(runnerId, exposure);

        return exposure;
    }

    @SuppressWarnings("unused")
    private synchronized double getRunnerMatchedExposure(final RunnerId runnerId) {
        final Double exposureObject = this.runnerMatchedExposureMap.get(runnerId);
        final double exposure;
        if (exposureObject == null) {
            logger.error("null exposure during getRunnerMatchedExposure for {} in {}", runnerId, Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
            exposure = 0d;
        } else {
            exposure = exposureObject;
        }

        return exposure;
    }

    private synchronized double getRunnerTotalExposure(final ManagedRunner managedRunner) {
        final RunnerId runnerId = managedRunner == null ? null : managedRunner.getRunnerId();
        return getRunnerTotalExposure(runnerId);
    }

    private synchronized double getRunnerTotalExposure(final RunnerId runnerId) {
        final Double exposureObject = this.runnerTotalExposureMap.get(runnerId);
        final double exposure;
        if (exposureObject == null) {
            logger.error("null exposure during getRunnerTotalExposure for {} in {}", runnerId, Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
            exposure = 0d;
        } else {
            exposure = exposureObject;
        }
        return exposure;
    }

    private synchronized double calculateMarketTotalExposureWithoutRunner(final ManagedRunner excludedManagedRunner) {
        final RunnerId excludedRunnerId = excludedManagedRunner == null ? null : excludedManagedRunner.getRunnerId();
        double marketTotalExposureWithoutRunner = Double.NaN;
        for (final Map.Entry<RunnerId, Double> entry : this.runnerTotalExposureMap.entrySet()) {
            final RunnerId runnerId = entry.getKey();
            if (Objects.equals(runnerId, excludedRunnerId)) { // ignoring this runner
            } else {
                final Double runnerTotalExposure = entry.getValue();
                final double runnerTotalExposurePrimitive = runnerTotalExposure == null ? 0d : runnerTotalExposure;
                marketTotalExposureWithoutRunner = Double.isNaN(marketTotalExposureWithoutRunner) ? runnerTotalExposurePrimitive : Math.max(marketTotalExposureWithoutRunner, runnerTotalExposurePrimitive);
            }
        }
        if (Double.isNaN(marketTotalExposureWithoutRunner)) {
            marketTotalExposureWithoutRunner = 0d;
        }
        return marketTotalExposureWithoutRunner;
    }

    private synchronized double getRunnerTotalExposureConsideringCanceled(final ManagedRunner managedRunner) {
        final RunnerId runnerId = managedRunner == null ? null : managedRunner.getRunnerId();
        return getRunnerTotalExposureConsideringCanceled(runnerId);
    }

    private synchronized double getRunnerTotalExposureConsideringCanceled(final RunnerId runnerId) {
        final Double exposureObject = this.runnerTotalExposureConsideringCanceledMap.get(runnerId);
        final double exposure;
        if (exposureObject == null) {
            logger.error("null exposure during getRunnerTotalExposureConsideringCanceled for {} in {}", runnerId, Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
            exposure = 0d;
        } else {
            exposure = exposureObject;
        }

        return exposure;
    }

    synchronized boolean checkCancelAllUnmatchedBetsFlag(@NotNull final Method sendPostRequestRescriptMethod) {
        // only runs if the AtomicBoolean flag is set, normally when due to an error I can't calculate exposure
        final boolean shouldRun = this.cancelAllUnmatchedBets.getAndSet(false);
        if (shouldRun) {
            cancelAllUnmatchedBets(sendPostRequestRescriptMethod, "cancelAllUnmatchedBetsFlag");
        } else { // nothing to be done, flag for cancelling is not set
        }
        return shouldRun;
    }

    private synchronized int cancelAllUnmatchedBets(@NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        // cancel all unmatched bets, don't worry about exposure; generally used when, because of some error, I can't calculate exposure
//        int modifications = 0;
        this.cancelAllUnmatchedBets.set(false);

//        if (this.orderMarket == null) { // this is a normal branch, no orders are placed on this market, so nothing to be done in this branch
//        } else {
//            @NotNull final ArrayList<OrderMarketRunner> orderMarketRunners = this.orderMarket.getOrderMarketRunners();
//            for (final OrderMarketRunner orderMarketRunner : orderMarketRunners) {
//                if (orderMarketRunner == null) {
//                    logger.error("null orderMarketRunner in cancelAllUnmatchedBets for: {}", Generic.objectToString(this.orderMarket));
//                } else {
//                    final RunnerId runnerId = orderMarketRunner.getRunnerId();
//                    if (runnerId == null) {
//                        logger.error("null runnerId in orderMarketRunner: {}", Generic.objectToString(orderMarketRunner));
//                    } else {
//                        @NotNull final HashMap<String, Order> unmatchedOrders = orderMarketRunner.getUnmatchedOrders();
//                        for (final Order order : unmatchedOrders.values()) {
//                            if (order == null) {
//                                logger.error("null order in cancelAllUnmatchedBets for: {}", Generic.objectToString(this.orderMarket));
//                            } else {
//                                final Side side = order.getSide();
//                                final Double price = order.getP();
//                                final Double size = order.getSr();
//                                final String betId = order.getId();
//                                if (side == null || price == null || size == null || betId == null) {
//                                    logger.error("null order attributes in cancelAllUnmatchedBets for: {} {} {} {} {}", side, price, size, betId, Generic.objectToString(order));
//                                } else {
//                                    modifications += Generic.booleanToInt(order.cancelOrder(this.marketId, runnerId, sendPostRequestRescriptMethod));
//                                }
//                            }
//                        } // end for
//                    }
//                }
//            } // end for
//        }
        return SharedStatics.orderCache.cancelUnmatched(this.marketId, simpleGetRunnersMap(), sendPostRequestRescriptMethod, reason);
    }

    public synchronized boolean isSupported(@NotNull final RulesManager rulesManager) {
        final boolean result;
        if (this.market == null) {
            result = false;
            logger.info("trying to run managedMarket isSupported without attached market for: {} {}", this.marketId, this.marketName);
        } else {
            final MarketDefinition marketDefinition = this.market.getMarketDefinition();
            if (marketDefinition == null) {
                result = false;
                logger.error("marketDefinition null while run managedMarket isSupported for: {}", Generic.objectToString(this, "currentManageThread", "parentEvent", "market"));
            } else {
                final MarketBettingType marketBettingType = marketDefinition.getBettingType();
                final Integer nWinners = marketDefinition.getNumberOfWinners();
                final PriceLadderType priceLadderType = marketDefinition.getPriceLadderType();
                if (marketBettingType == MarketBettingType.ODDS && nWinners != null && nWinners == 1 && priceLadderType == PriceLadderType.CLASSIC) {
                    result = true;
                } else {
                    result = false;
                    if (isEnabledMarket()) {
                        logger.info("disabling unsupported managedMarket: {} {} {} {} {}", this.marketId, this.marketName, marketBettingType, nWinners, priceLadderType);
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

    synchronized void manageMarketStamp(final long currentTime) {
        this.manageMarketStamp = currentTime;
    }

    synchronized long getManageMarketStamp() {
        return this.manageMarketStamp;
    }

    // check that the limit bets per hour is not reached; only place bets if it's not reached; error message if limit reached; depending on how close to the limit I am, only orders with certain priority will be placed
    // priority depends on the type of modification and on the amount; some urgent orders might be placed in any case
    // manage market timeStamp; recent is 5 seconds; some non urgent actions that add towards hourly order limit will only be done if non recent, and the stamp will only get updated on this branch
    // the solution I found was to set the manageMarketPeriod in the BetFrequencyLimit class, depending on how close to the hourly limit I am
    public void manage(@NotNull final BetFrequencyLimit speedLimit, @NotNull final ExistingFunds existingFunds, @NotNull final RulesManager rulesManager, @NotNull final Method sendPostRequestRescriptMethod,
                       @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, final boolean emergencyExecute) {
        // intentionally not synchronized; isBeingManaged AtomicBoolean will be used to make sure it only runs once
        if (this.isEnabledMarket()) {
            final boolean previousValue = this.isBeingManaged.getAndSet(true);
            if (previousValue) { // was already beingManaged, another thread manages the market right now
                if (emergencyExecute) {
                    if (this.currentManageThread == null) {
                        logger.error("null currentManageThread for: {} {} {}", this.marketId, this.marketName, this.parentEventId);
                    } else {
                        this.currentManageThread.interruptSleep.set(true);
                    }
                } else { // no urgency, this thread will exit
                }
            } else { // market was not being managed, I'll manage it now
                this.currentManageThread = new ManagedMarketThread(this, speedLimit, existingFunds, rulesManager, marketCataloguesMap, sendPostRequestRescriptMethod);
                final Thread thread = new Thread(this.currentManageThread);
                thread.start();
            }
        } else { // not enabled, won't be managed
        }
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
        final ManagedMarket that = (ManagedMarket) obj;
        return Objects.equals(this.marketId, that.marketId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.marketId);
    }
}
