package info.fmro.shared.logic;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.market.MarketRunner;
import info.fmro.shared.stream.cache.order.OrderMarket;
import info.fmro.shared.stream.cache.order.OrderMarketRunner;
import info.fmro.shared.stream.definitions.Order;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass", "RedundantSuppression"})
public class ManagedRunner
        extends Exposure
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedRunner.class);
    private static final long serialVersionUID = 3553997020269888719L;
    private static final long attachOrderMarketRunnerRecentPeriod = 100L;
    private final String marketId;
    private final RunnerId runnerId;
    //    private final long selectionId;
    //    private final double handicap; // The handicap associated with the runner in case of Asian handicap markets (e.g. marketTypes ASIAN_HANDICAP_DOUBLE_LINE, ASIAN_HANDICAP_SINGLE_LINE) null otherwise.
    private double backAmountLimit, layAmountLimit; // amountLimits at 0d by default, which means no betting unless modified; negative limit means no limit
    private double minBackOdds = 1_001d, maxLayOdds = 1d; // defaults are unusable, which means no betting unless modified
    private double toBeUsedBackOdds = 1_001d, toBeUsedLayOdds = 1d;
    private double proportionOfMarketLimitPerRunner, idealBackExposure;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private transient MarketRunner marketRunner;
    @Nullable
    private transient OrderMarketRunner orderMarketRunner;
    private long attachOrderMarketRunnerStamp;

    public ManagedRunner(final String marketId, final RunnerId runnerId) {
        super();
        this.marketId = marketId;
        this.runnerId = runnerId;
    }

    public synchronized void setAttachOrderMarketRunnerStamp() {
        final long currentTime = System.currentTimeMillis();
        setAttachOrderMarketRunnerStamp(currentTime);
    }

    private synchronized void setAttachOrderMarketRunnerStamp(final long currentTime) {
        this.attachOrderMarketRunnerStamp = currentTime;
    }

    public synchronized long getAttachOrderMarketRunnerStamp() {
        return this.attachOrderMarketRunnerStamp;
    }

    public synchronized boolean isAttachOrderMarketRunnerRecent() {
        final long currentTime = System.currentTimeMillis();
        return isAttachOrderMarketRunnerRecent(currentTime);
    }

    private synchronized boolean isAttachOrderMarketRunnerRecent(final long currentTime) {
        return this.attachOrderMarketRunnerStamp + attachOrderMarketRunnerRecentPeriod > currentTime;
    }

    private synchronized boolean isActive(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache) {
        attachRunner(marketCache);
        return this.marketRunner != null && this.marketRunner.isActive();
    }

    public synchronized boolean isActive(final Market market) { // this is faster than the varian with marketCache
        attachRunner(market);
        return this.marketRunner != null && this.marketRunner.isActive();
    }

    private synchronized void attachRunner(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache) {
        if (this.marketRunner == null) {
            final Market market = Formulas.getMarket(this.marketId, marketCache);
            if (market == null) { // can happen when the market is added
            } else {
                attachRunner(market);
            }
        } else { // I already have the marketRunner, nothing to be done
        }
    }

    synchronized void attachRunner(final Market market) {
        if (this.marketRunner == null) {
            if (market == null) { // this actually happens in the client
                if (ProgramName.CLIENT == Generic.programName.get()) { // this actually happens in client
                } else {
                    logger.error("null market in attachRunner for: {} {}", this.marketId, Generic.objectToString(this.runnerId)); // I'll just print the error message; this error shouldn't happen and I don't think it's properly fixable
                }
            } else {
                this.marketRunner = market.getMarketRunner(this.runnerId);
                if (this.marketRunner == null) {
                    logger.error("no marketRunner found in attachRunner for: {} {}", Generic.objectToString(this), Generic.objectToString(market));
                }
            }
        } else { // I already have the marketRunner, nothing to be done
        }
    }

    @SuppressWarnings("unused")
    private synchronized void attachOrderRunner(@NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        attachOrderRunner(orderCache, false, System.currentTimeMillis());
    }

    private synchronized void attachOrderRunner(@NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, final boolean ignoreRecentFlag, final long currentTime) {
        if (this.orderMarketRunner == null) {
            if (ignoreRecentFlag || !isAttachOrderMarketRunnerRecent(currentTime)) {
                final OrderMarket orderMarket = Formulas.getOrderMarket(this.marketId, orderCache);
                attachOrderRunner(orderMarket, true, currentTime);
            } else { // !ignoreRecentFlag && isAttachOrderMarketRunnerRecent()
                // too recent, won't try to attach again
            }
        } else { // I already have the orderMarketRunner, nothing to be done
        }
    }

    @SuppressWarnings("unused")
    private synchronized void attachOrderRunner(final OrderMarket orderMarket) {
        attachOrderRunner(orderMarket, false, System.currentTimeMillis());
    }

    synchronized void attachOrderRunner(final OrderMarket orderMarket, final boolean ignoreRecentFlag, final long currentTime) {
        if (this.orderMarketRunner == null) {
            if (orderMarket == null) { // this is actually normal, no orders exist on the market
//                logger.error("null orderMarket in attachOrderRunner for: {}", Generic.objectToString(this)); // I'll just print the error message; this error shouldn't happen and I don't think it's properly fixable
//                logger.info("null orderMarket in attachOrderRunner for: {} {}", this.marketId, Generic.objectToString(this.runnerId));
//                Generic.alreadyPrintedMap.logOnce(Generic.DAY_LENGTH_MILLISECONDS, logger, LogLevel.INFO, "null orderMarket in attachOrderRunner for: {} {}", this.marketId, Generic.objectToString(this.runnerId));
            } else {
                if (ignoreRecentFlag || !isAttachOrderMarketRunnerRecent(currentTime)) {
                    this.orderMarketRunner = orderMarket.getOrderMarketRunner(this.runnerId);
                    if (this.orderMarketRunner == null) { // normal, it means no orders exist for this managedRunner, nothing else to be done
                    }
                } else { // !ignoreRecentFlag && isAttachOrderMarketRunnerRecent()
                    // too recent, won't try to attach again
                }
            }
        } else { // I already have the orderMarketRunner, nothing to be done
        }
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized MarketRunner getMarketRunner(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache) {
        attachRunner(marketCache);
        return this.marketRunner;
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized OrderMarketRunner getOrderMarketRunner(@NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        return getOrderMarketRunner(orderCache, false, System.currentTimeMillis());
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized OrderMarketRunner getOrderMarketRunner(@NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, final long currentTime) {
        return getOrderMarketRunner(orderCache, false, currentTime);
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized OrderMarketRunner getOrderMarketRunner(@NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, final boolean ignoreRecentFlag, final long currentTime) {
        attachOrderRunner(orderCache, ignoreRecentFlag, currentTime);
        return this.orderMarketRunner;
    }

    synchronized void processOrders(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        getOrderMarketRunner(orderCache); // updates the orderMarketRunner
        if (this.orderMarketRunner == null) {
            logger.error("null orderMarketRunner in ManagedRunner.processOrders for: {}", Generic.objectToString(this));
        } else {
            final RunnerId orderRunnerId = this.orderMarketRunner.getRunnerId();
            final RunnerId localRunnerId = this.runnerId;
            if (localRunnerId.equals(orderRunnerId)) {
//                this.updateExposure(orderMarketRunner.getExposure());
                this.getExposure(orderCache, pendingOrdersThread); // updates the exposure into this object
            } else {
                logger.error("not equal runnerIds in ManagedRunner.processOrders for: {} {} {}", Generic.objectToString(orderRunnerId), Generic.objectToString(localRunnerId), Generic.objectToString(this));
            }
        }
    }

    synchronized void resetOrderMarketRunner() {
        this.orderMarketRunner = null;
    }

    private synchronized void resetToBeUsedOdds() { // reset to unusable values
        this.toBeUsedBackOdds = 1_001d;
        this.toBeUsedLayOdds = 1d;
    }

    synchronized int checkRunnerLimits(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        // check the back/lay exposure limits for the runner, to make sure there's no error
        int exposureHasBeenModified = 0;
        getOrderMarketRunner(orderCache); // updates the orderMarketRunner
//        if (this.orderMarketRunner != null) {
        final double backTotalExposure = this.getBackTotalExposure(), layTotalExposure = this.getLayTotalExposure();
        final double localBackAmountLimit = this.getBackAmountLimit(), localLayAmountLimit = this.getLayAmountLimit();
        if (localBackAmountLimit + .1d < backTotalExposure || localLayAmountLimit + .1d < layTotalExposure) {
            logger.error("exposure limit has been breached back:{} {} lay:{} {} for runner: {}", localBackAmountLimit, backTotalExposure, localLayAmountLimit, layTotalExposure, Generic.objectToString(this));
            final double backExcessExposure = Math.max(0d, backTotalExposure - localBackAmountLimit), layExcessExposure = Math.max(0d, layTotalExposure - localLayAmountLimit);
            final double backMatchedExposure = this.getBackMatchedExposure(), layMatchedExposure = this.getLayMatchedExposure();
            final double backExcessMatchedExposure = Math.max(0d, backMatchedExposure - localBackAmountLimit), layExcessMatchedExposure = Math.max(0d, layMatchedExposure - localLayAmountLimit);
            if (this.orderMarketRunner != null) {
                exposureHasBeenModified += this.orderMarketRunner.cancelUnmatchedAmounts(backExcessExposure, layExcessExposure, pendingOrdersThread);
            } else { // orderMarketRunner is null if no orders exist on the runner yet
            }

            if (backExcessMatchedExposure >= .1d || layExcessMatchedExposure >= .1d) {
                logger.error("matched exposure has breached the limit back:{} {} lay:{} {} for runner: {}", localBackAmountLimit, backMatchedExposure, localLayAmountLimit, layMatchedExposure, Generic.objectToString(this));
                if (this.orderMarketRunner != null) {
                    exposureHasBeenModified += this.balanceMatchedAmounts(backExcessMatchedExposure, layExcessMatchedExposure, pendingOrdersThread);
                } else { // orderMarketRunner is null if no orders exist on the runner yet
                }
            } else { // matched amounts don't break the limits, nothing to be done
            }

            if (exposureHasBeenModified > 0) {
                this.processOrders(pendingOrdersThread, orderCache);
            } else {
                logger.error("exposureHasBeenModified {} not positive in checkRunnerLimits, while exposure limit is breached", exposureHasBeenModified);
            }
        } else { // no limit breach, nothing to do
        }
//        } else { // this is normal, orderMarketRunner will be null if there are no orders placed
//            logger.error("null orderMarketRunner in checkRunnerLimits: {}", Generic.objectToString(this));
//            logger.info("null orderMarketRunner in checkRunnerLimits: {} {}", this.marketId, Generic.objectToString(this.runnerId));
//            Generic.alreadyPrintedMap.logOnce(Generic.DAY_LENGTH_MILLISECONDS, logger, LogLevel.INFO, "null orderMarketRunner in checkRunnerLimits: {} {}", this.marketId, Generic.objectToString(this.runnerId));
//        }
        return exposureHasBeenModified;
    }

    synchronized int removeExposure(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        // check the back/lay exposure limits for the runner, to make sure there's no error
        int exposureHasBeenModified = 0;
        getOrderMarketRunner(orderCache); // updates the orderMarketRunner
        if (this.orderMarketRunner != null) {
            final double backMatchedExposure = this.getBackMatchedExposure(), layMatchedExposure = this.getLayMatchedExposure();
            final double backMatchedExcessExposure, layMatchedExcessExposure;
            if (backMatchedExposure > layMatchedExposure) {
                backMatchedExcessExposure = backMatchedExposure - layMatchedExposure;
                layMatchedExcessExposure = 0d;
            } else {
                backMatchedExcessExposure = 0d;
                layMatchedExcessExposure = layMatchedExposure - backMatchedExposure;
            }

            if (backMatchedExcessExposure < .1d && layMatchedExcessExposure < .1d) {
                exposureHasBeenModified += this.orderMarketRunner.cancelUnmatched(pendingOrdersThread);
            } else if (backMatchedExcessExposure >= .1d) {
                exposureHasBeenModified += this.orderMarketRunner.cancelUnmatched(Side.B, pendingOrdersThread);
                // matchedBackExposure, matchedLayExposure, unmatchedBackExposure, unmatchedBackProfit, unmatchedLayExposure, unmatchedLayProfit, tempBackExposure, tempBackProfit, tempLayExposure, tempLayProfit, tempBackCancel, tempLayCancel;
                final double backExcessExposureAfterTempIsConsidered =
                        backMatchedExcessExposure + this.getBackTempExposure() + this.getBackTempProfit() - this.getLayTempExposure() - this.getLayTempProfit();
                if (backExcessExposureAfterTempIsConsidered < .1d) {
                    exposureHasBeenModified += this.orderMarketRunner.cancelUnmatched(Side.L, pendingOrdersThread);
                } else {
                    final double excessOnTheOtherSideRemaining = this.orderMarketRunner.cancelUnmatchedExceptExcessOnTheOtherSide(Side.L, backExcessExposureAfterTempIsConsidered, pendingOrdersThread);
                    if (excessOnTheOtherSideRemaining < backExcessExposureAfterTempIsConsidered) {
                        exposureHasBeenModified++;
                    } else { // no modification was made
                    }

                    if (excessOnTheOtherSideRemaining >= .1d) {
                        exposureHasBeenModified += this.balanceMatchedAmounts(excessOnTheOtherSideRemaining, 0d, pendingOrdersThread);
                    } else { // problem solved, no more adjustments needed
                    }
                }
            } else if (layMatchedExcessExposure >= .1d) {
                exposureHasBeenModified += this.orderMarketRunner.cancelUnmatched(Side.L, pendingOrdersThread);
                // matchedBackExposure, matchedLayExposure, unmatchedBackExposure, unmatchedBackProfit, unmatchedLayExposure, unmatchedLayProfit, tempBackExposure, tempBackProfit, tempLayExposure, tempLayProfit, tempBackCancel, tempLayCancel;
                final double layExcessExposureAfterTempIsConsidered =
                        layMatchedExcessExposure + this.getLayTempExposure() + this.getLayTempProfit() - this.getBackTempExposure() - this.getBackTempProfit();
                if (layExcessExposureAfterTempIsConsidered < .1d) {
                    exposureHasBeenModified += this.orderMarketRunner.cancelUnmatched(Side.B, pendingOrdersThread);
                } else {
                    final double excessOnTheOtherSideRemaining = this.orderMarketRunner.cancelUnmatchedExceptExcessOnTheOtherSide(Side.B, layExcessExposureAfterTempIsConsidered, pendingOrdersThread);
                    if (excessOnTheOtherSideRemaining < layExcessExposureAfterTempIsConsidered) {
                        exposureHasBeenModified++;
                    } else { // no modification was made
                    }

                    if (excessOnTheOtherSideRemaining >= .1d) {
                        exposureHasBeenModified += this.balanceMatchedAmounts(0d, excessOnTheOtherSideRemaining, pendingOrdersThread);
                    } else { // problem solved, no more adjustments needed
                    }
                }
            } else {
                logger.error("this branch should not be reached in removeExposure: {} {} {}", backMatchedExcessExposure, layMatchedExcessExposure, Generic.objectToString(this));
            }
        } else {
            logger.info("null orderMarketRunner in removeExposure: {} {}", this.marketId, Generic.objectToString(this.runnerId));
        }
        return exposureHasBeenModified;
    }

    synchronized int calculateOdds(final double marketCalculatedLimit, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache,
                                   @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache) {
        // updates toBeUsedBackOdds and toBeUsedLayOdds
        int exposureHasBeenModified = 0;
        resetToBeUsedOdds();
        this.getMarketRunner(marketCache); // updates the marketRunner
        this.getOrderMarketRunner(orderCache); // updates the orderMarketRunner
        if (isActive(marketCache)) {
            if (this.marketRunner == null) {
                logger.error("trying to calculateOdds with null marketRunner for: {}", Generic.objectToString(this));
            } else {
                final HashMap<String, Order> unmatchedOrders = this.orderMarketRunner == null ? null : this.orderMarketRunner.getUnmatchedOrders();

                final double existingBackOdds = this.marketRunner.getBestAvailableBackPrice(unmatchedOrders, this.getBackAmountLimit(marketCalculatedLimit), currencyRate);
                final double existingLayOdds = this.marketRunner.getBestAvailableLayPrice(unmatchedOrders, this.getLayAmountLimit(marketCalculatedLimit), currencyRate);

                final double layNStepDifferentOdds = existingLayOdds == 0 ? 1_000d : info.fmro.shared.utility.Formulas.getNStepDifferentOdds(existingLayOdds, -1);
                final double backNStepDifferentOdds = existingBackOdds == 0 ? 1.01d : info.fmro.shared.utility.Formulas.getNStepDifferentOdds(existingBackOdds, 1);

                this.toBeUsedBackOdds = Math.max(this.getMinBackOdds(), layNStepDifferentOdds);
                this.toBeUsedLayOdds = Math.min(this.getMaxLayOdds(), backNStepDifferentOdds);

                // I'll allow unusable odds
//            this.toBeUsedBackOdds = Math.min(Math.max(this.toBeUsedBackOdds, 1.01d), 1_000d);
//            this.toBeUsedLayOdds = Math.min(Math.max(this.toBeUsedLayOdds, 1.01d), 1_000d);
            }
            if (this.orderMarketRunner != null) {
                // send order to cancel all back bets at worse odds than to be used ones / send order to cancel all back bets
                exposureHasBeenModified += info.fmro.shared.utility.Formulas.oddsAreUsable(this.toBeUsedBackOdds) ?
                                           this.orderMarketRunner.cancelUnmatched(Side.B, this.toBeUsedBackOdds, pendingOrdersThread) : this.orderMarketRunner.cancelUnmatched(Side.B, pendingOrdersThread);

                // send order to cancel all lay bets at worse odds than to be used ones / send order to cancel all lay bets
                exposureHasBeenModified += info.fmro.shared.utility.Formulas.oddsAreUsable(this.toBeUsedLayOdds) ?
                                           this.orderMarketRunner.cancelUnmatched(Side.L, this.toBeUsedLayOdds, pendingOrdersThread) : this.orderMarketRunner.cancelUnmatched(Side.L, pendingOrdersThread);
            } else { // this is actually normal if there are no orders on runner
//            logger.error("null orderMarketRunner in calculateOdds: {}", Generic.objectToString(this));
//            logger.info("null orderMarketRunner in calculateOdds: {} {}", this.marketId, Generic.objectToString(this.runnerId));
//            Generic.alreadyPrintedMap.logOnce(Generic.DAY_LENGTH_MILLISECONDS, logger, LogLevel.INFO, "null orderMarketRunner in calculateOdds: {} {}", this.marketId, Generic.objectToString(this.runnerId));
            }

            if (this.marketRunner != null && this.orderMarketRunner != null) {
                exposureHasBeenModified += this.cancelHardToReachOrders(pendingOrdersThread, currencyRate, orderCache, marketCache);
            } else { // error messages were printed previously, nothing to be done
            }
        } else { // won't manage inactive runners
        }

        return exposureHasBeenModified; // exposure will be recalculated outside the method, based on the return value indicating modifications have been made; but might be useless, as orders are just placed, not yet executed
    }

    private synchronized int cancelHardToReachOrders(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache,
                                                     @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache) {
        // find orders at good odds, but that are useless now as they are unlikely to be reached, and cancel them, updating runner exposure; the algorithm may not be simple
        int modifications = 0;
        this.getMarketRunner(marketCache); // updates the marketRunner
        this.getOrderMarketRunner(orderCache); // updates the orderMarketRunner

        if (this.orderMarketRunner == null || this.marketRunner == null) {
            logger.error("null orderMarketRunner or marketRunner in cancelHardToReachOrders for: {}", Generic.objectToString(this));
        } else {
            // back
            final TreeMap<Double, Double> unmatchedBackAmounts = this.orderMarketRunner.getUnmatchedBackAmounts(), availableLayAmounts = this.marketRunner.getAvailableToLay(currencyRate);
            Formulas.removeOwnAmountsFromAvailableTreeMap(availableLayAmounts, unmatchedBackAmounts);
            final NavigableSet<Double> unmatchedBackPrices = unmatchedBackAmounts.descendingKeySet();
            double worstOddsThatAreGettingCanceledBack = 0d;
            for (final Double unmatchedPrice : unmatchedBackPrices) {
                if (unmatchedPrice == null) {
                    logger.error("null unmatchedPrice in cancelHardToReachOrders for: {} {}", Generic.objectToString(unmatchedBackAmounts), Generic.objectToString(this));
                } else {
                    final Double unmatchedAmount = unmatchedBackAmounts.get(unmatchedPrice);
                    final double unmatchedAmountPrimitive = unmatchedAmount == null ? 0d : unmatchedAmount;
                    final SortedMap<Double, Double> smallerPriceAvailableAmounts = availableLayAmounts.headMap(unmatchedPrice);
                    double smallerSum = 0d;
                    for (final Double smallerAmount : smallerPriceAvailableAmounts.values()) {
                        final double smallerAmountPrimitive = smallerAmount == null ? 0d : smallerAmount;
                        smallerSum += smallerAmountPrimitive;
                    }

                    // simple condition for deciding that my amount can't be reached
                    if (smallerSum > 2d * unmatchedAmountPrimitive) {
                        worstOddsThatAreGettingCanceledBack = unmatchedPrice; // only the last price will matter, all odds that are better or same will get canceled
                    } else {
                        break; // breaks when 1 amount is not removed, and the next ones won't be removed
                    }
                }
            } // end for
            if (worstOddsThatAreGettingCanceledBack == 0d) { // nothing to be done
            } else {
                modifications += this.orderMarketRunner.cancelUnmatchedTooGoodOdds(Side.B, worstOddsThatAreGettingCanceledBack, pendingOrdersThread);
            }

            // lay
            final TreeMap<Double, Double> unmatchedLayAmounts = this.orderMarketRunner.getUnmatchedLayAmounts(), availableBackAmounts = this.marketRunner.getAvailableToBack(currencyRate);
            Formulas.removeOwnAmountsFromAvailableTreeMap(availableBackAmounts, unmatchedLayAmounts);
            final NavigableSet<Double> unmatchedLayPrices = unmatchedLayAmounts.descendingKeySet();
            double worstOddsThatAreGettingCanceledLay = 0d;
            for (final Double unmatchedPrice : unmatchedLayPrices) {
                if (unmatchedPrice == null) {
                    logger.error("null unmatchedPrice in cancelHardToReachOrders for: {} {}", Generic.objectToString(unmatchedLayAmounts), Generic.objectToString(this));
                } else {
                    final Double unmatchedAmount = unmatchedLayAmounts.get(unmatchedPrice);
                    final double unmatchedAmountPrimitive = unmatchedAmount == null ? 0d : unmatchedAmount;
                    final SortedMap<Double, Double> higherPriceAvailableAmounts = availableBackAmounts.tailMap(unmatchedPrice, false);
                    double higherSum = 0d;
                    for (final Double higherAmount : higherPriceAvailableAmounts.values()) {
                        final double higherAmountPrimitive = higherAmount == null ? 0d : higherAmount;
                        higherSum += higherAmountPrimitive;
                    }

                    // simple condition for deciding that my amount can't be reached
                    if (higherSum > 2d * unmatchedAmountPrimitive) {
                        worstOddsThatAreGettingCanceledLay = unmatchedPrice; // only the last price will matter, all odds that are better or same will get canceled
                    } else {
                        break; // breaks when 1 amount is not removed, and the next ones won't be removed
                    }
                }
            } // end for
            if (worstOddsThatAreGettingCanceledLay == 0d) { // nothing to be done
            } else {
                modifications += this.orderMarketRunner.cancelUnmatchedTooGoodOdds(Side.L, worstOddsThatAreGettingCanceledLay, pendingOrdersThread);
            }
        }

        return modifications;
    }

    public synchronized String getMarketId() {
        return this.marketId;
    }

    public synchronized double getToBeUsedBackOdds() {
        return this.toBeUsedBackOdds;
    }

    public synchronized double getToBeUsedLayOdds() {
        return this.toBeUsedLayOdds;
    }

    public synchronized double getTotalValue(@NotNull final AtomicDouble currencyRate, @NotNull final SynchronizedMap<? super String, ? extends Market> marketCache) {
        final double result;
        if (this.getMarketRunner(marketCache) != null) {
            result = this.marketRunner.getTvEUR(currencyRate);
        } else {
            logger.error("no marketRunner present in getTotalValue for: {}", Generic.objectToString(this));
            result = 0d;
        }
        return result;
    }

    synchronized double getLastTradedPrice(@NotNull final SynchronizedMap<? super String, ? extends Market> marketCache) {
        final double result;
        if (this.getMarketRunner(marketCache) != null) {
            result = this.marketRunner.getLtp();
        } else {
            logger.error("no marketRunner present in getLastTradedPrice for: {}", Generic.objectToString(this));
            result = 0d;
        }
        return result;
    }

    public synchronized RunnerId getRunnerId() {
        return this.runnerId;
    }

    public synchronized Long getSelectionId() {
        return this.runnerId.getSelectionId();
    }

    public synchronized Double getHandicap() {
        return this.runnerId.getHandicap();
    }

    public synchronized double simpleGetBackAmountLimit() {
        return this.backAmountLimit;
    }

    public synchronized double getBackAmountLimit() {
        return Formulas.oddsAreUsable(this.minBackOdds) ? this.backAmountLimit : 0d;
    }

    @Contract(pure = true)
    private synchronized double getBackAmountLimit(final double marketCalculatedLimit) {
        final double result, localBackAmountLimit = this.getBackAmountLimit();
        if (marketCalculatedLimit < 0) {
            result = localBackAmountLimit;
        } else if (localBackAmountLimit < 0) {
            result = marketCalculatedLimit;
        } else {
            result = Math.min(marketCalculatedLimit, localBackAmountLimit);
        }
        return result;
    }

    public synchronized double simpleGetLayAmountLimit() {
        return this.layAmountLimit;
    }

    public synchronized double getLayAmountLimit() {
        return Formulas.oddsAreUsable(this.maxLayOdds) ? this.layAmountLimit : 0d;
    }

    @Contract(pure = true)
    private synchronized double getLayAmountLimit(final double marketCalculatedLimit) {
        final double result, localLayAmountLimit = this.getLayAmountLimit();
        if (marketCalculatedLimit < 0) {
            result = localLayAmountLimit;
        } else if (localLayAmountLimit < 0) {
            result = marketCalculatedLimit;
        } else {
            result = Math.min(marketCalculatedLimit, localLayAmountLimit);
        }
        return result;
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized double getMinBackOdds() {
        return this.minBackOdds;
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized double getMaxLayOdds() {
        return this.maxLayOdds;
    }

    @SuppressWarnings("UnusedReturnValue")
    synchronized boolean setProportionOfMarketLimitPerRunner(final double newValue, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        final boolean modified;
        if (newValue >= 0d) {
            if (DoubleMath.fuzzyEquals(newValue, this.proportionOfMarketLimitPerRunner, .000001)) {
                modified = false;
            } else {
                this.proportionOfMarketLimitPerRunner = newValue;
                modified = true;
            }
        } else {
            modified = false;
            logger.error("trying to set negative proportionOfMarketLimitPerRunner {} for: {} {}", newValue, this.proportionOfMarketLimitPerRunner, Generic.objectToString(this));
        }
        if (modified) {
            marketsToCheck.add(this.marketId);
        } else { // no modification, nothing to be done
        }

        return modified;
    }

    synchronized double getProportionOfMarketLimitPerRunner() {
        return this.proportionOfMarketLimitPerRunner;
    }

    synchronized double addIdealBackExposure(final double idealBackExposureToBeAdded, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        return setIdealBackExposure(getIdealBackExposure() + idealBackExposureToBeAdded, marketsToCheck);
    }

    synchronized double setIdealBackExposure(final double newIdealBackExposure, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck) {
        final double newExposureAssigned, previousValue = this.idealBackExposure;
        if (newIdealBackExposure >= 0d) {
            if (Formulas.oddsAreUsable(this.minBackOdds)) {
                final double localBackAmountLimit = this.getBackAmountLimit();
                if (newIdealBackExposure >= localBackAmountLimit) {
                    newExposureAssigned = localBackAmountLimit - this.idealBackExposure;
                    this.idealBackExposure = localBackAmountLimit;
                } else {
                    newExposureAssigned = newIdealBackExposure - this.idealBackExposure;
                    this.idealBackExposure = newIdealBackExposure;
                }
            } else { // won't place back bets, I won't set the idealBackExposure
                this.idealBackExposure = 0d;
                newExposureAssigned = 0d; // limit in this case should be 0d, so it is reached
            }
        } else {
            logger.error("trying to set negative idealBackExposure {} for: {} {}", newIdealBackExposure, this.idealBackExposure, Generic.objectToString(this));
            this.idealBackExposure = 0d;
            newExposureAssigned = 0d; // in case of this strange error, I'll also return 0d, as I don't want to further try to setIdealBackExposure
        }
        if (DoubleMath.fuzzyEquals(this.idealBackExposure, previousValue, Formulas.CENT_TOLERANCE)) { // no significant modification, nothing to be done
        } else {
            marketsToCheck.add(this.marketId);
        }
        return newExposureAssigned;
    }

    synchronized double getIdealBackExposure() {
        return this.idealBackExposure;
    }

    synchronized boolean setBackAmountLimit(final double newBackAmountLimit, @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        final boolean modified;
        if (Double.isNaN(newBackAmountLimit)) {
            modified = false;
        } else  //noinspection FloatingPointEquality
            if (this.backAmountLimit == newBackAmountLimit) {
                modified = false;
            } else {
                this.backAmountLimit = newBackAmountLimit;
                modified = true;
            }

        if (modified) {
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setBackAmountLimit, this.marketId, this.runnerId, this.backAmountLimit));
            rulesHaveChanged.set(true);
        }
        return modified;
    }

    synchronized boolean setLayAmountLimit(final double newLayAmountLimit, @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        final boolean modified;
        if (Double.isNaN(newLayAmountLimit)) {
            modified = false;
        } else  //noinspection FloatingPointEquality
            if (this.layAmountLimit == newLayAmountLimit) {
                modified = false;
            } else {
                this.layAmountLimit = newLayAmountLimit;
                modified = true;
            }

        if (modified) {
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setLayAmountLimit, this.marketId, this.runnerId, this.layAmountLimit));
            rulesHaveChanged.set(true);
        }
        return modified;
    }

    synchronized boolean setMinBackOdds(final double newMinBackOdds, @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        final boolean modified;
        if (Double.isNaN(newMinBackOdds)) {
            modified = false;
        } else  //noinspection FloatingPointEquality
            if (this.minBackOdds == newMinBackOdds) {
                modified = false;
            } else {
                this.minBackOdds = newMinBackOdds;
                modified = true;
            }

        if (modified) {
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMinBackOdds, this.marketId, this.runnerId, this.minBackOdds));
            rulesHaveChanged.set(true);
        }
        return modified;
    }

    synchronized boolean setMaxLayOdds(final double newMaxLayOdds, @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        final boolean modified;
        if (Double.isNaN(newMaxLayOdds)) {
            modified = false;
        } else  //noinspection FloatingPointEquality
            if (this.maxLayOdds == newMaxLayOdds) {
                modified = false;
            } else {
                this.maxLayOdds = newMaxLayOdds;
                modified = true;
            }

        if (modified) {
            listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMaxLayOdds, this.marketId, this.runnerId, this.maxLayOdds));
            rulesHaveChanged.set(true);
        }
        return modified;
    }

    public synchronized double getTotalBackExposure() {
        return this.getBackMatchedExposure() + this.getBackUnmatchedExposure() + this.getBackTempExposure();
    }

    public synchronized double getTotalLayExposure() {
        return this.getLayMatchedExposure() + this.getLayUnmatchedExposure() + this.getLayTempExposure();
    }

    synchronized double placeOrder(final Side side, final double price, final double size, final OrdersThreadInterface pendingOrdersThread) {
        // exposure.setBackTotalExposure(matchedBackExposure + unmatchedBackExposure + tempBackExposure);
        // exposure.setLayTotalExposure(matchedLayExposure + unmatchedLayExposure + tempLayExposure);
        final double sizePlaced;

        if (side == Side.B) {
            final double backTotalExposure = this.getBackMatchedExposure() + this.getBackUnmatchedExposure() + this.getBackTempExposure();
            final double availableBackExposure = Math.max(0d, this.getBackAmountLimit() - backTotalExposure);
            sizePlaced = Math.min(availableBackExposure, size);
        } else if (side == Side.L) {
            final double layTotalExposure = this.getLayMatchedExposure() + this.getLayUnmatchedExposure() + this.getLayTempExposure();
            final double availableLayExposure = Math.max(0d, this.getLayAmountLimit() - layTotalExposure);
            final double reducedPrice = price - 1d; // protection against division by zero
            sizePlaced = reducedPrice == 0d ? size : Math.min(availableLayExposure / reducedPrice, size);
        } else {
            logger.error("unknown side {} {} {} during placeOrder for: {}", side, price, size, Generic.objectToString(this));
            sizePlaced = 0d;
        }

        return pendingOrdersThread.addPlaceOrder(this.marketId, this.runnerId, side, price, sizePlaced);
    }

    synchronized int balanceTotalAmounts(final double backExcessExposure, final double layExcessExposure, final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        int exposureHasBeenModified = 0;
        final double backUnmatchedExposureToBeCanceled = Math.min(backExcessExposure, this.getBackUnmatchedExposure()), layUnmatchedExposureToBeCanceled = Math.min(layExcessExposure, this.getLayUnmatchedExposure());
        exposureHasBeenModified += cancelUnmatchedAmounts(backUnmatchedExposureToBeCanceled, layUnmatchedExposureToBeCanceled, pendingOrdersThread, orderCache);
        exposureHasBeenModified += balanceMatchedAmounts(backExcessExposure - backUnmatchedExposureToBeCanceled, layExcessExposure - layUnmatchedExposureToBeCanceled, pendingOrdersThread);

        return exposureHasBeenModified;
    }

    synchronized int cancelUnmatchedAmounts(final double backExcessExposure, final double layExcessExposure, final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner(orderCache);
        return localOrderMarketRunner == null ? 0 : localOrderMarketRunner.cancelUnmatchedAmounts(backExcessExposure, layExcessExposure, pendingOrdersThread);
    }

    private synchronized int balanceMatchedAmounts(final double backExcessExposure, final double layExcessExposure, final OrdersThreadInterface pendingOrdersThread) {
        int exposureHasBeenModified = 0;
        if (backExcessExposure >= .1d) { // I need to place a lay bet, with profit that would cancel the excess
            // backExcessExposure - newLayProfit = newLayExposure
            // backExcessExposure - sizeToPlace = sizeToPlace*(toBeUsedLayOdds-1d)
            // backExcessExposure = sizeToPlace * toBeUsedLayOdds
            if (placeOrder(Side.L, this.toBeUsedLayOdds, this.toBeUsedLayOdds == 0d ? 0d : backExcessExposure / this.toBeUsedLayOdds, pendingOrdersThread) > 0d) {
                exposureHasBeenModified++;
            } else { // no modification, nothing to be done
            }
        } else { // no excess exposure present, nothing to do
        }

        if (layExcessExposure >= .1d) { // I need to place a back bet, with profit that would cancel the excess
            // layExcessExposure - newBackProfit = newBackExposure
            // layExcessExposure - sizeToPlace*(toBeUsedBackOdds-1d) = sizeToPlace
            // layExcessExposure = sizeToPlace * toBeUsedBackOdds
            if (placeOrder(Side.B, this.toBeUsedBackOdds, this.toBeUsedBackOdds == 0d ? 0d : layExcessExposure / this.toBeUsedBackOdds, pendingOrdersThread) > 0d) {
                exposureHasBeenModified++;
            } else { // no modification, nothing to be done
            }
        } else { // no excess exposure present, nothing to do
        }
        return exposureHasBeenModified;
    }

    private synchronized void getOrderMarketRunnerExposure(@NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, final long currentTime) {
        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner(orderCache, currentTime);
        if (localOrderMarketRunner == null) { // normal, nothing to do
        } else {
            localOrderMarketRunner.getMatchedExposure(this);
            localOrderMarketRunner.getUnmatchedExposureAndProfit(this);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized void getExposure(@NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache, final OrdersThreadInterface pendingOrdersThread) {
        final long currentTime = System.currentTimeMillis();
        this.getOrderMarketRunnerExposure(orderCache, currentTime); // updates matchedBackExposure and matchedLayExposure; updates unmatchedBackExposure/Profit and unmatchedLayExposure/Profit
        getTempExposure(pendingOrdersThread);
        this.timeStamp(); // it's fine to have the timeStamp before some chunk of the method, as this sequence is all synchronized; I need to timeStamp before the lines with getBack/LayUnmatchedProfit() & getBack/LayTempProfit()
        this.setBackUnmatchedProfit(this.getBackUnmatchedProfit() + this.getBackTempProfit());
        this.setLayUnmatchedProfit(this.getLayUnmatchedProfit() + this.getLayTempProfit());
    }

    synchronized void getTempExposure(final OrdersThreadInterface pendingOrdersThread) {
        if (pendingOrdersThread == null) { // I'm in the Client, I'm not using the pendingOrdersThread
        } else {
            pendingOrdersThread.checkTemporaryOrdersExposure(this.marketId, this.runnerId, this);
        }
    }

    @SuppressWarnings("unused")
    synchronized int cancelUnmatched(final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) { // cancel all unmatched orders
        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner(orderCache);
        return localOrderMarketRunner == null ? 0 : localOrderMarketRunner.cancelUnmatched(pendingOrdersThread);
    }

    synchronized int cancelUnmatched(final Side sideToCancel, final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) { // cancel all unmatched orders on that side
        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner(orderCache);
        return localOrderMarketRunner == null ? 0 : localOrderMarketRunner.cancelUnmatched(sideToCancel, pendingOrdersThread);
    }

    @SuppressWarnings("unused")
    synchronized int cancelUnmatched(final Side sideToCancel, final double worstNotCanceledOdds, final OrdersThreadInterface pendingOrdersThread, @NotNull final SynchronizedMap<? super String, ? extends OrderMarket> orderCache) {
        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner(orderCache);
        return localOrderMarketRunner == null ? 0 : localOrderMarketRunner.cancelUnmatched(sideToCancel, worstNotCanceledOdds, pendingOrdersThread);
    }

    public synchronized int update(final double newMinBackOdds, final double newMaxLayOdds, final double newBackAmountLimit, final double newLayAmountLimit, @NotNull final MarketsToCheckQueue<? super String> marketsToCheck,
                                   @NotNull final ListOfQueues listOfQueues, @NotNull final AtomicBoolean rulesHaveChanged) {
        int modified = 0;
        modified += Generic.booleanToInt(this.setMinBackOdds(newMinBackOdds, listOfQueues, rulesHaveChanged));
        modified += Generic.booleanToInt(this.setMaxLayOdds(newMaxLayOdds, listOfQueues, rulesHaveChanged));
        modified += Generic.booleanToInt(this.setBackAmountLimit(newBackAmountLimit, listOfQueues, rulesHaveChanged));
        modified += Generic.booleanToInt(this.setLayAmountLimit(newLayAmountLimit, listOfQueues, rulesHaveChanged));

        if (modified > 0) {
            marketsToCheck.add(this.marketId);
        } else { // nothing to be done
        }
        return modified;
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
        final ManagedRunner that = (ManagedRunner) obj;
        return Objects.equals(this.marketId, that.marketId) &&
               Objects.equals(this.runnerId, that.runnerId);
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(this.marketId, this.runnerId);
    }
}
