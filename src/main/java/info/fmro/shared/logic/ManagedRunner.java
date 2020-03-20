package info.fmro.shared.logic;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.market.MarketCache;
import info.fmro.shared.stream.cache.market.MarketRunner;
import info.fmro.shared.stream.cache.order.OrderCache;
import info.fmro.shared.stream.cache.order.OrderMarket;
import info.fmro.shared.stream.cache.order.OrderMarketRunner;
import info.fmro.shared.stream.definitions.Order;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.OrdersThreadInterface;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
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

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass", "RedundantSuppression"})
public class ManagedRunner
        extends Exposure
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedRunner.class);
    private static final long serialVersionUID = 3553997020269888719L;
    private final String marketId;
    private final RunnerId runnerId;
    //    private final long selectionId;
    //    private final double handicap; // The handicap associated with the runner in case of Asian handicap markets (e.g. marketTypes ASIAN_HANDICAP_DOUBLE_LINE, ASIAN_HANDICAP_SINGLE_LINE) null otherwise.
    private double backAmountLimit, layAmountLimit; // amountLimits at 0d by default, which means no betting unless modified; negative limit means no limit
    private double minBackOdds = 1_001d, maxLayOdds = 1d; // defaults are unusable, which means no betting unless modified
    private double toBeUsedBackOdds = 1_001d, toBeUsedLayOdds = 1d;
    private double proportionOfMarketLimitPerRunner, idealBackExposure;
    private transient MarketRunner marketRunner;
    @Nullable
    private transient OrderMarketRunner orderMarketRunner;

    public ManagedRunner(final String marketId, final RunnerId runnerId) {
        super();
        this.marketId = marketId;
        this.runnerId = runnerId;
    }

    private synchronized void attachRunner(@NotNull final MarketCache marketCache) {
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

    private synchronized void attachOrderRunner(@NotNull final OrderCache orderCache) {
        if (this.orderMarketRunner == null) {
            final OrderMarket orderMarket = Formulas.getOrderMarket(this.marketId, orderCache);
            attachOrderRunner(orderMarket);
        } else { // I already have the orderMarketRunner, nothing to be done
        }
    }

    synchronized void attachOrderRunner(final OrderMarket orderMarket) {
        if (this.orderMarketRunner == null) {
            if (orderMarket == null) { // this is actually normal, no orders exist on the market
//                logger.error("null orderMarket in attachOrderRunner for: {}", Generic.objectToString(this)); // I'll just print the error message; this error shouldn't happen and I don't think it's properly fixable
//                logger.info("null orderMarket in attachOrderRunner for: {} {}", this.marketId, Generic.objectToString(this.runnerId));
//                Generic.alreadyPrintedMap.logOnce(Generic.DAY_LENGTH_MILLISECONDS, logger, LogLevel.INFO, "null orderMarket in attachOrderRunner for: {} {}", this.marketId, Generic.objectToString(this.runnerId));
            } else {
                this.orderMarketRunner = orderMarket.getOrderMarketRunner(this.runnerId);
                if (this.orderMarketRunner == null) { // normal, it means no orders exist for this managedRunner, nothing else to be done
                }
            }
        } else { // I already have the orderMarketRunner, nothing to be done
        }
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized MarketRunner getMarketRunner(@NotNull final MarketCache marketCache) {
        attachRunner(marketCache);
        return this.marketRunner;
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized OrderMarketRunner getOrderMarketRunner(@NotNull final OrderCache orderCache) {
        attachOrderRunner(orderCache);
        return this.orderMarketRunner;
    }

    synchronized void processOrders(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache) {
        getOrderMarketRunner(orderCache); // updates the orderMarketRunner
        if (this.orderMarketRunner == null) {
            logger.error("null orderMarketRunner in ManagedRunner.processOrders for: {}", Generic.objectToString(this));
        } else {
            final RunnerId orderRunnerId = this.orderMarketRunner.getRunnerId();
            final RunnerId localRunnerId = this.runnerId;
            if (localRunnerId.equals(orderRunnerId)) {
//                this.updateExposure(orderMarketRunner.getExposure());
                this.orderMarketRunner.getExposure(this, pendingOrdersThread); // updates the exposure into this object
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

    synchronized int checkRunnerLimits(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache) { // check the back/lay exposure limits for the runner, to make sure there's no error
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
                    exposureHasBeenModified += this.orderMarketRunner.balanceMatchedAmounts(localBackAmountLimit, localLayAmountLimit, this.getToBeUsedBackOdds(), this.getToBeUsedLayOdds(), backExcessMatchedExposure, layExcessMatchedExposure,
                                                                                            pendingOrdersThread);
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

    synchronized int removeExposure(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final OrderCache orderCache) { // check the back/lay exposure limits for the runner, to make sure there's no error
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
                        backMatchedExcessExposure + this.orderMarketRunner.getTempBackExposure() + this.orderMarketRunner.getTempBackProfit() - this.orderMarketRunner.getTempLayExposure() - this.orderMarketRunner.getTempLayProfit();
                if (backExcessExposureAfterTempIsConsidered < .1d) {
                    exposureHasBeenModified += this.orderMarketRunner.cancelUnmatched(Side.L, pendingOrdersThread);
                } else {
                    final double excessOnTheOtherSideRemaining = this.orderMarketRunner.cancelUnmatchedExceptExcessOnTheOtherSide(Side.L, backExcessExposureAfterTempIsConsidered, pendingOrdersThread);
                    if (excessOnTheOtherSideRemaining < backExcessExposureAfterTempIsConsidered) {
                        exposureHasBeenModified++;
                    } else { // no modification was made
                    }

                    if (excessOnTheOtherSideRemaining >= .1d) {
                        exposureHasBeenModified += this.orderMarketRunner.balanceMatchedAmounts(this.getBackAmountLimit(), this.getLayAmountLimit(), this.getToBeUsedBackOdds(), this.getToBeUsedLayOdds(), excessOnTheOtherSideRemaining, 0d,
                                                                                                pendingOrdersThread);
                    } else { // problem solved, no more adjustments needed
                    }
                }
            } else if (layMatchedExcessExposure >= .1d) {
                exposureHasBeenModified += this.orderMarketRunner.cancelUnmatched(Side.L, pendingOrdersThread);
                // matchedBackExposure, matchedLayExposure, unmatchedBackExposure, unmatchedBackProfit, unmatchedLayExposure, unmatchedLayProfit, tempBackExposure, tempBackProfit, tempLayExposure, tempLayProfit, tempBackCancel, tempLayCancel;
                final double layExcessExposureAfterTempIsConsidered =
                        layMatchedExcessExposure + this.orderMarketRunner.getTempLayExposure() + this.orderMarketRunner.getTempLayProfit() - this.orderMarketRunner.getTempBackExposure() - this.orderMarketRunner.getTempBackProfit();
                if (layExcessExposureAfterTempIsConsidered < .1d) {
                    exposureHasBeenModified += this.orderMarketRunner.cancelUnmatched(Side.B, pendingOrdersThread);
                } else {
                    final double excessOnTheOtherSideRemaining = this.orderMarketRunner.cancelUnmatchedExceptExcessOnTheOtherSide(Side.B, layExcessExposureAfterTempIsConsidered, pendingOrdersThread);
                    if (excessOnTheOtherSideRemaining < layExcessExposureAfterTempIsConsidered) {
                        exposureHasBeenModified++;
                    } else { // no modification was made
                    }

                    if (excessOnTheOtherSideRemaining >= .1d) {
                        exposureHasBeenModified += this.orderMarketRunner.balanceMatchedAmounts(this.getBackAmountLimit(), this.getLayAmountLimit(), this.getToBeUsedBackOdds(), this.getToBeUsedLayOdds(), 0d, excessOnTheOtherSideRemaining,
                                                                                                pendingOrdersThread);
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

    synchronized int calculateOdds(final double marketCalculatedLimit, @NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate, @NotNull final OrderCache orderCache, @NotNull final MarketCache marketCache) {
        // updates toBeUsedBackOdds and toBeUsedLayOdds
        int exposureHasBeenModified = 0;
        resetToBeUsedOdds();
        this.getMarketRunner(marketCache); // updates the marketRunner
        this.getOrderMarketRunner(orderCache); // updates the orderMarketRunner
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

        return exposureHasBeenModified; // exposure will be recalculated outside the method, based on the return value indicating modifications have been made; but might be useless, as orders are just placed, not yet executed
    }

    private synchronized int cancelHardToReachOrders(@NotNull final OrdersThreadInterface pendingOrdersThread, @NotNull final AtomicDouble currencyRate, @NotNull final OrderCache orderCache, @NotNull final MarketCache marketCache) {
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

    public synchronized double getTotalValue(@NotNull final AtomicDouble currencyRate, @NotNull final MarketCache marketCache) {
        final double result;
        if (this.getMarketRunner(marketCache) != null) {
            result = this.marketRunner.getTvEUR(currencyRate);
        } else {
            logger.error("no marketRunner present in getTotalValue for: {}", Generic.objectToString(this));
            result = 0d;
        }
        return result;
    }

    synchronized double getLastTradedPrice(@NotNull final MarketCache marketCache) {
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
    synchronized boolean setProportionOfMarketLimitPerRunner(final double newValue, @NotNull final RulesManager rulesManager) {
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
            rulesManager.addMarketToCheck(this.marketId);
        } else { // no modification, nothing to be done
        }

        return modified;
    }

    synchronized double getProportionOfMarketLimitPerRunner() {
        return this.proportionOfMarketLimitPerRunner;
    }

    synchronized double addIdealBackExposure(final double idealBackExposureToBeAdded, @NotNull final RulesManager rulesManager) {
        return setIdealBackExposure(getIdealBackExposure() + idealBackExposureToBeAdded, rulesManager);
    }

    synchronized double setIdealBackExposure(final double newIdealBackExposure, @NotNull final RulesManager rulesManager) {
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
            rulesManager.addMarketToCheck(this.marketId);
        }
        return newExposureAssigned;
    }

    synchronized double getIdealBackExposure() {
        return this.idealBackExposure;
    }

    synchronized boolean setBackAmountLimit(final double newBackAmountLimit, @NotNull final RulesManager rulesManager) {
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
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setBackAmountLimit, this.marketId, this.runnerId, this.backAmountLimit));
            rulesManager.rulesHaveChanged.set(true);
        }
        return modified;
    }

    synchronized boolean setLayAmountLimit(final double newLayAmountLimit, @NotNull final RulesManager rulesManager) {
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
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setLayAmountLimit, this.marketId, this.runnerId, this.layAmountLimit));
            rulesManager.rulesHaveChanged.set(true);
        }
        return modified;
    }

    synchronized boolean setMinBackOdds(final double newMinBackOdds, @NotNull final RulesManager rulesManager) {
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
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMinBackOdds, this.marketId, this.runnerId, this.minBackOdds));
            rulesManager.rulesHaveChanged.set(true);
        }
        return modified;
    }

    synchronized boolean setMaxLayOdds(final double newMaxLayOdds, @NotNull final RulesManager rulesManager) {
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
            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setMaxLayOdds, this.marketId, this.runnerId, this.maxLayOdds));
            rulesManager.rulesHaveChanged.set(true);
        }
        return modified;
    }

    public synchronized int update(final double newMinBackOdds, final double newMaxLayOdds, final double newBackAmountLimit, final double newLayAmountLimit, @NotNull final RulesManager rulesManager) {
        int modified = 0;
        modified += Generic.booleanToInt(this.setMinBackOdds(newMinBackOdds, rulesManager));
        modified += Generic.booleanToInt(this.setMaxLayOdds(newMaxLayOdds, rulesManager));
        modified += Generic.booleanToInt(this.setBackAmountLimit(newBackAmountLimit, rulesManager));
        modified += Generic.booleanToInt(this.setLayAmountLimit(newLayAmountLimit, rulesManager));

        if (modified > 0) {
            rulesManager.addMarketToCheck(this.marketId);
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
