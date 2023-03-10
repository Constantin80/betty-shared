package info.fmro.shared.stream.cache;

import com.google.common.math.DoubleMath;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.logic.ExistingFunds;
import info.fmro.shared.logic.ManagedMarket;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.logic.RulesManager;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("UtilityClass")
public final class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    @Contract(pure = true)
    private Utils() {
    }

    public static void calculateMarketLimits(final double maxTotalLimit, @NotNull final Iterable<? extends ManagedMarket> marketsSet, final boolean shouldCalculateExposure, final boolean marketLimitsCanBeIncreased,
                                             @NotNull final ExistingFunds safetyLimits, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap, @NotNull final RulesManager rulesManager) {
        @SuppressWarnings("unused") double totalMatchedExposure = 0d, totalExposure = 0d;
        double sumOfMaxMarketLimits = 0d;
        final Collection<ManagedMarket> marketsWithErrorCalculatingExposure = new HashSet<>(1), marketsWithExposureHigherThanTheirMaxLimit = new HashSet<>(1);
        for (final ManagedMarket managedMarket : marketsSet) {
            if (managedMarket == null) {
                logger.error("null managedMarket in calculateMarketLimits for: {}", Generic.objectToString(marketsSet));
            } else if (!SharedStatics.marketCache.markets.containsKey(managedMarket.getMarketId())) { // can be normal both if expired market and if marketCache not initialised; no need to print anything
//               logger.info("possibly expired managedMarket in calculateMarketLimits: {}", managedMarket.getMarketId());
            } else {
                if (shouldCalculateExposure) {
                    managedMarket.attachMarket(rulesManager, marketCataloguesMap);
                    managedMarket.calculateExposure(rulesManager);
                } else { // no need to calculate exposure, it was just calculated previously
                }
                final double maxMarketLimit = Math.min(maxTotalLimit, managedMarket.getMaxMarketLimit(safetyLimits));
                sumOfMaxMarketLimits += maxMarketLimit;
                if (managedMarket.defaultExposureValuesExist()) {
                    totalMatchedExposure += maxMarketLimit;
                    totalExposure += maxMarketLimit;
                    marketsWithErrorCalculatingExposure.add(managedMarket); // this market should have all its unmatched bets cancelled, nothing else as I don't know the exposure
                } else {
                    final double marketMatchedExposure = managedMarket.getMarketMatchedExposure();
                    totalMatchedExposure += marketMatchedExposure;
                    final double marketTotalExposure = managedMarket.getMarketTotalExposure();
                    if (DoubleMath.fuzzyCompare(marketTotalExposure, maxMarketLimit, Formulas.CENT_TOLERANCE) > 0) {
                        marketsWithExposureHigherThanTheirMaxLimit.add(managedMarket); // if total exposure > maxMarketLimit, reduce the exposure
//                        totalExposure += Math.max(maxMarketLimit, marketMatchedExposure);
                        if (!managedMarket.isEnabledMarket()) {
                            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.DEBUG, "disabled managedMarket with total exposure {} higher than maxLimit {}: {} {} {}",
                                                                    marketTotalExposure, maxMarketLimit, managedMarket.getMarketId(), managedMarket.simpleGetMarketName(), managedMarket.simpleGetParentEventId());
                        } else if (SharedStatics.notPlacingOrders || SharedStatics.denyBetting.get()) {
                            SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.DEBUG, "while betting denied, managedMarket with total exposure {} higher than maxLimit {}: {} {} {}",
                                                                    marketTotalExposure, maxMarketLimit, managedMarket.getMarketId(), managedMarket.simpleGetMarketName(), managedMarket.simpleGetParentEventId());
                        } else {
                            logger.error("managedMarket with total exposure {} higher than maxLimit {}: {} {} {}", marketTotalExposure, maxMarketLimit, managedMarket.getMarketId(), managedMarket.simpleGetMarketName(),
                                         managedMarket.simpleGetParentEventId());
                        }
                    } else { // normal case
                    }
                    totalExposure += marketTotalExposure;
                }
            }
        } // end for

        // todo test order placing

        // todo show and use crossmatch amounts as well; maybe use BEST_OFFERS with depth 10 and never place my own bets further than that, unless mandatory placing; done, test

        // todo balance on marketWithTwoRunners or on specific runners, no more balance on entire markets with more than 2 runners; done, test
        // todo test the balancing on 2 runner market and on single runners, extensive testing, with bets on 1 runner or 2 runners

        // todo remove exposure should have a setting to leave the profit on just 1 side; it's not clear how I will use the "Prefer BACK/LAY/NONE" setting in the logic
        // todo program needs to have a simple get out of market at 1 hour before live and a panicked get out at 5 minutes before
        // todo event/market/runner need to have a get out button and panick get out button; when I press it, the market becomes disabled, and then I get out of it
        // todo for panick I need to have different odds set; these can be automatic, depending on the regular odds
        // todo during any kind of balancing, I should behave as mandatoryPlacing is on
        // todo having a nSteps above minBackLimit and an extra amountLimit for that, and same for lay; the extra amountLimit will be spread over the steps with some formula
        // todo it means I'll have an extra limit for each of the steps, and each of those will need to be checked; placing bets to balance matched amounts should only be done at the main odds
        // todo I need to check that the max steps don't break the rule of lay limit being smaller than back limit; if rule is broken, error message and remove 1 step from each side until rule is obeyed

//        final double totalMarketLimit = Math.min(maxTotalLimit, sumOfMaxMarketLimits);
//        @SuppressWarnings("unused") final double availableTotalExposure = totalMarketLimit - totalExposure; // can be positive or negative, not used for now, as I use the ConsideringOnlyMatched variant
//        final double availableExposureInTheMarkets = totalMarketLimit - totalExposure; // should be positive, else this might be an error, or maybe not error
//        final double availableTotalExposureConsideringOnlyMatched = totalMarketLimit - totalMatchedExposure; // can be positive or negative
//        final double availableExposureInTheMarketsConsideringOnlyMatched = totalMarketLimit - totalMatchedExposure; // should always be positive, else this is an error

        for (final ManagedMarket managedMarket : marketsWithErrorCalculatingExposure) {
            if (managedMarket.simpleGetMarket() == null) { // no market attached, won't do anything on this market yet, likely the stream hasn't updated yet
            } else {
                managedMarket.cancelAllUnmatchedBets.set(true);
                managedMarket.setCalculatedLimit(0d, marketLimitsCanBeIncreased, safetyLimits, rulesManager.listOfQueues);
            }
        }
        for (final ManagedMarket managedMarket : marketsWithExposureHigherThanTheirMaxLimit) {
//            final double totalExposure = managedMarket.getMarketTotalExposure();
            final double maxLimit = Math.min(maxTotalLimit, managedMarket.getMaxMarketLimit(safetyLimits));
//            final double reducedExposure = totalExposure - maxLimit;
//            availableExposureInTheMarkets += reducedExposure;
            // I don't think modifying the availableExposureInTheMarkets is needed, as the maxMarketLimit was already used when calculating in the case of these ExposureHigherThanTheirMaxLimit markets

            managedMarket.setCalculatedLimit(maxLimit, marketLimitsCanBeIncreased, safetyLimits, rulesManager.listOfQueues);
        }
        if (sumOfMaxMarketLimits <= maxTotalLimit) { // nothing to do, totalMarketLimit == sumOfMaxMarketLimits
//            logger.info("sumOfMaxMarketLimits <= maxTotalLimit: {} {}", sumOfMaxMarketLimits, maxTotalLimit);
            for (final ManagedMarket managedMarket : marketsSet) {
                if (marketsWithErrorCalculatingExposure.contains(managedMarket)) { // nothing to do, all unmatched bets on this market were previously been canceled
                } else {
                    final double maxMarketLimit = Math.min(maxTotalLimit, managedMarket.getMaxMarketLimit(safetyLimits));
//                    final double calculatedLimit = managedMarket.simpleGetCalculatedLimit();
//                    if (calculatedLimit > maxMarketLimit) {
                    managedMarket.setCalculatedLimit(maxMarketLimit, marketLimitsCanBeIncreased, safetyLimits, rulesManager.listOfQueues);
//                    } else { // calculatedLimit is smaller than maxLimit, nothing to do
//                    }
                }
            } // end for
        } else {
            final double calculatedLimitProportionOutOfMaxLimit = Math.max(Math.min(sumOfMaxMarketLimits == 0d ? 1d : maxTotalLimit / sumOfMaxMarketLimits, 1d), 0d);
            for (final ManagedMarket managedMarket : marketsSet) {
                if (marketsWithErrorCalculatingExposure.contains(managedMarket)) { // nothing to do, all unmatched bets on this market were previously been canceled
                    managedMarket.setCalculatedLimit(0d, marketLimitsCanBeIncreased, safetyLimits, rulesManager.listOfQueues);
                } else { // calculatedLimit = (maxMarketLimit - matchedExposure) / proportionOfAvailableMarketExposureThatWillBeUsed
                    final double maxMarketLimit = Math.min(maxTotalLimit, managedMarket.getMaxMarketLimit(safetyLimits));
//                    final double matchedExposure = managedMarket.getMarketMatchedExposure();
                    final double calculatedLimit = maxMarketLimit * calculatedLimitProportionOutOfMaxLimit;
//                    logger.info("set market calculatedLimit: {} {} {} {}", calculatedLimit, maxMarketLimit, sumOfMaxMarketLimits, maxTotalLimit);
                    managedMarket.setCalculatedLimit(calculatedLimit, marketLimitsCanBeIncreased, safetyLimits, rulesManager.listOfQueues);
                }
            } // end for
        }

//      it's not trivial and it's probably not good to have a complicated way of setting the market limit, based on matched and not matched exposure; let's keep things simple
//            if (availableExposureInTheMarkets >= 0d) {
//
//        } else if (availableTotalExposureConsideringOnlyMatched >= 0d) {
////            if (availableExposureInTheMarkets < 0d) {
////                logger.error("availableExposureInTheMarkets negative in calculateMarketLimits: {} {} for: {} {} {} {}", sumOfMaxMarketLimits, totalExposure, maxTotalLimit, shouldCalculateExposure, marketLimitsCanBeIncreased,
////                             Generic.objectToString(marketsSet));
////            } else { // normal branch, nothing to do here
////            }
//
//            double proportionOfAvailableMarketExposureThatWillBeUsed = Math.max(0d, availableTotalExposureConsideringOnlyMatched == 0d ? 1d : availableExposureInTheMarkets / availableTotalExposureConsideringOnlyMatched);
//            if (proportionOfAvailableMarketExposureThatWillBeUsed > 1d) {
//                logger.error("proportionOfAvailableMarketExposureThatWillBeUsed too big: {} {} {} {} {} {} {}", proportionOfAvailableMarketExposureThatWillBeUsed, availableExposureInTheMarkets, availableTotalExposureConsideringOnlyMatched, maxTotalLimit,
//                             sumOfMaxMarketLimits, totalExposure, totalMatchedExposure);
//                proportionOfAvailableMarketExposureThatWillBeUsed = 1d;
//            } else { // no error, nothing to be done
//            }
//            for (final ManagedMarket managedMarket : marketsSet) {
//                if (marketsWithErrorCalculatingExposure.contains(managedMarket)) { // nothing to do, all unmatched bets on this market were previously been canceled
//                } else { // calculatedLimit = (maxMarketLimit - matchedExposure) / proportionOfAvailableMarketExposureThatWillBeUsed
//                    final double maxMarketLimit = managedMarket.getMaxMarketLimit(safetyLimits);
//                    final double matchedExposure = managedMarket.getMarketMatchedExposure();
//                    final double calculatedLimit = (maxMarketLimit - matchedExposure) * proportionOfAvailableMarketExposureThatWillBeUsed + matchedExposure;
//                    managedMarket.setCalculatedLimit(calculatedLimit, marketLimitsCanBeIncreased, safetyLimits);
//                }
//            } // end for
//        } else {
//            logger.error("negative availableTotalExposureConsideringOnlyMatched in calculateMarketLimits: {} {} for: {} {} {} {}", totalMatchedExposure, availableTotalExposureConsideringOnlyMatched, maxTotalLimit, shouldCalculateExposure,
//                         marketLimitsCanBeIncreased, Generic.objectToString(marketsSet));
//            // minimize exposure in all markets, except those with error in calculating the exposure, where all unmatched will be canceled
//            double proportionMatchedExposureWithinLimit;
//            if (totalMatchedExposure == 0d) {
//                logger.error("bad totalMatchedExposure in calculateMarketLimits: {} {} {} {} {} {}", availableExposureInTheMarkets, availableTotalExposureConsideringOnlyMatched, maxTotalLimit, sumOfMaxMarketLimits, totalExposure, totalMatchedExposure);
//                proportionMatchedExposureWithinLimit = 1d;
//            } else {
//                proportionMatchedExposureWithinLimit = totalMarketLimit / totalMatchedExposure;
//            }
//            if (proportionMatchedExposureWithinLimit < 0d || proportionMatchedExposureWithinLimit > 1d) {
//                logger.error("bad proportionMatchedExposureWithinLimit in calculateMarketLimits: {} {} {} {} {} {} {}", proportionMatchedExposureWithinLimit, availableExposureInTheMarkets, availableTotalExposureConsideringOnlyMatched, maxTotalLimit,
//                             sumOfMaxMarketLimits, totalExposure, totalMatchedExposure);
//                proportionMatchedExposureWithinLimit = Math.max(Math.min(proportionMatchedExposureWithinLimit, 1d), 0d);
//            } else { // no error, nothing to be done
//            }
//            for (final ManagedMarket managedMarket : marketsSet) {
//                if (marketsWithErrorCalculatingExposure.contains(managedMarket)) { // nothing to do, all unmatched bets on this market were previously been canceled
//                } else {
//                    final double matchedExposure = managedMarket.getMarketMatchedExposure();
//                    final double maxLimit = managedMarket.getMaxMarketLimit(safetyLimits);
//                    managedMarket.setCalculatedLimit(Math.min(matchedExposure * proportionMatchedExposureWithinLimit, maxLimit), marketLimitsCanBeIncreased, safetyLimits);
//                }
//            } // end for
//        }
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyNestedMethod"})
    public static List<Double> getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final List<Side> sidesToPlaceExposureOn,
                                                                                             final double excessMatchedExposure) {
        final List<Double> exposureList;
        if (sidesToPlaceExposureOn.size() == 2 && firstRunner.isMandatoryPlace() == secondRunner.isMandatoryPlace()) {
            @NotNull final Side firstSide = sidesToPlaceExposureOn.get(0), secondSide = sidesToPlaceExposureOn.get(1);
            if ((firstSide == Side.B && secondSide == Side.L) || (firstSide == Side.L && secondSide == Side.B)) {
                // nonMatchedExposure should include the tempExposure
                // positive or negative result, depending on existing unmatchedExposure; this differs from the following overloaded methods, when result can only be positive and does not depend on unmatchedExposure
                // I'm getting the raw excessMatchedExposure, without considering existing exposure and limits
                // the factors are the price of toBeUsedOdds, and which of the toBeUsedOdds is more profitable; those two should be enough for now; also lay bets should be given slight priority over back bets, as other gamblers like to back rather than lay
                if (excessMatchedExposure <= 0d) {
                    logger.error("bogus excessMatchedExposure for getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure: {} {} {} {} {}", firstRunner.getMarketId(), firstRunner.getRunnerId(), secondRunner.getRunnerId(),
                                 Generic.objectToString(sidesToPlaceExposureOn), excessMatchedExposure);
                    exposureList = List.of(0d, 0d);
                } else {
                    final double firstToBeUsedOdds = firstRunner.getOddsLimit(firstSide), secondToBeUsedOdds = secondRunner.getOddsLimit(secondSide);
                    if (!Formulas.oddsAreUsable(firstToBeUsedOdds) || !Formulas.oddsAreUsable(secondToBeUsedOdds)) {
                        logger.error("unusable odds for getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure: {} {} {} {} {} {} {}", firstRunner.getMarketId(), firstRunner.getRunnerId(), secondRunner.getRunnerId(),
                                     Generic.objectToString(sidesToPlaceExposureOn), firstRunner.getOddsLimit(firstSide), secondRunner.getOddsLimit(secondSide), excessMatchedExposure);
                        exposureList = List.of(0d, 0d);
                    } else {
                        final double firstSmallerOddsBonus = Math.sqrt(Math.sqrt((secondToBeUsedOdds - 1d) / (firstToBeUsedOdds - 1d))); // double sqrt should be well balanced exposure, a limited advantage for the smaller odds
                        final double firstAmountLayBonus = firstSide == Side.L ? 1.25d : .8d;

                        final double firstAmountProfitability = firstSide == Side.B ? firstToBeUsedOdds - 1d : 1d / (firstToBeUsedOdds - 1d);
                        final double secondAmountProfitability = secondSide == Side.B ? secondToBeUsedOdds - 1d : 1d / (secondToBeUsedOdds - 1d);
                        final double firstAmountProfitabilityBonus;
                        if (firstAmountProfitability > secondAmountProfitability) {
                            firstAmountProfitabilityBonus = 2d;
                        } else //noinspection FloatingPointEquality
                            if (firstAmountProfitability == secondAmountProfitability) {
                                firstAmountProfitabilityBonus = 1d;
                            } else {
                                firstAmountProfitabilityBonus = .5d;
                            }

                        final double firstOverSecondFinalProportion = firstSmallerOddsBonus * firstAmountLayBonus * firstAmountProfitabilityBonus;
                        final double secondExposureWithoutLimits = excessMatchedExposure / (firstOverSecondFinalProportion + 1d);
                        final double firstExposureWithoutLimits = secondExposureWithoutLimits * firstOverSecondFinalProportion;

                        final double firstExposureWithLimits, secondExposureWithLimits;
                        if (firstExposureWithoutLimits == 0d && secondExposureWithoutLimits == 0d) {
                            firstExposureWithLimits = firstExposureWithoutLimits;
                            secondExposureWithLimits = secondExposureWithoutLimits;
                        } else {
                            final double firstExposureLimit = firstRunner.getIdealExposure(firstSide) - firstRunner.getMatchedExposure(firstSide),
                                    secondExposureLimit = secondRunner.getIdealExposure(secondSide) - secondRunner.getMatchedExposure(secondSide);
                            final double totalExposureLimit = firstExposureLimit + secondExposureLimit, totalExposure = firstExposureWithoutLimits + secondExposureWithoutLimits;
                            if (totalExposure > totalExposureLimit) {
                                firstExposureWithLimits = firstExposureLimit;
                                secondExposureWithLimits = secondExposureLimit;
                            } else {
                                if (firstExposureWithoutLimits > firstExposureLimit) {
                                    firstExposureWithLimits = firstExposureLimit;
                                    secondExposureWithLimits = totalExposure - firstExposureLimit;
                                } else if (secondExposureWithoutLimits > secondExposureLimit) {
                                    firstExposureWithLimits = totalExposure - secondExposureLimit;
                                    secondExposureWithLimits = secondExposureLimit;
                                } else {
                                    firstExposureWithLimits = firstExposureWithoutLimits;
                                    secondExposureWithLimits = secondExposureWithoutLimits;
                                }
                            }
                        }

                        final double firstNonMatchedExposure = firstRunner.getUnmatchedExposure(firstSide), secondNonMatchedExposure = secondRunner.getUnmatchedExposure(secondSide);
                        final double firstTempExposure = firstRunner.rawTempExposure(firstSide), secondTempExposure = secondRunner.rawTempExposure(secondSide);
                        if (firstTempExposure > firstNonMatchedExposure || secondTempExposure > secondNonMatchedExposure) {
                            logger.error("non inclusive nonMatchedExposures for: {} {} {} {} {} {} {} {} {} {} {}", firstTempExposure, secondTempExposure, firstNonMatchedExposure, secondNonMatchedExposure, firstRunner.getMarketId(),
                                         firstRunner.getRunnerId(), secondRunner.getRunnerId(), Generic.objectToString(sidesToPlaceExposureOn), firstRunner.getOddsLimit(firstSide), secondRunner.getOddsLimit(secondSide), excessMatchedExposure);
                            exposureList = List.of(-firstNonMatchedExposure, -secondNonMatchedExposure);
                        } else if (firstExposureWithLimits == 0d && secondExposureWithLimits == 0d) { // will cancel all unmatched
                            exposureList = List.of(firstTempExposure - firstNonMatchedExposure, secondTempExposure - secondNonMatchedExposure);
                        } else {
                            final double totalNonMatchedExposure = firstNonMatchedExposure + secondNonMatchedExposure, totalTempExposure = firstTempExposure + secondTempExposure;
                            final double totalExposure = firstExposureWithLimits + secondExposureWithLimits;

                            if (totalExposure >= totalNonMatchedExposure) { // I'll place more orders
                                if (firstExposureWithLimits < firstNonMatchedExposure) {
                                    exposureList = List.of(0d, totalExposure - totalNonMatchedExposure);
                                } else if (secondExposureWithLimits < secondNonMatchedExposure) {
                                    exposureList = List.of(totalExposure - totalNonMatchedExposure, 0d);
                                } else {
                                    exposureList = List.of(firstExposureWithLimits - firstNonMatchedExposure, secondExposureWithLimits - secondNonMatchedExposure);
                                }
                            } else { // I need to cancel some orders
                                if (totalExposure >= totalTempExposure) { // some unmatchedExposure will remain
                                    if (firstExposureWithLimits < firstTempExposure) {
                                        exposureList = List.of(firstTempExposure - firstNonMatchedExposure, totalExposure - secondNonMatchedExposure - firstTempExposure);
                                    } else if (secondExposureWithLimits < secondTempExposure) {
                                        exposureList = List.of(totalExposure - firstNonMatchedExposure - secondTempExposure, secondTempExposure - secondNonMatchedExposure);
                                    } else {
                                        exposureList = List.of(firstExposureWithLimits - firstNonMatchedExposure, secondExposureWithLimits - secondNonMatchedExposure);
                                    }
                                } else { // no unmatchedExposure can be left
                                    exposureList = List.of(firstTempExposure - firstNonMatchedExposure, secondTempExposure - secondNonMatchedExposure);
                                }
                            }
                        }
                    }
                }
            } else {
                logger.error("bogus sides for getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure: {} {} {} {}", Generic.objectToString(sidesToPlaceExposureOn), Generic.objectToString(firstRunner), Generic.objectToString(secondRunner),
                             excessMatchedExposure);
                exposureList = List.of(0d, 0d);
            }
        } else {
            logger.error("bogus sideList or mandatoryPlace for getExposureToBePlacedForTwoWayMarketWithExcessMatchedExposure: {} {} {} {} {} {}", Generic.objectToString(sidesToPlaceExposureOn), Generic.objectToString(firstRunner),
                         Generic.objectToString(secondRunner), excessMatchedExposure, firstRunner.isMandatoryPlace(), secondRunner.isMandatoryPlace());
            exposureList = List.of(0d, 0d);
        }
        return exposureList;
    }

    @SuppressWarnings("OverlyNestedMethod")
    public static List<Double> getExposureToBePlacedForTwoWayMarket(@NotNull final ManagedRunner firstRunner, @NotNull final ManagedRunner secondRunner, @NotNull final List<Side> sidesToPlaceExposureOn, final double availableLimit) {
        final List<Double> exposureList;
        if (sidesToPlaceExposureOn.size() == 2 && firstRunner.isMandatoryPlace() == secondRunner.isMandatoryPlace()) {
            @NotNull final Side firstSide = sidesToPlaceExposureOn.get(0), secondSide = sidesToPlaceExposureOn.get(1);
            if ((firstSide == Side.B && secondSide == Side.L) || (firstSide == Side.L && secondSide == Side.B)) {
                // I'm getting the raw availableLimit, without considering existing exposure and limits
                // the factors are the price of toBeUsedOdds, and which of the toBeUsedOdds is more profitable; those two should be enough for now; also lay bets should be given slight priority over back bets, as other gamblers like to back rather than lay
                if (availableLimit == 0d) {
                    logger.error("availableLimit zero for getAmountsToBePlacedForTwoWayMarket: {} {} {} {}", firstRunner.getMarketId(), firstRunner.getRunnerId(), secondRunner.getRunnerId(), Generic.objectToString(sidesToPlaceExposureOn));
                    exposureList = List.of(0d, 0d);
                } else {
                    final double firstOddsLimit = firstRunner.getOddsLimit(firstSide), secondOddsLimit = secondRunner.getOddsLimit(secondSide);
                    if (!Formulas.oddsAreUsable(firstOddsLimit) || !Formulas.oddsAreUsable(secondOddsLimit)) {
                        if (Formulas.oddsAreDisabled(firstOddsLimit, firstSide) || Formulas.oddsAreDisabled(secondOddsLimit, secondSide)) { // normal branch, odds disabled, will place 0d amount, no need to print anything
                        } else {
                            logger.error("bogus internal arguments for getAmountsToBePlacedForTwoWayMarket: {} {} {} {} {}", firstRunner.getMarketId(), firstRunner.getRunnerId(), secondRunner.getRunnerId(), Generic.objectToString(sidesToPlaceExposureOn),
                                         availableLimit);
                        }
                        exposureList = List.of(0d, 0d);
                    } else {
                        final double firstAmountLayBonus = firstSide == Side.L ? 1.25d : .8d;
                        final double firstToBeUsedOdds, secondToBeUsedOdds;
//                        // here I calculate the toBeUsedOdds
//                        if (availableLimit > 0d) { // I'll place new exposure
//                            if (firstRunner.isMandatoryPlace()) {
//                                firstToBeUsedOdds = firstOddsLimit;
//                                secondToBeUsedOdds = secondOddsLimit;
//                            } else {
//                                double tempFirstToBeUsedOdds = firstOddsLimit, tempSecondToBeUsedOdds = secondOddsLimit;
//
//                            }
//                        } else { // I'll cancel amounts
//
//                        }
                        // I decided to not bother with complex calculation, simple is good enough, and some formulas I use are quite rough anyway
                        firstToBeUsedOdds = firstOddsLimit;
                        secondToBeUsedOdds = secondOddsLimit;

                        final double firstSmallerOddsBonus = Math.sqrt(Math.sqrt((secondToBeUsedOdds - 1d) / (firstToBeUsedOdds - 1d))); // double sqrt should be well balanced exposure, a limited advantage for the smaller odds
                        final double firstAmountProfitability = firstSide == Side.B ? firstToBeUsedOdds - 1d : 1d / (firstToBeUsedOdds - 1d);
                        final double secondAmountProfitability = secondSide == Side.B ? secondToBeUsedOdds - 1d : 1d / (secondToBeUsedOdds - 1d);
                        final double firstAmountProfitabilityBonus;
                        if (firstAmountProfitability > secondAmountProfitability) {
                            firstAmountProfitabilityBonus = 2d;
                        } else //noinspection FloatingPointEquality
                            if (firstAmountProfitability == secondAmountProfitability) {
                                firstAmountProfitabilityBonus = 1d;
                            } else {
                                firstAmountProfitabilityBonus = .5d;
                            }

                        final double firstOverSecondFinalProportion = firstSmallerOddsBonus * firstAmountLayBonus * firstAmountProfitabilityBonus;
                        final double firstUnmatchedExposure = firstRunner.rawUnmatchedExposure(firstSide), secondUnmatchedExposure = secondRunner.rawUnmatchedExposure(secondSide), totalUnmatchedExposure = firstUnmatchedExposure + secondUnmatchedExposure,
                                firstNonMatchedExposure = firstRunner.getUnmatchedExposure(firstSide), secondNonMatchedExposure = secondRunner.getUnmatchedExposure(secondSide);
                        final double exposureToAddOnSecond, exposureToAddOnFirst;
                        if (availableLimit > 0d) {
                            double resultingNonMatchedExposureOnSecond = (availableLimit + firstNonMatchedExposure + secondNonMatchedExposure) / (firstOverSecondFinalProportion + 1d);
                            resultingNonMatchedExposureOnSecond = Math.max(secondNonMatchedExposure, resultingNonMatchedExposureOnSecond);
                            resultingNonMatchedExposureOnSecond = Math.min(secondNonMatchedExposure + availableLimit, resultingNonMatchedExposureOnSecond);
                            exposureToAddOnSecond = resultingNonMatchedExposureOnSecond - secondNonMatchedExposure;
                            exposureToAddOnFirst = availableLimit - exposureToAddOnSecond;
                        } else { // < 0d
                            if (-availableLimit >= totalUnmatchedExposure) {
                                exposureToAddOnSecond = -secondUnmatchedExposure;
                                exposureToAddOnFirst = -firstUnmatchedExposure;
                            } else { // some unmatched exposure will be left
                                double resultingUnmatchedExposureOnSecond = (availableLimit + totalUnmatchedExposure) / (firstOverSecondFinalProportion + 1d);
                                resultingUnmatchedExposureOnSecond = Math.min(secondUnmatchedExposure, resultingUnmatchedExposureOnSecond);
                                resultingUnmatchedExposureOnSecond = Math.max(secondUnmatchedExposure + availableLimit, resultingUnmatchedExposureOnSecond);
                                exposureToAddOnSecond = resultingUnmatchedExposureOnSecond - secondUnmatchedExposure;
                                exposureToAddOnFirst = availableLimit - exposureToAddOnSecond;
                            }
                        }

                        if (exposureToAddOnFirst <= 0d && exposureToAddOnSecond <= 0d) { // negative or zero, so won't break limits
                            exposureList = List.of(exposureToAddOnFirst, exposureToAddOnSecond);
                        } else { // positive
                            final double firstExposureLimitRemaining = firstRunner.getIdealExposure(firstSide) - firstRunner.getTotalExposure(firstSide),
                                    secondExposureLimitRemaining = secondRunner.getIdealExposure(secondSide) - secondRunner.getTotalExposure(secondSide);
                            final double totalExposureLimitRemaining = firstExposureLimitRemaining + secondExposureLimitRemaining, totalExposureToAdd = exposureToAddOnFirst + exposureToAddOnSecond;
                            if (totalExposureToAdd > totalExposureLimitRemaining) {
                                exposureList = List.of(firstExposureLimitRemaining, secondExposureLimitRemaining);
                            } else {
                                if (exposureToAddOnFirst > firstExposureLimitRemaining) {
                                    exposureList = List.of(firstExposureLimitRemaining, totalExposureToAdd - firstExposureLimitRemaining);
                                } else if (exposureToAddOnSecond > secondExposureLimitRemaining) {
                                    exposureList = List.of(totalExposureToAdd - secondExposureLimitRemaining, secondExposureLimitRemaining);
                                } else {
                                    exposureList = List.of(exposureToAddOnFirst, exposureToAddOnSecond);
                                }
                            }
//                            logger.info("exposureList=({};{}) exposureToAddOnFirst={} exposureToAddOnSecond={} firstExposureLimitRemaining={} secondExposureLimitRemaining={}", exposureList.get(0), exposureList.get(1), exposureToAddOnFirst,
//                                        exposureToAddOnSecond, firstExposureLimitRemaining, secondExposureLimitRemaining);
                        }
                    }
                }
            } else {
                logger.error("bogus sides for getAmountsToBePlacedForTwoWayMarket: {} {} {} {} {}", firstRunner.getMarketId(), firstRunner.getRunnerId(), secondRunner.getRunnerId(), Generic.objectToString(sidesToPlaceExposureOn), availableLimit);
                exposureList = List.of(0d, 0d);
            }
        } else {
            logger.error("bogus sideList or mandatoryPlace for getAmountsToBePlacedForTwoWayMarket: {} {} {} {} {} {} {}", firstRunner.getMarketId(), firstRunner.getRunnerId(), secondRunner.getRunnerId(), Generic.objectToString(sidesToPlaceExposureOn),
                         availableLimit, firstRunner.isMandatoryPlace(), secondRunner.isMandatoryPlace());
            exposureList = List.of(0d, 0d);
        }
        return exposureList;
    }
}
