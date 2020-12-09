package info.fmro.shared.logic;

import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManagedMarketThread
        implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedMarketThread.class);
    @NotNull
    private final ManagedMarket managedMarket;
    @NotNull
    private final BetFrequencyLimit speedLimit;
    @NotNull
    private final ExistingFunds existingFunds;
    @NotNull
    private final RulesManager rulesManager;
    @SuppressWarnings("PackageVisibleField")
    @NotNull
    final AtomicBoolean interruptSleep = new AtomicBoolean();
    @NotNull
    private final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap;
    @NotNull
    private final Method sendPostRequestRescriptMethod;

    @SuppressWarnings("ConstructorWithTooManyParameters")
    ManagedMarketThread(@NotNull final ManagedMarket managedMarket, @NotNull final BetFrequencyLimit speedLimit, @NotNull final ExistingFunds existingFunds, @NotNull final RulesManager rulesManager,
                        @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final Method sendPostRequestRescriptMethod) {
        this.managedMarket = managedMarket;
        this.speedLimit = speedLimit;
        this.existingFunds = existingFunds;
        this.rulesManager = rulesManager;
        this.marketCataloguesMap = marketCataloguesMap;
        this.sendPostRequestRescriptMethod = sendPostRequestRescriptMethod;
    }

    @Override
    public void run() {
        if (this.managedMarket.exposureCanBeCalculated(this.rulesManager)) {
            final long startTime = System.currentTimeMillis();
            final long timeSinceLastManageMarketStamp = startTime - this.managedMarket.getManageMarketStamp();
            if (this.managedMarket.hasLimitEverBeenCalculated()) { // already calculated
            } else {
                this.rulesManager.calculateMarketLimits(this.existingFunds, this.marketCataloguesMap);
            }
            final long speedLimitPeriod = this.speedLimit.getManageMarketPeriod(this.managedMarket.simpleGetCalculatedLimit(), this.existingFunds);
            final long timeToSleep = speedLimitPeriod - timeSinceLastManageMarketStamp;
            logger.debug("manage enabled: {} timeSinceLastManage:{}s speedLimit:{}s", this.managedMarket.marketId, Generic.millisecondsToSecondsString(timeSinceLastManageMarketStamp), Generic.millisecondsToSecondsString(speedLimitPeriod));
            Generic.threadSleepSegmented(timeToSleep, 100L, SharedStatics.mustStop, this.interruptSleep);
//                if (timeSinceLastManageMarketStamp >= speedLimitPeriod) {
//                } else { // not enough time has passed since last manage, nothing to be done
//                }
            if (SharedStatics.mustStop.get()) { // program exiting, nothing to be done
            } else {
                final long timeManageHasStarted = System.currentTimeMillis();
//                if (timeToSleep > 0L) {
//                    startTime = System.currentTimeMillis();
//                } else { // have not slept, no need to update currentTime
//                }

//                if (timeToSleep > Exposure.recentPeriod - 1_000L) {
                this.managedMarket.calculateExposure(this.rulesManager);
//                } else { // I didn't sleep that much , no need to recalculate exposure
//                }

//                if (this.managedMarket.exposureIsRecent(currentTime)) {
//                        attachMarket(marketCache, listOfQueues, marketsToCheck, events, markets, rulesHaveChanged, marketCataloguesMap, programStartTime);
////                logger.info("managedMarket has attached market: {} {}", this.id, this.market != null);
//                        if (this.market != null) {
                this.managedMarket.manageMarketStamp(timeManageHasStarted);
//                this.managedMarket.attachOrderMarket(this.rulesManager, this.marketCataloguesMap);
                if (this.managedMarket.isSupported(this.rulesManager)) {
                    if (this.managedMarket.checkCancelAllUnmatchedBetsFlag(this.sendPostRequestRescriptMethod)) {
                        // all unmatched bets have been canceled already, not much more to be done
                        logger.info("manage cancelAllUnmatchedBetsFlag: {} {}", this.managedMarket.marketId, this.managedMarket.simpleGetMarketName());
                    } else {
//                            logger.info("manage market is supported: {} {}", this.id, this.marketName);
//                    final double calculatedLimit = this.getCalculatedLimit();
                        int exposureHasBeenModified = 0;
                        @NotNull final ArrayList<ManagedRunner> runnersList = this.managedMarket.simpleGetRunners();
                        for (final ManagedRunner runner : runnersList) {
                            runner.resetRemovedExposureDuringThisManageIteration(); // resets the variables that keep track of already removed exposure, that will be used later in this method
                            // removes orders that can be moved to better odds, hardToReachOrders, and unmatched orders at worse odds than limit
                            exposureHasBeenModified += runner.cancelBetsAtTooGoodOrTooBadOdds(this.existingFunds.currencyRate, this.sendPostRequestRescriptMethod);
                        }
                        if (exposureHasBeenModified > 0) {
                            this.managedMarket.calculateExposure(this.rulesManager);
                            exposureHasBeenModified = 0;
                        } else { // no need to calculateExposure
                        }

                        @NotNull final ArrayList<ManagedRunner> runnersOrderedList = this.managedMarket.createRunnersOrderedList();
                        if (this.managedMarket.isMarketLiveOrAlmostLive(this.rulesManager.marketsToCheck) && !this.managedMarket.isKeepAtInPlay()) {
                            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.INFO, "managedMarket isLiveOrAlmostLive: {} {} {}", this.managedMarket.marketId, this.managedMarket.simpleGetMarketName(), runnersOrderedList.size());
                            //noinspection UnusedAssignment
                            exposureHasBeenModified += this.managedMarket.removeExposureGettingOut(runnersOrderedList, this.existingFunds, this.sendPostRequestRescriptMethod, this.speedLimit);
                        } else {
//                                logger.info("manage market useTheNewLimit: {} {} runners:{}", this.managedMarket.marketId, this.managedMarket.marketName, runnersOrderedList.size());
                            for (final ManagedRunner runner : runnersList) {
                                exposureHasBeenModified += runner.checkRunnerLimits(this.existingFunds, this.sendPostRequestRescriptMethod, this.speedLimit);
                            }
                            if (exposureHasBeenModified > 0) {
                                this.managedMarket.calculateExposure(this.rulesManager);
                                exposureHasBeenModified = 0;
                            } else { // no need to calculateExposure
                            }

                            //noinspection UnusedAssignment
                            exposureHasBeenModified += this.managedMarket.useTheNewLimit(runnersOrderedList, this.rulesManager, this.existingFunds, this.sendPostRequestRescriptMethod, this.speedLimit);
                        }
                    }
                } else { // for not supported I can't calculate the limit
                    logger.error("trying to manage unSupported managedMarket, nothing will be done: {} {}", this.managedMarket.marketId, this.managedMarket.simpleGetMarketName());
                }
                this.managedMarket.lastCheckMarketRequestStamp.remove(timeManageHasStarted);
                final long existingCheckMarketRequestStamp = this.managedMarket.lastCheckMarketRequestStamp.get();
                if (existingCheckMarketRequestStamp > 0L) {
                    this.rulesManager.marketsToCheck.put(this.managedMarket.marketId, existingCheckMarketRequestStamp);
                } else { // no need to recheck the managedMarket now
                }
//                } else { // exposure not recent, error message was posted when isRecent was checked, nothing to be done
//                }
            }
        } else { // exposure can't be calculated, nothing to be done, log messages have been printed already
        }
        this.managedMarket.isBeingManaged.set(false);
    }
}
