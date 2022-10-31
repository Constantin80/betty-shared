package info.fmro.shared.stream.definitions;

import com.google.common.math.DoubleMath;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.RunnerOrderModification;
import info.fmro.shared.stream.cache.RunnerOrderModificationsList;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.LogLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LevelPriceSizeLadder
        implements Serializable {
    public static final int MAX_LEVEL = 10;
    public static final double knowledgeError = Double.NaN, noKnowledgeExistsBack = Double.MAX_VALUE, maximalKnowledgeExistsBack = Double.MIN_VALUE, noKnowledgeExistsLay = Double.MIN_VALUE, maximalKnowledgeExistsLay = Double.MAX_VALUE;
    private static final Logger logger = LoggerFactory.getLogger(LevelPriceSizeLadder.class);
    @Serial
    private static final long serialVersionUID = 990070832400710390L;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<Integer, LevelPriceSize> levelToPriceSize = new TreeMap<>(); // level, LevelPriceSize
    @NotNull
    private final Side side; // odds size is in reverse order for back and natural order for lay
    private final double noKnowledgeExists, maximalKnowledgeExists;

    public LevelPriceSizeLadder(@NotNull final Side side) {
        this.side = side;

        if (this.side == Side.B) {
            this.noKnowledgeExists = noKnowledgeExistsBack;
            this.maximalKnowledgeExists = maximalKnowledgeExistsBack;
        } else if (this.side == Side.L) {
            this.noKnowledgeExists = noKnowledgeExistsLay;
            this.maximalKnowledgeExists = maximalKnowledgeExistsLay;
        } else {
            logger.error("unknown side {} during LevelPriceSizeLadder object creation", this.side);
            this.noKnowledgeExists = knowledgeError;
            this.maximalKnowledgeExists = knowledgeError;
        }
    }

    // todo only return modifications that are within worstKnowledge, but also return the worst knowledge itself
    @Nullable
    public synchronized RunnerOrderModificationsList onPriceChangeGetModifications(final boolean isImage, final Iterable<? extends List<Double>> prices) {
        final Map<Integer, LevelPriceSize> initialMap = new HashMap<>(this.levelToPriceSize);
        final double initialWorstOddsWhereIHaveKnowledge = getWorstOddsWhereIHaveKnowledge();
        onPriceChange(isImage, prices);

        final double worstOddsWhereIHaveKnowledge = getWorstOddsWhereIHaveKnowledge(initialWorstOddsWhereIHaveKnowledge);
        final Collection<Double> pricesSet = new HashSet<>(initialMap.keySet());
        pricesSet.addAll(this.levelToPriceSize.keySet());
//        final Side side = getSide();
        @Nullable final List<RunnerOrderModification> returnList;
        if (pricesSet.isEmpty()) {
            returnList = null;
        } else {
            returnList = new ArrayList<>(pricesSet.size());
            for (final Double price : pricesSet) {
                if (price == null) {
                    SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.ERROR, "null price in onPriceChangeGetModifications for: {} {}", Generic.objectToString(initialMap), Generic.objectToString(this.levelToPriceSize));
                } else {
                    final LevelPriceSize initialPriceSize = initialMap.get(price);
                    final LevelPriceSize finalPriceSize = this.levelToPriceSize.get(price);
                    final double initialSize = initialPriceSize == null ? 0d : initialPriceSize.getSizeGBP();
                    final double finalSize = finalPriceSize == null ? 0d : finalPriceSize.getSizeGBP();
                    final double modification = finalSize - initialSize;
                    if (DoubleMath.fuzzyEquals(modification, 0d, 0.0001d)) { // no modification
                    } else {
                        final RunnerOrderModification runnerOrderModification = new RunnerOrderModification(this.side, price, modification);
                        returnList.add(runnerOrderModification);
                    }
                }
            } // end for
        }

        return new RunnerOrderModificationsList(returnList, worstOddsWhereIHaveKnowledge);
    }

    public synchronized void onPriceChange(final boolean isImage, final Iterable<? extends List<Double>> prices) {
        if (isImage) {
            this.levelToPriceSize.clear();
        }
        if (prices != null) {
            for (final List<Double> priceAndSize : prices) {
                final LevelPriceSize levelPriceSize = new LevelPriceSize(priceAndSize);
                if (levelPriceSize.getSizeGBP() == 0d) {
                    this.levelToPriceSize.remove(levelPriceSize.getLevel());
                } else {
                    this.levelToPriceSize.put(levelPriceSize.getLevel(), levelPriceSize);
                }
            }
        } else { // nothing to be done
        }
    }

    private synchronized double getWorstOddsWhereIHaveKnowledge(final double initialValue) { // will return the least knowledge out of initialValue and calculatedValue
        final double calculatedValue = getWorstOddsWhereIHaveKnowledge(), returnValue;
        if (this.side == Side.B) {
            returnValue = Math.max(initialValue, calculatedValue);
        } else if (this.side == Side.L) {
            returnValue = Math.min(initialValue, calculatedValue);
        } else {
            logger.error("unknown side {} during getWorstOddsWhereIHaveKnowledge for: {}", this.side, Generic.objectToString(this));
            returnValue = knowledgeError;
        }
        return returnValue;
    }

    private synchronized double getWorstOddsWhereIHaveKnowledge() {
        final double returnValue;
        if (this.levelToPriceSize.isEmpty()) {
            returnValue = this.noKnowledgeExists;
        } else {
            final LevelPriceSize levelPriceSize = this.levelToPriceSize.get(MAX_LEVEL - 1);
            if (levelPriceSize == null) { // the runner on this side has less than MAX_LEVEL - 1 prices, so I have all of them
                returnValue = this.maximalKnowledgeExists;
            } else {
                returnValue = levelPriceSize.getPrice();
            }
        }
        return returnValue;
    }

// although these min and max methods are probably fine, I'm only interested in the existence and value of position MAX_LEVEL - 1
//    public synchronized double getMinOdds() {
//        final double returnValue, returnError = Double.MAX_VALUE;
//        if (this.side == Side.B) {
//
//        } else if (this.side == Side.L) {
//            final LevelPriceSize levelPriceSize = this.levelToPriceSize.get(0);
//            if (levelPriceSize == null) {
//                logger.error("position zero levelPriceSize null for: {}", Generic.objectToString(this));
//                returnValue = returnError;
//            } else {
//                returnValue = levelPriceSize.getPrice();
//            }
//        } else {
//            logger.error("unknown side {} for: {}", this.side, Generic.objectToString(this));
//            returnValue = returnError;
//        }
//        return returnValue;
//    }
//
//    public synchronized double getMaxOdds() {
//        final double returnValue, returnError = 0d;
//        if (this.side == Side.B) {
//            final LevelPriceSize levelPriceSize = this.levelToPriceSize.get(0);
//            if (levelPriceSize == null) {
//                logger.error("position zero levelPriceSize null for: {}", Generic.objectToString(this));
//                returnValue = returnError;
//            } else {
//                returnValue = levelPriceSize.getPrice();
//            }
//        } else if (this.side == Side.L) {
//        } else {
//            logger.error("unknown side {} for: {}", this.side, Generic.objectToString(this));
//            returnValue = returnError;
//        }
//        return returnValue;
//    }
}
