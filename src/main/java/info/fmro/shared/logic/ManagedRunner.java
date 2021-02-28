package info.fmro.shared.logic;

import com.google.common.math.DoubleMath;
import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.enums.PrefSide;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.enums.RulesManagerModificationCommand;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.OrdersList;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.cache.market.MarketRunner;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass", "RedundantSuppression"})
public class ManagedRunner
        extends Exposure
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedRunner.class);
    @Serial
    private static final long serialVersionUID = 3553997020269888719L;
    // at 3d, it means my bets need to have at least a third the value of the bets placed by others on worse odds; lower value is more strict, higher is more permissive: 10d means minimum 10% of value, while .1d means 10x the value
    public static final double HARD_TO_REACH_THRESHOLD = 3d;
    // at 2d, it means the bets being placed need to have at least half the value of the bets placed by others on worse odds; lower value is more strict, higher is more permissive: 10d means minimum 10% of value, while .1d means 10x the value
    public static final double PLACE_BET_THRESHOLD = 2d;
    // at .1d, it means the bets that are at worse odds or equal odds compared to mine need to be more than 10% of my bets, else they will be considered tiny
    public static final double TINY_AMOUNTS_THRESHOLD = .1d;
    //    private static final long attachOrderMarketRunnerRecentPeriod = 100L;
    private final String marketId;
    private final RunnerId runnerId;
    //    private final long selectionId;
    //    private final double handicap; // The handicap associated with the runner in case of Asian handicap markets (e.g. marketTypes ASIAN_HANDICAP_DOUBLE_LINE, ASIAN_HANDICAP_SINGLE_LINE) null otherwise.
    private double backAmountLimit, layAmountLimit; // amountLimits at 0d by default, which means no betting unless modified; negative limit means no limit; I no longer allow negative values
    private double minBackOdds = 1_001d, maxLayOdds = 1d; // defaults are unusable, which means no betting unless modified
    //    private double toBeUsedBackOdds = 1_001d, toBeUsedLayOdds = 1d;
    private double proportionOfMarketLimitPerRunner, idealBackExposure, idealLayExposure;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private transient MarketRunner marketRunner;
    private final AtomicBoolean mandatoryPlace = new AtomicBoolean(), marketKeepAtInPlay;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private AtomicBoolean marketMandatoryPlace;
    private PrefSide prefSide = PrefSide.NONE;
//    @Nullable
//    private transient OrderMarketRunner orderMarketRunner;
//    private long attachOrderMarketRunnerStamp;

    public ManagedRunner(final String marketId, @NotNull final RunnerId runnerId, @NotNull final AtomicBoolean marketMandatoryPlace, @NotNull final AtomicBoolean marketKeepAtInPlay) {
        super();
        this.marketId = marketId;
        this.runnerId = runnerId;
        this.marketMandatoryPlace = marketMandatoryPlace;
        this.marketKeepAtInPlay = marketKeepAtInPlay;
    }

//    public synchronized void setAttachOrderMarketRunnerStamp() {
//        final long currentTime = System.currentTimeMillis();
//        setAttachOrderMarketRunnerStamp(currentTime);
//    }
//
//    private synchronized void setAttachOrderMarketRunnerStamp(final long currentTime) {
//        this.attachOrderMarketRunnerStamp = currentTime;
//    }
//
//    public synchronized long getAttachOrderMarketRunnerStamp() {
//        return this.attachOrderMarketRunnerStamp;
//    }
//
//    public synchronized boolean isAttachOrderMarketRunnerRecent() {
//        final long currentTime = System.currentTimeMillis();
//        return isAttachOrderMarketRunnerRecent(currentTime);
//    }
//
//    private synchronized boolean isAttachOrderMarketRunnerRecent(final long currentTime) {
//        return this.attachOrderMarketRunnerStamp + attachOrderMarketRunnerRecentPeriod > currentTime;
//    }

    synchronized boolean errorBackSmallerThanLayOdds() {
        final boolean error = this.minBackOdds < this.maxLayOdds;
        if (error) {
            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "errorBackSmallerThanLayOdds B:{} L:{} for: {} {}", this.minBackOdds, this.maxLayOdds, this.marketId, this.runnerId);
        } else { // normal, no error, nothing to print
        }
        return error;
    }

    private synchronized boolean isActive() {
        attachRunner();
        return this.marketRunner != null && this.marketRunner.isActive();
    }

    public synchronized boolean isActive(final Market market) { // this is faster than the varian with marketCache
        attachRunner(market);
        return this.marketRunner != null && this.marketRunner.isActive();
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isMandatoryPlace() { // not synchronized
        return this.marketMandatoryPlace.get() || this.mandatoryPlace.get();
    }

    private boolean isKeepAtInPlay() { // not synchronized
        return this.marketKeepAtInPlay.get();
    }

    public synchronized PrefSide getPrefSide() {
        return this.prefSide;
    }

    synchronized void setMandatoryPlace(final boolean mandatoryPlace, @NotNull final RulesManager rulesManager) {
        if (this.mandatoryPlace.get() == mandatoryPlace) { // no update needed
        } else {
            this.mandatoryPlace.set(mandatoryPlace);
            rulesManager.rulesHaveChanged.set(true);
            rulesManager.marketsMapModified.set(true);
            if (rulesManager.marketsForOutsideCheck.add(this.marketId)) {
                rulesManager.newMarketsOrEventsForOutsideCheck.set(true);
            }
            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());

            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setRunnerMandatoryPlace, this.marketId, this.runnerId, this.mandatoryPlace.get()));
        }
    }

    synchronized void hardSetMarketMandatoryPlace(final AtomicBoolean marketMandatoryPlaceAtomic, @NotNull final RulesManager rulesManager) {
        if (this.marketMandatoryPlace == marketMandatoryPlaceAtomic) { // no update needed
        } else {
            this.marketMandatoryPlace = marketMandatoryPlaceAtomic;
            rulesManager.rulesHaveChanged.set(true);
            rulesManager.marketsMapModified.set(true);
            if (rulesManager.marketsForOutsideCheck.add(this.marketId)) {
                rulesManager.newMarketsOrEventsForOutsideCheck.set(true);
            }
            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());
        }
    }

    public synchronized void setPrefSide(final PrefSide prefSide, @NotNull final RulesManager rulesManager) {
        if (this.prefSide == prefSide) { // no update needed
        } else if (prefSide == null) {
            logger.error("trying to set null prefSide for: {} {}", this.marketId, this.runnerId);
        } else {
            this.prefSide = prefSide;
            rulesManager.rulesHaveChanged.set(true);
            rulesManager.marketsMapModified.set(true);
            if (rulesManager.marketsForOutsideCheck.add(this.marketId)) {
                rulesManager.newMarketsOrEventsForOutsideCheck.set(true);
            }
            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());

            rulesManager.listOfQueues.send(new SerializableObjectModification<>(RulesManagerModificationCommand.setRunnerPrefSide, this.marketId, this.runnerId, this.prefSide));
        }
    }

    private synchronized void attachRunner() {
        if (this.marketRunner == null) {
            final Market market = Formulas.getMarket(this.marketId);
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
                if (ProgramName.CLIENT == SharedStatics.programName.get()) { // this actually happens in client
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

    @SuppressWarnings("WeakerAccess")
    public synchronized MarketRunner getMarketRunner() {
        attachRunner();
        return this.marketRunner;
    }

    synchronized void updateExposure() {
        SharedStatics.orderCache.updateExposure(this);
    }

    synchronized double checkRunnerLimits(@NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod, @NotNull final BetFrequencyLimit speedLimit) {
        // check the back/lay exposure limits for the runner, to make sure there's no error
        double exposureCanceledAndBalanced = 0d;
//        getOrderMarketRunner(orderCache); // updates the orderMarketRunner
//        if (this.orderMarketRunner != null) {
        final double backTotalExposure = this.getBackTotalExposure(), layTotalExposure = this.getLayTotalExposure();
//        final double localBackAmountLimit = this.getBackAmountLimit(), localLayAmountLimit = this.getLayAmountLimit();
        if (this.idealBackExposure + .1d < backTotalExposure || this.idealLayExposure + .1d < layTotalExposure) {
            if (SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get()) {
                SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.DEBUG, "exposure limit has been breached while betting_denied back:{}->{} lay:{}->{} for runner: {} {}",
                                                        this.idealBackExposure, backTotalExposure, this.idealLayExposure, layTotalExposure, this.getMarketId(), this.getRunnerId());
            } else {
                logger.error("exposure limit has been breached back:{}->{} lay:{}->{} for runner: {} {}", this.idealBackExposure, backTotalExposure, this.idealLayExposure, layTotalExposure, this.getMarketId(), this.getRunnerId());
            }
            final double backExcessExposure = Math.max(0d, backTotalExposure - this.idealBackExposure), layExcessExposure = Math.max(0d, layTotalExposure - this.idealLayExposure);
            final double backMatchedExposure = this.getBackMatchedExposure(), layMatchedExposure = this.getLayMatchedExposure();
            final double backExcessMatchedExposure = Math.max(0d, backMatchedExposure - this.idealBackExposure), layExcessMatchedExposure = Math.max(0d, layMatchedExposure - this.idealLayExposure);
//            if (this.orderMarketRunner != null) {
            exposureCanceledAndBalanced += cancelUnmatchedAmounts(backExcessExposure, layExcessExposure, sendPostRequestRescriptMethod, "checkRunnerLimits cancelUnmatched") > 0d ? 1 : 0;
//            } else { // orderMarketRunner is null if no orders exist on the runner yet
//            }
            if (backExcessMatchedExposure >= .1d || layExcessMatchedExposure >= .1d) {
                logger.error("matched exposure has breached the limit back:{}->{} lay:{}->{} for runner: {} {}", this.idealBackExposure, backMatchedExposure, this.idealLayExposure, layMatchedExposure, this.getMarketId(), this.getRunnerId());
//                if (this.orderMarketRunner != null) {
                exposureCanceledAndBalanced += this.balanceMatchedAmounts(backExcessMatchedExposure, layExcessMatchedExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, "checkRunnerLimits balanceMatched");
//                } else { // orderMarketRunner is null if no orders exist on the runner yet
//                }
            } else { // matched amounts don't break the limits, nothing to be done
            }

            if (exposureCanceledAndBalanced > 0d) {
                this.updateExposure();
            } else {
                if (SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get()) {
                    SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.DEBUG, "exposureHasBeenModified {} not positive in checkRunnerLimits, while exposure limit is breached and betting denied", exposureCanceledAndBalanced);
                } else if (this.rawBackTempExposure() > 0d || this.rawLayTempExposure() > 0d) {
                    logger.warn("exposureHasBeenModified {} not positive in checkRunnerLimits, while exposure limit is breached, backTemp={} layTemp={}", exposureCanceledAndBalanced, this.rawBackTempExposure(), this.rawLayTempExposure());
                } else {
                    logger.error("exposureHasBeenModified {} not positive in checkRunnerLimits, while exposure limit is breached", exposureCanceledAndBalanced);
                }
            }
        } else { // no limit breach, nothing to do
        }
//        } else { // this is normal, orderMarketRunner will be null if there are no orders placed
//            logger.error("null orderMarketRunner in checkRunnerLimits: {}", Generic.objectToString(this));
//            logger.info("null orderMarketRunner in checkRunnerLimits: {} {}", this.marketId, Generic.objectToString(this.runnerId));
//            SharedStatics.alreadyPrintedMap.logOnce(Generic.DAY_LENGTH_MILLISECONDS, logger, LogLevel.INFO, "null orderMarketRunner in checkRunnerLimits: {} {}", this.marketId, Generic.objectToString(this.runnerId));
//        }
        return exposureCanceledAndBalanced;
    }

    synchronized int removeExposureGettingOut(@NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod, @NotNull final BetFrequencyLimit speedLimit, final String reason) {
        // check the back/lay exposure limits for the runner, to make sure there's no error
        int exposureHasBeenModified = 0;
//        getOrderMarketRunner(orderCache); // updates the orderMarketRunner
        if (hasExposure()) {
            final double rawBackTempExposure = rawBackTempExposure(), rawLayTempExposure = rawLayTempExposure(), backMatchedExcessExposure = getBackMatchedExposure() - getLayMatchedExposure() + rawBackTempExposure - rawLayTempExposure,
                    rawTempExposure = rawBackTempExposure + rawLayTempExposure;

            // by using "- rawBackTempExposure() - rawLayTempExposure()" on the balanceMatchedAmounts I take the most conservative approach, to avoid placing unnecessary orders
            if (Math.abs(backMatchedExcessExposure) < .1d) {
                exposureHasBeenModified += SharedStatics.orderCache.cancelUnmatched(getMarketId(), getRunnerId(), this, sendPostRequestRescriptMethod, reason);
            } else if (backMatchedExcessExposure >= .1d) {
                exposureHasBeenModified += SharedStatics.orderCache.cancelUnmatched(getMarketId(), getRunnerId(), Side.B, this, sendPostRequestRescriptMethod, reason);
                final double exposureLeftUntilIStartToCancel = SharedStatics.orderCache.cancelUnmatchedExceptExcessOnTheOtherSide(getMarketId(), getRunnerId(), Side.L, backMatchedExcessExposure, this, sendPostRequestRescriptMethod, reason);
                if (exposureLeftUntilIStartToCancel == 0d) {
                    exposureHasBeenModified++;
                } else { // no modification was made
                }

                if (exposureLeftUntilIStartToCancel >= .1d) {
                    if (balanceMatchedAmounts(exposureLeftUntilIStartToCancel - rawTempExposure, 0d, existingFunds, sendPostRequestRescriptMethod, speedLimit, reason) > 0d) {
                        exposureHasBeenModified++;
                    } else { // no modification made
                    }
                } else { // problem solved, no more adjustments needed
                }
            } else { // layMatchedExcessExposure >= .1d
                exposureHasBeenModified += SharedStatics.orderCache.cancelUnmatched(getMarketId(), getRunnerId(), Side.L, this, sendPostRequestRescriptMethod, reason);
                final double exposureLeftUntilIStartToCancel = SharedStatics.orderCache.cancelUnmatchedExceptExcessOnTheOtherSide(getMarketId(), getRunnerId(), Side.B, -backMatchedExcessExposure, this, sendPostRequestRescriptMethod, reason);
                if (exposureLeftUntilIStartToCancel == 0d) {
                    exposureHasBeenModified++;
                } else { // no modification was made
                }

                if (exposureLeftUntilIStartToCancel >= .1d) {
                    if (this.balanceMatchedAmounts(0d, exposureLeftUntilIStartToCancel - rawTempExposure, existingFunds, sendPostRequestRescriptMethod, speedLimit, reason) > 0d) {
                        exposureHasBeenModified++;
                    } else { // no modification made
                    }
                } else { // problem solved, no more adjustments needed
                }
            }
        } else { // normal, often there's no exposure on a runner
//            logger.info("null orderMarketRunner in removeExposure: {} {}", this.marketId, Generic.objectToString(this.runnerId));
        }
        return exposureHasBeenModified;
    }

    synchronized int cancelBetsAtTooGoodOrTooBadOdds(@NotNull final AtomicDouble currencyRate, @NotNull final Method sendPostRequestRescriptMethod) {
        // example for back:
//        1.01--
//        1.02--
//        1.03--
//        1.04--i can move from here to better
//        1.05--place here
//        1.06 -hardToReach
//        1.07
//        hardToReach cancels from 1.06+
//        canGetBetter cancels from 1.04-

        int exposureHasBeenModified = 0;
        this.getMarketRunner(); // updates the marketRunner
        if (isActive()) {
            if (this.marketRunner == null) {
                logger.error("trying to calculateOdds with null marketRunner for: {}", Generic.objectToString(this));
            } else {
                final boolean mandatory = isMandatoryPlace();
                // back
                @NotNull final OrdersList unmatchedBackAmountsUnparsed = SharedStatics.orderCache.getUnmatchedBackAmounts(this.marketId, this.runnerId), availableLayAmountsUnparsed = this.marketRunner.getAvailableToLay(currencyRate);
                @NotNull final TreeMap<Double, Double> unmatchedBackAmounts = unmatchedBackAmountsUnparsed.getOrdersThatAppearInRecords(availableLayAmountsUnparsed), availableLayAmounts = availableLayAmountsUnparsed.getOrders();
                final double backBestOddsWhereICanMoveAmountsToBetterOdds = Formulas.getBestOddsWhereICanMoveAmountsToBetterOdds(this.marketId, this.runnerId, Side.B, unmatchedBackAmounts, availableLayAmounts);
                if (backBestOddsWhereICanMoveAmountsToBetterOdds == 0d) { // nothing to be done
                } else {
                    exposureHasBeenModified += SharedStatics.orderCache.cancelUnmatchedAtWorseOdds(this.marketId, this.runnerId, Side.B, backBestOddsWhereICanMoveAmountsToBetterOdds, this, sendPostRequestRescriptMethod,
                                                                                                   true, "ICanMoveAmountsToBetterOdds");
                }
                final double minWorstOdds = mandatory ? Formulas.getNextOddsUp(this.minBackOdds, Side.B) : 1d;
                final double worstOddsThatAreGettingCanceledBack = Formulas.getWorstOddsThatCantBeReached(this.marketId, this.runnerId, Side.B, unmatchedBackAmounts, availableLayAmounts, false, false, minWorstOdds);
                if (worstOddsThatAreGettingCanceledBack == 0d) { // nothing to be done
                } else {
                    exposureHasBeenModified += SharedStatics.orderCache.cancelUnmatchedTooGoodOdds(this.marketId, this.runnerId, Side.B, worstOddsThatAreGettingCanceledBack, this, sendPostRequestRescriptMethod,
                                                                                                   "cancelBetsAtTooGoodOrTooBadOdds unmatchedTooGood");
                }

                // lay
                @NotNull final OrdersList unmatchedLayAmountsUnparsed = SharedStatics.orderCache.getUnmatchedLayAmounts(this.marketId, this.runnerId), availableBackAmountsUnparsed = this.marketRunner.getAvailableToBack(currencyRate);
                @NotNull final TreeMap<Double, Double> unmatchedLayAmounts = unmatchedLayAmountsUnparsed.getOrdersThatAppearInRecords(availableBackAmountsUnparsed), availableBackAmounts = availableBackAmountsUnparsed.getOrders();
                final double layBestOddsWhereICanMoveAmountsToBetterOdds = Formulas.getBestOddsWhereICanMoveAmountsToBetterOdds(this.marketId, this.runnerId, Side.L, unmatchedLayAmounts, availableBackAmounts);
                if (layBestOddsWhereICanMoveAmountsToBetterOdds == 0d) { // nothing to be done
                } else {
                    exposureHasBeenModified += SharedStatics.orderCache.cancelUnmatchedAtWorseOdds(this.marketId, this.runnerId, Side.L, layBestOddsWhereICanMoveAmountsToBetterOdds, this, sendPostRequestRescriptMethod,
                                                                                                   true, "ICanMoveAmountsToBetterOdds");
                }
                final double maxWorstOdds = mandatory ? Formulas.getNextOddsDown(this.maxLayOdds, Side.L) : 1_001d;
                final double worstOddsThatAreGettingCanceledLay = Formulas.getWorstOddsThatCantBeReached(this.marketId, this.runnerId, Side.L, unmatchedLayAmounts, availableBackAmounts, false, false, maxWorstOdds);
                if (worstOddsThatAreGettingCanceledLay == 0d) { // nothing to be done
                } else {
                    exposureHasBeenModified += SharedStatics.orderCache.cancelUnmatchedTooGoodOdds(this.marketId, this.runnerId, Side.L, worstOddsThatAreGettingCanceledLay, this, sendPostRequestRescriptMethod,
                                                                                                   "cancelBetsAtTooGoodOrTooBadOdds unmatchedTooGood");
                }

                // send order to cancel all back bets at worse odds than to be used ones / send order to cancel all back bets
                exposureHasBeenModified += Formulas.oddsAreUsable(this.minBackOdds) ?
                                           SharedStatics.orderCache.cancelUnmatched(this.marketId, this.runnerId, Side.B, this.minBackOdds, this, sendPostRequestRescriptMethod, "worseOddsThanToBeUsed") :
                                           SharedStatics.orderCache.cancelUnmatched(this.marketId, this.runnerId, Side.B, this, sendPostRequestRescriptMethod, "unusableOddsToBeUsed");
                // send order to cancel all lay bets at worse odds than to be used ones / send order to cancel all lay bets
                exposureHasBeenModified += Formulas.oddsAreUsable(this.maxLayOdds) ?
                                           SharedStatics.orderCache.cancelUnmatched(this.marketId, this.runnerId, Side.L, this.maxLayOdds, this, sendPostRequestRescriptMethod, "worseOddsThanToBeUsed") :
                                           SharedStatics.orderCache.cancelUnmatched(this.marketId, this.runnerId, Side.L, this, sendPostRequestRescriptMethod, "unusableOddsToBeUsed");
            }
        } else { // won't manage inactive runners
        }
        return exposureHasBeenModified; // exposure will be recalculated outside the method, based on the return value indicating modifications have been made; but might be useless, as orders are just placed, not yet executed
    }

    public String getMarketId() {
        return this.marketId;
    }

    public synchronized double getTotalValue(@NotNull final AtomicDouble currencyRate) {
        final double result;
        if (this.getMarketRunner() != null) {
            result = this.marketRunner.getTvEUR(currencyRate);
        } else {
            logger.error("no marketRunner present in getTotalValue for: {}", Generic.objectToString(this));
            result = 0d;
        }
        return result;
    }

    synchronized double getLastTradedPrice() {
        final double result;
        if (this.getMarketRunner() != null) {
            result = this.marketRunner.getLtp();
        } else {
            logger.error("no marketRunner present in getLastTradedPrice for: {}", Generic.objectToString(this));
            result = 0d;
        }
        return result;
    }

    public RunnerId getRunnerId() {
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
        return Formulas.oddsAreUsable(this.minBackOdds) ? Math.max(this.backAmountLimit, 0d) : 0d;
    }

    public synchronized double simpleGetLayAmountLimit() {
        return this.layAmountLimit;
    }

    public synchronized double getLayAmountLimit() {
        return Formulas.oddsAreUsable(this.maxLayOdds) ? Math.max(this.layAmountLimit, 0d) : 0d;
    }

//    @Contract(pure = true)
//    private synchronized double getBackAmountLimit(final double marketCalculatedLimit) {
//        final double result, localBackAmountLimit = this.getBackAmountLimit();
//        if (marketCalculatedLimit < 0) {
//            result = localBackAmountLimit;
//        } else if (localBackAmountLimit < 0) {
//            result = marketCalculatedLimit;
//        } else {
//            result = Math.min(marketCalculatedLimit, localBackAmountLimit);
//        }
//        return result;
//    }
//
//    @Contract(pure = true)
//    private synchronized double getLayAmountLimit(final double marketCalculatedLimit) {
//        final double result, localLayAmountLimit = this.getLayAmountLimit();
//        if (marketCalculatedLimit < 0) {
//            result = localLayAmountLimit;
//        } else if (localLayAmountLimit < 0) {
//            result = marketCalculatedLimit;
//        } else {
//            result = Math.min(marketCalculatedLimit, localLayAmountLimit);
//        }
//        return result;
//    }

    public synchronized double getOddsLimit(final Side side) {
        final double result;
        if (side == Side.B) {
            result = getMinBackOdds();
        } else if (side == Side.L) {
            result = getMaxLayOdds();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
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
    synchronized boolean setProportionOfMarketLimitPerRunner(final double newValue) {
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
//        if (modified) {
//            marketsToCheck.put(this.marketId, System.currentTimeMillis());
//        } else { // no modification, nothing to be done
//        }

        return modified;
    }

    synchronized double getProportionOfMarketLimitPerRunner() {
        return this.proportionOfMarketLimitPerRunner;
    }

    synchronized double addIdealBackExposure(final double idealBackExposureToBeAdded) {
        return setIdealBackExposure(getIdealBackExposure() + idealBackExposureToBeAdded);
    }

    synchronized double setIdealBackExposure(final double newIdealBackExposure) {
        final double newExposureAssigned;
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
//        if (DoubleMath.fuzzyEquals(this.idealBackExposure, previousValue, Formulas.CENT_TOLERANCE)) { // no significant modification, nothing to be done
//        } else {
//            marketsToCheck.put(this.marketId, System.currentTimeMillis());
//        }
        return newExposureAssigned;
    }

    @SuppressWarnings("UnusedReturnValue")
    synchronized double setIdealLayExposure(final double newIdealLayExposure) {
        final double newExposureAssigned;
        if (newIdealLayExposure >= 0d) {
            if (Formulas.oddsAreUsable(this.maxLayOdds)) {
                final double localLayAmountLimit = this.getLayAmountLimit();
                if (newIdealLayExposure >= localLayAmountLimit) {
                    newExposureAssigned = localLayAmountLimit - this.idealLayExposure;
                    this.idealLayExposure = localLayAmountLimit;
                } else {
                    newExposureAssigned = newIdealLayExposure - this.idealLayExposure;
                    this.idealLayExposure = newIdealLayExposure;
                }
            } else { // won't place lay bets, I won't set the idealLayExposure
                this.idealLayExposure = 0d;
                newExposureAssigned = 0d; // limit in this case should be 0d, so it is reached
            }
        } else {
            logger.error("trying to set negative idealLayExposure {} for: {} {}", newIdealLayExposure, this.idealLayExposure, Generic.objectToString(this));
            this.idealLayExposure = 0d;
            newExposureAssigned = 0d; // in case of this strange error, I'll also return 0d, as I don't want to further try to setIdealLayExposure
        }
//        if (DoubleMath.fuzzyEquals(this.idealLayExposure, previousValue, Formulas.CENT_TOLERANCE)) { // no significant modification, nothing to be done
//        } else {
//            marketsToCheck.put(this.marketId, System.currentTimeMillis());
//        }
        return newExposureAssigned;
    }

    public synchronized double getIdealExposure(final Side side) {
        final double result;
        if (side == Side.B) {
            result = getIdealBackExposure();
        } else if (side == Side.L) {
            result = getIdealLayExposure();
        } else {
            logger.error("STRANGE unknown side: {}", side);
            result = Double.NaN;
        }
        return result;
    }

    public synchronized double getIdealBackExposure() {
        return this.idealBackExposure;
    }

    public synchronized double getIdealLayExposure() {
        return this.idealLayExposure;
    }

    synchronized boolean setBackAmountLimit(final double newBackAmountLimit, @NotNull final RulesManager rulesManager) {
        final boolean modified;
        if (Double.isNaN(newBackAmountLimit)) {
            modified = false;
        } else  //noinspection FloatingPointEquality
            if (this.backAmountLimit == newBackAmountLimit) {
                modified = false;
            } else if (newBackAmountLimit < 0d) {
                logger.error("trying to set negative backLimit {} for: {}", newBackAmountLimit, Generic.objectToString(this));
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
            } else if (newLayAmountLimit < 0d) {
                logger.error("trying to set negative layLimit {} for: {}", newLayAmountLimit, Generic.objectToString(this));
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

    synchronized double placeOrder(final Side side, final double exposureIWantToPlace, @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod, @NotNull final BetFrequencyLimit speedLimit,
                                   final boolean isBalancingToRemoveExistingExposure, final String reason) {
        final double price;
        if (side == Side.B) {
            price = this.minBackOdds;
        } else if (side == Side.L) {
            price = this.maxLayOdds;
        } else {
            logger.error("unknown side {} {} during placeOrder for: {} {}", side, exposureIWantToPlace, reason, Generic.objectToString(this));
            price = 0d;
        }
        return placeOrder(side, price, exposureIWantToPlace, existingFunds, sendPostRequestRescriptMethod, speedLimit, isBalancingToRemoveExistingExposure, reason);
    }

    private synchronized double placeOrder(final Side side, final double price, final double exposureIWantToPlace, @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod,
                                           @NotNull final BetFrequencyLimit speedLimit, final boolean isBalancingToRemoveExistingExposure, final String reason) {
        // exposure.setBackTotalExposure(matchedBackExposure + unmatchedBackExposure + tempBackExposure);
        // exposure.setLayTotalExposure(matchedLayExposure + unmatchedLayExposure + tempLayExposure);
        final double sizePlaced;
        if (this.marketRunner == null) {
            logger.error("null marketRunner in placeOrder for: {} {} {} {} odds:{} exposure:{}", this.marketId, this.runnerId, side, reason, price, exposureIWantToPlace);
            sizePlaced = 0d;
        } else if (Formulas.oddsAreUsable(price)) {
            @NotNull final OrdersList myUnmatchedAmountsUnparsed, availableAmountsOnOppositeSideUnparsed;
            @NotNull final TreeMap<Double, Double> myUnmatchedAmounts, availableAmountsOnOppositeSide;
            final double exposureICanPlace, oddsThatCanBeUsed;
            final double totalValueMatched = this.marketRunner.getTvEUR(existingFunds.currencyRate);

            if (side == Side.B) {
                myUnmatchedAmountsUnparsed = SharedStatics.orderCache.getUnmatchedBackAmounts(this.marketId, this.runnerId);
                availableAmountsOnOppositeSideUnparsed = this.marketRunner.getAvailableToLay(existingFunds.currencyRate);
                myUnmatchedAmounts = myUnmatchedAmountsUnparsed.getOrdersThatAppearInRecords(availableAmountsOnOppositeSideUnparsed);
                availableAmountsOnOppositeSide = availableAmountsOnOppositeSideUnparsed.getOrders();
//                final double backTotalExposure = this.getBackMatchedExposure() + this.getBackUnmatchedExposure() + this.getBackTempExposure();
                final double backTotalExposure = this.getBackTotalExposure();
                final double availableBackExposure = Math.max(0d, this.idealBackExposure - backTotalExposure);
                exposureICanPlace = Math.min(availableBackExposure, exposureIWantToPlace);
                oddsThatCanBeUsed = Formulas.getBestOddsThatCanBeUsed(this.marketId, this.runnerId, side, exposureICanPlace, myUnmatchedAmounts, availableAmountsOnOppositeSide, totalValueMatched);
            } else if (side == Side.L) {
                myUnmatchedAmountsUnparsed = SharedStatics.orderCache.getUnmatchedLayAmounts(this.marketId, this.runnerId);
                availableAmountsOnOppositeSideUnparsed = this.marketRunner.getAvailableToBack(existingFunds.currencyRate);
                myUnmatchedAmounts = myUnmatchedAmountsUnparsed.getOrdersThatAppearInRecords(availableAmountsOnOppositeSideUnparsed);
                availableAmountsOnOppositeSide = availableAmountsOnOppositeSideUnparsed.getOrders();
//                final double layTotalExposure = this.getLayMatchedExposure() + this.getLayUnmatchedExposure() + this.getLayTempExposure();
                final double layTotalExposure = this.getLayTotalExposure();
                final double availableLayExposure = Math.max(0d, this.idealLayExposure - layTotalExposure);
                exposureICanPlace = Math.min(availableLayExposure, exposureIWantToPlace);
                oddsThatCanBeUsed = Formulas.getBestOddsThatCanBeUsed(this.marketId, this.runnerId, side, exposureICanPlace, myUnmatchedAmounts, availableAmountsOnOppositeSide, totalValueMatched);
            } else {
                logger.error("unknown side {} {} {} during placeOrder for: {}", side, price, exposureIWantToPlace, Generic.objectToString(this));
                oddsThatCanBeUsed = 0d;
                exposureICanPlace = 0d;
            }
            if (exposureICanPlace > 0d) {
                final boolean oddsAreAcceptable = Formulas.oddsAreAcceptable(side, price, oddsThatCanBeUsed);
                if (oddsAreAcceptable) {
                    final double sizeICanPlace = Formulas.calculateBetSizeFromExposure(side, oddsThatCanBeUsed, exposureICanPlace);
                    sizePlaced = SharedStatics.orderCache.addPlaceOrder(this.marketId, this.runnerId, side, oddsThatCanBeUsed, sizeICanPlace, side == Side.B ? this.idealBackExposure : this.idealLayExposure, this, sendPostRequestRescriptMethod,
                                                                        speedLimit, existingFunds, this.marketKeepAtInPlay, isBalancingToRemoveExistingExposure, reason);
                } else if (isMandatoryPlace()) {
                    final double sizeICanPlace = Formulas.calculateBetSizeFromExposure(side, price, exposureICanPlace);
                    sizePlaced = SharedStatics.orderCache.addPlaceOrder(this.marketId, this.runnerId, side, price, sizeICanPlace, side == Side.B ? this.idealBackExposure : this.idealLayExposure, this, sendPostRequestRescriptMethod, speedLimit,
                                                                        existingFunds, this.marketKeepAtInPlay, isBalancingToRemoveExistingExposure, reason);
                } else {
                    SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.INFO, "unacceptable {} odds {} vs limit:{} in managedRunner.placeOrder for: {} {} {}", side, oddsThatCanBeUsed, price, this.marketId, this.runnerId, exposureIWantToPlace);
                    sizePlaced = 0d;
                }
            } else {
                sizePlaced = 0d;
            }
        } else {
            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.INFO, "unusable odds {} in placeOrder for: {} {} {} {}", price, this.marketId, this.runnerId, side, exposureIWantToPlace);
//            logger.error("unusable odds {} in placeOrder for: {} {} {} {}", price, this.marketId, this.runnerId, side, exposureIWantToPlace);
            sizePlaced = 0d;
        }

//        if (sizePlaced > 0d) {
//            getExposure();
//        } else { // nothing was placed, no need to recalculate exposure
//        }
        return sizePlaced;
    }

    synchronized double removeExposureIncludingMatched(final double backExcessExposure, final double layExcessExposure, @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod,
                                                       @NotNull final BetFrequencyLimit speedLimit,
                                                       final String reason) {
        double exposureCanceledAndBalanced = 0d;
        final double backUnmatchedExposureToBeCanceled = Math.min(backExcessExposure, this.getBackUnmatchedExposure()), layUnmatchedExposureToBeCanceled = Math.min(layExcessExposure, this.getLayUnmatchedExposure());
        exposureCanceledAndBalanced += cancelUnmatchedAmounts(backUnmatchedExposureToBeCanceled, layUnmatchedExposureToBeCanceled, sendPostRequestRescriptMethod, reason) > 0d ? 1 : 0;
        exposureCanceledAndBalanced += balanceMatchedAmounts(backExcessExposure - backUnmatchedExposureToBeCanceled, layExcessExposure - layUnmatchedExposureToBeCanceled, existingFunds, sendPostRequestRescriptMethod,
                                                             speedLimit, reason);
        return exposureCanceledAndBalanced;
    }

    synchronized double cancelUnmatchedAmounts(final double backExcessExposure, final double layExcessExposure, @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        return SharedStatics.orderCache.cancelUnmatchedAmounts(this.marketId, this.runnerId, backExcessExposure, layExcessExposure, this, sendPostRequestRescriptMethod, reason);
    }

    @SuppressWarnings("OverlyLongMethod")
    synchronized double balanceMatchedAmounts(final double backExcessExposure, final double layExcessExposure, @NotNull final ExistingFunds existingFunds, @NotNull final Method sendPostRequestRescriptMethod,
                                              @NotNull final BetFrequencyLimit speedLimit, final String reason) {
        double balancedExposure = 0d;
        if (backExcessExposure > 0d && layExcessExposure > 0d) {
            logger.error("excessExposure on both back and lay present in balanceMatchedAmounts; this can't be fixed by the program without intentionally breaking the limits, which might be dangerous: {} {} {} {}", backExcessExposure, layExcessExposure,
                         reason, Generic.objectToString(this));
        } else { // no error, nothing to print; the method will continue in both cases
        }
        // will balance the runner, only in the limit of the exposure already existing on the other side
//        final double layExposureToPlace = layExcessExposure > 0d ? Math.min((backExcessExposure - layExcessExposure) / 2d, this.getBackMatchedExposure()) - this.getLayUnmatchedExposure() :
//                                          Math.min(backExcessExposure, this.getBackMatchedExposure()) - this.getLayUnmatchedExposure();
        final double layExposureDeficit = layExcessExposure > 0d ? backExcessExposure - layExcessExposure - this.getLayUnmatchedExposure() - this.getBackPotentialUnmatchedProfit() :
                                          backExcessExposure - this.getLayUnmatchedExposure() - this.getBackPotentialUnmatchedProfit();
//        final double backExposureToPlace = backExcessExposure > 0d ? Math.min((layExcessExposure - backExcessExposure) / 2d, this.getLayMatchedExposure()) - this.getBackUnmatchedExposure() :
//                                           Math.min(layExcessExposure, this.getLayMatchedExposure()) - this.getBackUnmatchedExposure();
        final double backExposureDeficit = backExcessExposure > 0d ? layExcessExposure - backExcessExposure - this.getBackUnmatchedExposure() - this.getLayPotentialUnmatchedProfit() :
                                           layExcessExposure - this.getBackUnmatchedExposure() - this.getLayPotentialUnmatchedProfit();
        if (backExposureDeficit > 0d && layExposureDeficit > 0d) {
            logger.error("STRANGE exposureToPlace on both back and lay present in balanceMatchedAmounts: {} {} {} {}", backExposureDeficit, layExposureDeficit, reason, Generic.objectToString(this));
        } else { // formula: exposureDeficit = price * size
            final double totalValueMatched;

            if (layExposureDeficit >= .1d || backExposureDeficit >= .1d) {
                totalValueMatched = this.marketRunner.getTvEUR(existingFunds.currencyRate);
            } else {
                totalValueMatched = 0d;
            }

            if (backExposureDeficit >= .1d) { // I need to place a back bet, with profit that would cancel the excess
                if (Formulas.oddsAreUsable(this.minBackOdds)) {
                    final Side side = Side.B;
                    @NotNull final OrdersList myUnmatchedAmountsUnparsed = SharedStatics.orderCache.getUnmatchedBackAmounts(this.marketId, this.runnerId);
                    @NotNull final OrdersList availableAmountsOnOppositeSideUnparsed = this.marketRunner.getAvailableToLay(existingFunds.currencyRate);
                    @NotNull final TreeMap<Double, Double> myUnmatchedAmounts = myUnmatchedAmountsUnparsed.getOrdersThatAppearInRecords(availableAmountsOnOppositeSideUnparsed);
                    @NotNull final TreeMap<Double, Double> availableAmountsOnOppositeSide = availableAmountsOnOppositeSideUnparsed.getOrders();
                    Formulas.removeOwnAmountsFromAvailableTreeMap(availableAmountsOnOppositeSide, myUnmatchedAmounts);
                    SharedStatics.orderCache.addTemporaryAmountsToOwnAmounts(this.marketId, this.runnerId, side, myUnmatchedAmounts);
                    final double backTotalExposure = this.getBackTotalExposure();
                    final double availableBackExposure = Math.max(0d, this.idealBackExposure - backTotalExposure);

                    double oddsThatCanBeUsed = this.minBackOdds;
                    double finalPriceToUse, finalSizeToUse, exposureICanPlace;
                    int counter = 0;
                    do {
                        counter++;
                        finalPriceToUse = oddsThatCanBeUsed;
                        finalSizeToUse = backExposureDeficit / oddsThatCanBeUsed;
                        final double exposureIWantToPlace = Formulas.calculateExposure(side, finalPriceToUse, finalSizeToUse);
                        exposureICanPlace = Math.min(availableBackExposure, exposureIWantToPlace);
                        oddsThatCanBeUsed = Formulas.getBestOddsThatCanBeUsed(this.marketId, this.runnerId, side, exposureICanPlace, myUnmatchedAmounts, availableAmountsOnOppositeSide, totalValueMatched, false);
                    } while (!DoubleMath.fuzzyEquals(oddsThatCanBeUsed, finalPriceToUse, .001) && counter < 10);

                    if (exposureICanPlace > 0d) {
                        final boolean oddsAreAcceptable = Formulas.oddsAreAcceptable(side, this.minBackOdds, finalPriceToUse);
                        if (oddsAreAcceptable) { // no error
                        } else {
                            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.INFO, "unacceptable {} odds {} vs limit {}, will place the bet anyway in balanceMatchedAmounts for: {} {} {}", side, finalPriceToUse, this.minBackOdds,
                                                                    this.marketId, this.runnerId, exposureICanPlace);
                        }
                        final double sizePlaced = SharedStatics.orderCache.addPlaceOrder(this.marketId, this.runnerId, side, finalPriceToUse, finalSizeToUse, this.idealBackExposure, this, sendPostRequestRescriptMethod, speedLimit,
                                                                                         existingFunds, this.marketKeepAtInPlay, true, reason);
                        balancedExposure += Formulas.calculateProfit(side, finalPriceToUse, finalSizeToUse);
                    } else { // nothing to place
                    }
                } else {
                    logger.error("unusable minBackOdds in balanceMatchedAmounts for: {} {} {} {}", this.marketId, this.runnerId, this.minBackOdds, backExposureDeficit);
                }
            } else { // no lay excess exposure
            }
            if (layExposureDeficit >= .1d) { // I need to place a lay bet, with profit that would cancel the excess
                if (Formulas.oddsAreUsable(this.minBackOdds)) {
                    final Side side = Side.L;
                    @NotNull final OrdersList myUnmatchedAmountsUnparsed = SharedStatics.orderCache.getUnmatchedLayAmounts(this.marketId, this.runnerId);
                    @NotNull final OrdersList availableAmountsOnOppositeSideUnparsed = this.marketRunner.getAvailableToBack(existingFunds.currencyRate);
                    @NotNull final TreeMap<Double, Double> myUnmatchedAmounts = myUnmatchedAmountsUnparsed.getOrdersThatAppearInRecords(availableAmountsOnOppositeSideUnparsed);
                    @NotNull final TreeMap<Double, Double> availableAmountsOnOppositeSide = availableAmountsOnOppositeSideUnparsed.getOrders();
                    Formulas.removeOwnAmountsFromAvailableTreeMap(availableAmountsOnOppositeSide, myUnmatchedAmounts);
                    SharedStatics.orderCache.addTemporaryAmountsToOwnAmounts(this.marketId, this.runnerId, side, myUnmatchedAmounts);
                    final double layTotalExposure = this.getLayTotalExposure();
                    final double availableLayExposure = Math.max(0d, this.idealLayExposure - layTotalExposure);

                    double oddsThatCanBeUsed = this.maxLayOdds;
                    double finalPriceToUse, finalSizeToUse, exposureICanPlace;
                    int counter = 0;
                    do {
                        counter++;
                        finalPriceToUse = oddsThatCanBeUsed;
                        finalSizeToUse = backExposureDeficit / oddsThatCanBeUsed;
                        final double exposureIWantToPlace = Formulas.calculateExposure(side, finalPriceToUse, finalSizeToUse);
                        exposureICanPlace = Math.min(availableLayExposure, exposureIWantToPlace);
                        oddsThatCanBeUsed = Formulas.getBestOddsThatCanBeUsed(this.marketId, this.runnerId, side, exposureICanPlace, myUnmatchedAmounts, availableAmountsOnOppositeSide, totalValueMatched, false);
                    } while (!DoubleMath.fuzzyEquals(oddsThatCanBeUsed, finalPriceToUse, .001) && counter < 10);

                    if (exposureICanPlace > 0d) {
                        final boolean oddsAreAcceptable = Formulas.oddsAreAcceptable(side, this.maxLayOdds, finalPriceToUse);
                        if (oddsAreAcceptable) { // no error
                        } else {
                            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.INFO, "unacceptable {} odds {} vs limit {}, will place the bet anyway in balanceMatchedAmounts for: {} {} {}", side, finalPriceToUse, this.maxLayOdds,
                                                                    this.marketId, this.runnerId, exposureICanPlace);
                        }
                        final double sizePlaced = SharedStatics.orderCache.addPlaceOrder(this.marketId, this.runnerId, side, finalPriceToUse, finalSizeToUse, this.idealLayExposure, this, sendPostRequestRescriptMethod, speedLimit,
                                                                                         existingFunds, this.marketKeepAtInPlay, true, reason);
                        balancedExposure += Formulas.calculateProfit(side, finalPriceToUse, finalSizeToUse);
                    } else { // nothing to place
                    }
                } else {
                    logger.error("unusable maxLayOdds in balanceMatchedAmounts for: {} {} {} {}", this.marketId, this.runnerId, this.maxLayOdds, layExposureDeficit);
                }
            } else { // no back excess exposure
            }
        }
        return balancedExposure;
    }

//    @SuppressWarnings("WeakerAccess")
//    public synchronized void getExposure() {
//        final long currentTime = System.currentTimeMillis();
//        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner(currentTime);
//        if (localOrderMarketRunner == null) {
//            getTempExposure(); // I can only get tempExposure
//        } else {
//            localOrderMarketRunner.getExposureAndProfit(this);
//        }
//        this.timeStamp(); // it's fine to have the timeStamp before some chunk of the method, as this sequence is all synchronized; I need to timeStamp before the lines with getBack/LayUnmatchedProfit() & getBack/LayTempProfit()
//        this.setBackUnmatchedProfit(this.getBackUnmatchedProfit() + this.getBackTempProfit());
//        this.setLayUnmatchedProfit(this.getLayUnmatchedProfit() + this.getLayTempProfit());
//
//        this.setBackTotalExposure(this.getBackMatchedExposure() + this.getBackUnmatchedExposure() + this.getBackTempExposure());
//        this.setLayTotalExposure(this.getLayMatchedExposure() + this.getLayUnmatchedExposure() + this.getLayTempExposure());
//    }

//    synchronized void getTempExposure() {
//        if (SharedStatics.orderCache == null) { // I'm in the Client, I'm not using the pendingOrdersThread
//        } else {
//            SharedStatics.orderCache.checkTemporaryOrdersExposure(this.marketId, this.runnerId, this);
//        }
//    }

//    @SuppressWarnings("unused")
//    synchronized int cancelUnmatched(@NotNull final Method sendPostRequestRescriptMethod) { // cancel all unmatched orders
////        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner();
//        return SharedStatics.orderCache.cancelUnmatched(getMarketId(), getRunnerId(), sendPostRequestRescriptMethod);
//    }

//    synchronized int cancelUnmatched(final Side sideToCancel, @NotNull final Method sendPostRequestRescriptMethod) { // cancel all unmatched orders on that side
////        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner();
//        return SharedStatics.orderCache.cancelUnmatched(getMarketId(), getRunnerId(), sideToCancel, sendPostRequestRescriptMethod);
//    }
//
//    @SuppressWarnings("unused")
//    synchronized int cancelUnmatched(final Side sideToCancel, final double worstNotCanceledOdds, @NotNull final Method sendPostRequestRescriptMethod) {
////        final OrderMarketRunner localOrderMarketRunner = this.getOrderMarketRunner();
//        return SharedStatics.orderCache.cancelUnmatchedAtWorseOdds(getMarketId(), getRunnerId(), sideToCancel, worstNotCanceledOdds, sendPostRequestRescriptMethod);
//    }

    public synchronized int update(final double newMinBackOdds, final double newMaxLayOdds, final double newBackAmountLimit, final double newLayAmountLimit, @NotNull final RulesManager rulesManager) {
        int modified = 0;
        modified += Generic.booleanToInt(this.setMinBackOdds(newMinBackOdds, rulesManager));
        modified += Generic.booleanToInt(this.setMaxLayOdds(newMaxLayOdds, rulesManager));
        modified += Generic.booleanToInt(this.setBackAmountLimit(newBackAmountLimit, rulesManager));
        modified += Generic.booleanToInt(this.setLayAmountLimit(newLayAmountLimit, rulesManager));

        if (modified > 0) {
            rulesManager.marketsToCheck.put(this.marketId, System.currentTimeMillis());
        } else { // nothing to be done
        }
        return modified;
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
        final ManagedRunner that = (ManagedRunner) obj;
        return Objects.equals(this.marketId, that.marketId) &&
               Objects.equals(this.runnerId, that.runnerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.marketId, this.runnerId);
    }
}
