package info.fmro.shared.logic;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.order.OrderMarket;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import info.fmro.shared.utility.SynchronizedSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ManagedMarketThread
        implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedMarketThread.class);
    @NotNull
    private final ManagedMarket managedMarket;
    @NotNull
    private final SynchronizedMap<? super String, ? extends Market> marketCache;
    @NotNull
    private final SynchronizedMap<? super String, ? extends OrderMarket> orderCache;
    @NotNull
    private final OrdersThreadInterface pendingOrdersThread;
    @NotNull
    private final AtomicDouble currencyRate;
    @NotNull
    private final BetFrequencyLimit speedLimit;
    @NotNull
    private final ExistingFunds safetyLimits;
    @NotNull
    private final ListOfQueues listOfQueues;
    @NotNull
    private final MarketsToCheckQueue<? super String> marketsToCheck;
    @NotNull
    private final SynchronizedSet<? super String> marketsForOutsideCheck;
    @NotNull
    private final AtomicBoolean rulesHaveChanged;
    @NotNull
    private final AtomicBoolean marketsMapModified;
    @NotNull
    private final AtomicBoolean newMarketsOrEventsForOutsideCheck;
    @NotNull
    private final ManagedEventsMap events;
    @NotNull
    private final SynchronizedMap<String, ManagedMarket> markets;
    @NotNull
    private final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap;
    @NotNull
    private final AtomicBoolean mustStop;
    @NotNull
    private final AtomicLong orderCacheInitializedFromStreamStamp;
    private final long programStartTime;

    @SuppressWarnings("ConstructorWithTooManyParameters")
    ManagedMarketThread(@NotNull final ManagedMarket managedMarket, @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache,
                        @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate, @NotNull final BetFrequencyLimit speedLimit, @NotNull final ExistingFunds safetyLimits,
                        @NotNull final ListOfQueues listOfQueues, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck, @NotNull final SynchronizedSet<? super String> marketsForOutsideCheck,
                        @NotNull final AtomicBoolean rulesHaveChanged, @NotNull final AtomicBoolean marketsMapModified, @NotNull final AtomicBoolean newMarketsOrEventsForOutsideCheck, @NotNull final ManagedEventsMap events,
                        @NotNull final SynchronizedMap<String, ManagedMarket> markets, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final AtomicBoolean mustStop,
                        @NotNull final AtomicLong orderCacheInitializedFromStreamStamp, final long programStartTime) {
        this.managedMarket = managedMarket;
        this.marketCache = marketCache;
        this.orderCache = orderCache;
        this.pendingOrdersThread = pendingOrdersThread;
        this.currencyRate = currencyRate;
        this.speedLimit = speedLimit;
        this.safetyLimits = safetyLimits;
        this.listOfQueues = listOfQueues;
        this.marketsToCheck = marketsToCheck;
        this.marketsForOutsideCheck = marketsForOutsideCheck;
        this.rulesHaveChanged = rulesHaveChanged;
        this.marketsMapModified = marketsMapModified;
        this.newMarketsOrEventsForOutsideCheck = newMarketsOrEventsForOutsideCheck;
        this.events = events;
        this.markets = markets;
        this.marketCataloguesMap = marketCataloguesMap;
        this.mustStop = mustStop;
        this.orderCacheInitializedFromStreamStamp = orderCacheInitializedFromStreamStamp;
        this.programStartTime = programStartTime;
    }

    @SuppressWarnings("OverlyNestedMethod")
    @Override
    public void run() {
        if (this.managedMarket.exposureCanBeCalculated(this.listOfQueues, this.marketsToCheck, this.marketsForOutsideCheck, this.rulesHaveChanged, this.marketsMapModified, this.newMarketsOrEventsForOutsideCheck, this.orderCacheInitializedFromStreamStamp,
                                                       this.programStartTime)) {
            long currentTime = System.currentTimeMillis();
            final long timeSinceLastManageMarketStamp = currentTime - this.managedMarket.manageMarketStamp;
            final long speedLimitPeriod = this.speedLimit.getManageMarketPeriod(this.managedMarket.calculatedLimit, this.safetyLimits);
            final long timeToSleep = speedLimitPeriod - timeSinceLastManageMarketStamp;
            logger.info("manage enabled: {} timeSinceLastManage:{} speedLimit:{}", this.managedMarket.marketId, Generic.addCommas(timeSinceLastManageMarketStamp), Generic.addCommas(speedLimitPeriod));
            Generic.threadSleepSegmented(timeToSleep, 100L, this.mustStop);
//                if (timeSinceLastManageMarketStamp >= speedLimitPeriod) {
//                } else { // not enough time has passed since last manage, nothing to be done
//                }
            if (this.mustStop.get()) { // program exiting, nothing to be done
            } else {
                if (timeToSleep > 0L) {
                    currentTime = System.currentTimeMillis();
                } else { // have not slept, no need to update currentTime
                }

//                if (timeToSleep > Exposure.recentPeriod - 1_000L) {
                this.managedMarket.calculateExposure(this.pendingOrdersThread, this.orderCache, this.programStartTime, this.listOfQueues, this.marketsToCheck, this.marketsForOutsideCheck, this.rulesHaveChanged, this.marketsMapModified,
                                                     this.newMarketsOrEventsForOutsideCheck, this.orderCacheInitializedFromStreamStamp);
//                } else { // I didn't sleep that much , no need to recalculate exposure
//                }

                if (this.managedMarket.exposureIsRecent(currentTime)) {
//                        attachMarket(marketCache, listOfQueues, marketsToCheck, events, markets, rulesHaveChanged, marketCataloguesMap, programStartTime);
////                logger.info("managedMarket has attached market: {} {}", this.id, this.market != null);
//                        if (this.market != null) {
                    this.managedMarket.manageMarketStamp(currentTime);
                    this.managedMarket.attachOrderMarket(this.orderCache, this.marketCache, this.listOfQueues, this.marketsToCheck, this.events, this.markets, this.rulesHaveChanged, this.marketCataloguesMap, this.programStartTime);
                    if (this.managedMarket.isSupported(this.listOfQueues, this.marketsToCheck, this.marketsForOutsideCheck, this.rulesHaveChanged, this.marketsMapModified, this.newMarketsOrEventsForOutsideCheck)) {
                        if (this.managedMarket.checkCancelAllUnmatchedBetsFlag(this.pendingOrdersThread)) { // all unmatched bets have been canceled already, not much more to be done
                            logger.info("manage cancelAllUnmatchedBetsFlag: {} {}", this.managedMarket.marketId, this.managedMarket.marketName);
                        } else {
//                            logger.info("manage market is supported: {} {}", this.id, this.marketName);
//                    final double calculatedLimit = this.getCalculatedLimit();
                            int exposureHasBeenModified = 0;
                            for (final ManagedRunner runner : this.managedMarket.runners.values()) {
                                exposureHasBeenModified +=
                                        runner.calculateOdds(this.managedMarket.calculatedLimit, this.pendingOrdersThread, this.currencyRate, this.orderCache, this.marketCache); // also removes unmatched orders at worse odds, and hardToReachOrders
                            }
                            if (exposureHasBeenModified > 0) {
                                this.managedMarket.calculateExposure(this.pendingOrdersThread, this.orderCache, this.programStartTime, this.listOfQueues, this.marketsToCheck, this.marketsForOutsideCheck, this.rulesHaveChanged, this.marketsMapModified,
                                                                     this.newMarketsOrEventsForOutsideCheck, this.orderCacheInitializedFromStreamStamp);
                                exposureHasBeenModified = 0;
                            } else { // no need to calculateExposure
                            }

                            @NotNull final ArrayList<ManagedRunner> runnersOrderedList = this.managedMarket.createRunnersOrderedList(this.marketCache);
                            if (this.managedMarket.isMarketAlmostLive(this.marketsToCheck)) {
                                logger.info("manage market isMarketAlmostLive: {} {} {}", this.managedMarket.marketId, this.managedMarket.marketName, runnersOrderedList.size());
                                //noinspection UnusedAssignment
                                exposureHasBeenModified += this.managedMarket.removeExposure(runnersOrderedList, this.orderCache, this.pendingOrdersThread);
                            } else {
                                logger.info("manage market useTheNewLimit: {} {} {}", this.managedMarket.marketId, this.managedMarket.marketName, runnersOrderedList.size());
                                for (final ManagedRunner runner : this.managedMarket.runners.values()) {
                                    exposureHasBeenModified += runner.checkRunnerLimits(this.pendingOrdersThread, this.orderCache);
                                }
                                if (exposureHasBeenModified > 0) {
                                    this.managedMarket.calculateExposure(this.pendingOrdersThread, this.orderCache, this.programStartTime, this.listOfQueues, this.marketsToCheck, this.marketsForOutsideCheck, this.rulesHaveChanged, this.marketsMapModified,
                                                                         this.newMarketsOrEventsForOutsideCheck, this.orderCacheInitializedFromStreamStamp);
                                    exposureHasBeenModified = 0;
                                } else { // no need to calculateExposure
                                }

                                //noinspection UnusedAssignment
                                exposureHasBeenModified += this.managedMarket.useTheNewLimit(runnersOrderedList, this.orderCache, this.pendingOrdersThread, this.marketsToCheck, this.listOfQueues, this.marketsForOutsideCheck, this.rulesHaveChanged,
                                                                                             this.marketsMapModified, this.newMarketsOrEventsForOutsideCheck, this.orderCacheInitializedFromStreamStamp, this.programStartTime);
                            }
                        }
                    } else { // for not supported I can't calculate the limit
                        logger.error("trying to manage unSupported managedMarket, nothing will be done: {} {}", this.managedMarket.marketId, this.managedMarket.marketName);
                    }
                } else { // exposure not recent, error message was posted when isRecent was checked, nothing to be done
                }
            }
        } else { // exposure can't be calculated, nothing to be done, log messages have been printed already
        }
        this.managedMarket.isBeingManaged.set(false);
    }
}
