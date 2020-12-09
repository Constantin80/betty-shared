package info.fmro.shared.utility;

import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.EventType;
import info.fmro.shared.entities.MarketCatalogue;
import info.fmro.shared.entities.MarketDescription;
import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.cache.market.Market;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

@SuppressWarnings({"UtilityClass", "ClassWithTooManyMethods", "OverlyComplexClass"})
public final class Formulas {
    private static final Logger logger = LoggerFactory.getLogger(Formulas.class);
    //    public static final List<Double> pricesList = List.of(1.01, 1.02,1.03,1.04,1.05,1.06,1.07,1.08,1.09,1.1,1.11,1.12,1.13,1.14 ...);
    @SuppressWarnings("PublicStaticCollectionField")
    public static final List<Integer> pricesList; // odds prices, multiplied by 100, to have them stored as int
    @SuppressWarnings("PublicStaticCollectionField")
    public static final Map<String, String> charactersMap = Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f));
    public static final double CENT_TOLERANCE = .009999d;
    public static final double ODDS_TOLERANCE = .0001d;

    @Contract(pure = true)
    private Formulas() {
    }

    static {
//        charactersMap.put("Ã¡", "a");
//        charactersMap.put("Ã¼", "u");
        charactersMap.put("µ", "u");
        charactersMap.put("à", "a");
        charactersMap.put("â", "a");
        charactersMap.put("ã", "a");
        charactersMap.put("ä", "a");
        charactersMap.put("å", "a");
        charactersMap.put("æ", "ae");
        charactersMap.put("ç", "c");
        charactersMap.put("è", "e");
        charactersMap.put("é", "e");
        charactersMap.put("ê", "e");
        charactersMap.put("ë", "e");
        charactersMap.put("ì", "i");
        charactersMap.put("í", "i");
        charactersMap.put("î", "i");
        charactersMap.put("ï", "i");
        charactersMap.put("ñ", "n");
        charactersMap.put("ò", "o");
        charactersMap.put("ó", "o");
        charactersMap.put("ô", "o");
        charactersMap.put("õ", "o");
        charactersMap.put("ö", "o");
        charactersMap.put("ø", "o");
        charactersMap.put("ù", "u");
        charactersMap.put("ú", "u");
        charactersMap.put("û", "u");
        charactersMap.put("ü", "u");
        charactersMap.put("ý", "y");
        charactersMap.put("ß", "b");
        charactersMap.put("á", "a");
        charactersMap.put("ā", "a");
        charactersMap.put("ă", "a");
        charactersMap.put("ą", "a");
        charactersMap.put("ć", "c");
        charactersMap.put("č", "c");
        charactersMap.put("đ", "d");
        charactersMap.put("ď", "d");
        charactersMap.put("ē", "e");
        charactersMap.put("ĕ", "e");
        charactersMap.put("ė", "e");
        charactersMap.put("ę", "e");
        charactersMap.put("ě", "e");
        charactersMap.put("ģ", "g");
        charactersMap.put("ī", "i");
        charactersMap.put("į", "i");
        charactersMap.put(Generic.createStringFromCodes(305), "i");
        charactersMap.put("ķ", "k");
        charactersMap.put("ĺ", "l");
        charactersMap.put("ļ", "l");
        charactersMap.put("ľ", "l");
        charactersMap.put("ł", "l");
        charactersMap.put("ń", "n");
        charactersMap.put("ņ", "n");
        charactersMap.put("ň", "n");
        charactersMap.put("ō", "o");
        charactersMap.put("ő", "o");
        charactersMap.put("œ", "oe");
        charactersMap.put("ŕ", "r");
        charactersMap.put("ŗ", "r");
        charactersMap.put("ř", "r");
        charactersMap.put("ş", "s");
        charactersMap.put("ś", "s");
        charactersMap.put("š", "s");
        charactersMap.put("ţ", "t");
        charactersMap.put("ť", "t");
        charactersMap.put("ű", "u");
        charactersMap.put("ų", "u");
        charactersMap.put("ź", "z");
        charactersMap.put("ż", "z");
        charactersMap.put("ž", "z");

        charactersMap.put("-", " ");
        charactersMap.put("/", " ");
        charactersMap.put("'", " ");
        charactersMap.put("`", " ");
        charactersMap.put("’", " ");
        charactersMap.put("&", "and");
        charactersMap.put(".", " ");
        charactersMap.put("|||", "iii");
        charactersMap.put("||", "ii");
        charactersMap.put("[", "(");
        charactersMap.put("]", ")");
        charactersMap.put("{", "(");
        charactersMap.put("}", ")");

        final int mapCapacity = Generic.getCollectionCapacity(charactersMap.size(), 0.75f);
        final Map<String, String> tempMap = new LinkedHashMap<>(mapCapacity, 0.75f);
        synchronized (charactersMap) { // apply toLowerCase().trim() to all keys & values
            for (final Map.Entry<String, String> entry : charactersMap.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue(), modifiedKey = key.toLowerCase(Locale.ENGLISH), modifiedValue = value.toLowerCase(Locale.ENGLISH);
                if (!Objects.equals(key, modifiedKey)) {
                    logger.error("key gets modified in charactersMap static parsing: {} - {}", key, modifiedKey);
                }
                if (!Objects.equals(value, modifiedValue)) {
                    logger.error("value gets modified in charactersMap static parsing: {} - {}", value, modifiedValue);
                }

                tempMap.put(modifiedKey, modifiedValue);
            }

            charactersMap.clear();
            charactersMap.putAll(tempMap);
        } // end synchronized
    }

    static { // initialize priceList
        final Collection<Integer> localPricesList = new ArrayList<>(350);
        int counter = 101;
        do {
            localPricesList.add(counter);
            final int step;
            if (counter < 200) {
                step = 1;
            } else if (counter < 300) {
                step = 2;
            } else if (counter < 400) {
                step = 5;
            } else if (counter < 600) {
                step = 10;
            } else if (counter < 1_000) {
                step = 20;
            } else if (counter < 2_000) {
                step = 50;
            } else if (counter < 3_000) {
                step = 100;
            } else if (counter < 5_000) {
                step = 200;
            } else if (counter < 10_000) {
                step = 500;
            } else {
                step = 1_000;
            }

            counter += step;
        } while (counter <= 100_000);

        pricesList = List.copyOf(localPricesList);
    }

    public static double getNextOddsDown(final String stringOdds, @NotNull final Side side) {
        double odds;
        try {
            odds = Double.parseDouble(stringOdds);
        } catch (final NumberFormatException e) {
            odds = 1d;
            logger.error("NumberFormatException in getNextOddsDown for: {} {}", stringOdds, side, e);
        }
        return getNextOddsDown(odds, side);
    }

    public static double getNextOddsDown(final double odds, @NotNull final Side side) {
        final double result;
        final double closestOdds = getClosestOdds(odds, side);
        final boolean oddsAndClosestOddsAreEqual = oddsAreEqual(odds, closestOdds);
        if (oddsAndClosestOddsAreEqual || odds < closestOdds) {
            result = getNStepDifferentOdds(closestOdds, -1);
        } else { // odds > closestOdds
            result = closestOdds;
        }
        return result;
    }

    public static double getNextOddsUp(final String stringOdds, @NotNull final Side side) {
        double odds;
        try {
            odds = Double.parseDouble(stringOdds);
        } catch (final NumberFormatException e) {
            odds = 1_001d;
            logger.error("NumberFormatException in getNextOddsUp for: {} {}", stringOdds, side, e);
        }
        return getNextOddsUp(odds, side);
    }

    public static double getNextOddsUp(final double odds, @NotNull final Side side) {
        final double result;
        final double closestOdds = getClosestOdds(odds, side);
        final boolean oddsAndClosestOddsAreEqual = oddsAreEqual(odds, closestOdds);
        if (oddsAndClosestOddsAreEqual || odds > closestOdds) {
            result = getNStepDifferentOdds(closestOdds, 1);
        } else { // odds < closestOdds
            result = closestOdds;
        }
        return result;
    }

    public static double getClosestOdds(final String odds, @NotNull final Side side) {
        double doubleValue;
        //noinspection ProhibitedExceptionCaught
        try {
            doubleValue = Double.parseDouble(odds);
        } catch (NumberFormatException | NullPointerException e) {
            logger.warn("exception converting string in getClosestOdds: {} {}", odds, side);
            if (side == Side.B) {
                doubleValue = 1_001d;
            } else if (side == Side.L) {
                doubleValue = 1d;
            } else {
                logger.error("unknown side in getClosestOdds string: {} {}", side, odds);
                doubleValue = 0d;
            }
        }

        return getClosestOdds(doubleValue, side);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static double getClosestOdds(final double odds, @NotNull final Side side) {
        final double odds100 = odds * 100d;
        final double floor = Math.floor(odds100);
        final int intOdds;
        if (odds100 - floor < .01d) {
            intOdds = (int) floor;
        } else {
            final double ceiling = Math.ceil(odds100);
            intOdds = ceiling - odds100 < .01d || side == Side.B ? (int) ceiling : (int) floor;
        }
        return getClosestOdds(intOdds, side);
    }

    public static double getClosestOdds(final int odds, @NotNull final Side side) {
        final double result;
        final int oddsPosition = pricesList.indexOf(odds);
        if (oddsPosition < 0) {
            final int listSize = pricesList.size();
            if (odds < pricesList.get(0)) {
                result = 1d;
            } else if (odds > pricesList.get(listSize - 1)) {
                result = 1_001d;
            } else {
                int smallerOdds = 100;
                int largerOdds = 100_100;
                for (final int oddsInList : pricesList) {
                    if (odds > oddsInList) {
                        smallerOdds = oddsInList;
                    } else {
                        largerOdds = oddsInList;
                        break;
                    }
                }
                if (side == Side.B) {
                    result = largerOdds / 100d;
                } else if (side == Side.L) {
                    result = smallerOdds / 100d;
                } else {
                    logger.error("unknown side in getClosestOdds: {} {}", side, odds);
                    result = Generic.getClosestNumber(odds, smallerOdds, largerOdds) / 100d;
                }
            }
        } else {
            result = odds / 100d;
        }
        return Generic.roundDoubleAmount(result);
    }

    @SuppressWarnings({"OverloadedMethodsWithSameNumberOfParameters", "WeakerAccess", "RedundantSuppression"})
    public static double getNStepDifferentOdds(final double baseOdds, final int nSteps) {
        //noinspection NumericCastThatLosesPrecision
        final int intBaseOdds = (int) Math.round(baseOdds * 100d);

        return getNStepDifferentOdds(intBaseOdds, nSteps);
    }

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    private static double getNStepDifferentOdds(final int baseOdds, final int nSteps) {
        final double result;
        final int baseOddsPosition = pricesList.indexOf(baseOdds);
        final int listSize = pricesList.size();
        if (baseOddsPosition < 0) {
            if (baseOdds < pricesList.get(0)) {
                result = 1d;
            } else if (baseOdds > pricesList.get(listSize - 1)) {
                result = 1_001d;
            } else {
                logger.error("baseOdds {} not found in pricesList during getNStepDifferentOdds {}: {}", baseOdds, nSteps, baseOddsPosition);
                result = nSteps <= 0 ? 1d : 1_001d;
            }
        } else {
            final int resultPosition = baseOddsPosition + nSteps;
            if (resultPosition < 0) {
                result = 1d;
            } else if (resultPosition >= listSize) {
                result = 1_001d;
            } else {
                result = (double) (pricesList.get(resultPosition)) / 100d;
            }
//            final int resultPosition = Math.max(Math.min(baseOddsPosition + nSteps, listSize - 1), 0);
        }
        return Generic.roundDoubleAmount(result);
    }

    @Contract(pure = true)
    public static boolean oddsAreUsable(final double odds) {
//        return odds <= 1_000d && odds >= 1.01d;
        return (DoubleMath.fuzzyCompare(odds, 1_000d, ODDS_TOLERANCE) <= 0 && DoubleMath.fuzzyCompare(odds, 1.01d, ODDS_TOLERANCE) >= 0);
    }

    public static boolean oddsAreDisabled(final double odds, @NotNull final Side side) {
        return (side == Side.L && oddsAreEqual(odds, 1d)) || (side == Side.B && oddsAreEqual(odds, 1_001d));
    }

    public static double inverseOdds(final double odds, @NotNull final Side side) {
        final double returnValue;
        if (oddsAreUsable(odds)) {
            returnValue = getClosestOdds(Math.max(1.01d, 1d / (odds - 1d) + 1d), side == Side.B ? Side.L : Side.B);
        } else {
            if (side == Side.L && oddsAreEqual(odds, 1d)) {
                returnValue = 1_001d;
            } else if (side == Side.B && oddsAreEqual(odds, 1_001d)) {
                returnValue = 1d;
            } else {
                logger.error("unusable odds in Formulas.inverseOdds for: {} {}", odds, side);
                returnValue = side == Side.B ? 1d : 1_001d;
            }
        }
        return returnValue;
    }

//    private static double inverseOdds(final double odds) {
//        final double returnValue;
//        if (oddsAreUsable(odds)) {
//            returnValue = Math.max(1.01d, 1d / (odds - 1d) + 1d);
//        } else {
//            logger.error("unusable odds in Formulas.inverseOdds for: {}", odds);
//            returnValue = 0d;
//        }
//        return returnValue;
//    }

    public static int getOddsPosition(final double odds) {
        @SuppressWarnings("NumericCastThatLosesPrecision") final int intOdds = (int) Math.round(odds * 100d);
        return pricesList.indexOf(intOdds);
    }

    public static boolean oddsAreInverse(final double firstOdds, final double secondOdds) {
        return orderedOddsAreInverse(firstOdds, secondOdds) || orderedOddsAreInverse(secondOdds, firstOdds);
    }

    public static boolean orderedOddsAreInverse(final double firstBackOdds, final double secondLayOdds) { // assume first odds are back and second are lay
        final boolean areInverse;
        if (oddsAreUsable(firstBackOdds) && oddsAreUsable(secondLayOdds)) {
            final double inverseFirstOdds = inverseOdds(firstBackOdds, Side.B), inverseSecondOdds = inverseOdds(secondLayOdds, Side.L);
            areInverse = oddsAreEqual(firstBackOdds, inverseSecondOdds) || oddsAreEqual(inverseFirstOdds, secondLayOdds);
        } else { // unusable odds in Formulas.oddsAreInverse for: 1001.0 1.0
            if ((oddsAreEqual(firstBackOdds, 1d) && oddsAreEqual(secondLayOdds, 1_001d)) || (oddsAreEqual(secondLayOdds, 1d) && oddsAreEqual(firstBackOdds, 1_001d))) {
                areInverse = true;
            } else {
                logger.error("unusable odds in Formulas.oddsAreInverse for: {} {}", firstBackOdds, secondLayOdds);
                areInverse = false;
            }
        }
        return areInverse;
    }

    public static boolean oddsAreEqual(final double firstOdds, final double secondOdds) {
        return DoubleMath.fuzzyEquals(firstOdds, secondOdds, ODDS_TOLERANCE);
    }

    @SuppressWarnings("WeakerAccess")
    public static int oddsCompare(final double firstOdds, final double secondOdds) {
        return DoubleMath.fuzzyCompare(firstOdds, secondOdds, ODDS_TOLERANCE);
//        final int result;
//        if (DoubleMath.fuzzyEquals(firstOdds, secondOdds, ODDS_TOLERANCE)) {
//            result = 0;
//        } else if (firstOdds < secondOdds) {
//            result = -1;
//        } else {
//            result = 1;
//        }
//        return result;
    }

    @SuppressWarnings("WeakerAccess")
    public static int oddsCompare(final double firstOdds, final double secondOdds, final Side side) {
        final int result;
        if (side == Side.B) {
            result = oddsCompare(firstOdds, secondOdds);
        } else if (side == Side.L) {
            result = oddsCompare(secondOdds, firstOdds);
        } else {
            logger.error("unknown side {} in oddsCompare for: {} {}", side, firstOdds, secondOdds);
            result = oddsCompare(firstOdds, secondOdds); // I still return a result, no choice
        }
        return result;
    }

    public static double maxOdds(final double firstOdds, final double secondOdds, final Side side) {
        final double result;
        if (oddsCompare(firstOdds, secondOdds, side) < 0) {
            result = secondOdds;
        } else {
            result = firstOdds;
        }
        return result;
    }

    public static double minOdds(final double firstOdds, final double secondOdds, final Side side) {
        final double result;
        if (oddsCompare(firstOdds, secondOdds, side) > 0) {
            result = secondOdds;
        } else {
            result = firstOdds;
        }
        return result;
    }

    public static double getBetSizeFromExposure(final Side side, final double price, final double exposure) {
        final double betSize;
        if (side == null) {
            logger.error("null side in Formulas.getBetSizeFromExposure for: {} {}", price, exposure);
            betSize = 0d;
        } else if (exposure <= 0d || !oddsAreUsable(price)) { // normal to happen, won't print any message
            betSize = 0d;
        } else if (side == Side.B) {
            betSize = exposure;
        } else if (side == Side.L) {
            betSize = exposure / (price - 1d);
        } else {
            logger.error("bogus side {} in Formulas.getBetSizeFromExposure for: {} {}", side, price, exposure);
            betSize = 0d;
        }
        return betSize;
    }

    public static double exposure(final Side side, final double price, final double size) {
        final double exposure;
        if (side == null || size < 0d || !oddsAreUsable(price)) {
            logger.error("bad arguments in Formulas.exposure for: {} {} {}", side, price, size);
            exposure = 0d;
        } else if (side == Side.B) {
            exposure = size;
        } else if (side == Side.L) {
            exposure = size * (price - 1d);
        } else {
            logger.error("bogus side {} in Formulas.exposure for: {} {}", side, price, size);
            exposure = 0d;
        }
        return exposure;
    }

    public static double layExposure(final double price, final double size) {
        return exposure(Side.L, price, size);
    }

    public static boolean oddsAreAcceptable(final Side side, final double worstAcceptedOdds, final double price) {
        final boolean areAcceptable;

        if (side == null) {
            logger.error("null side in oddsAreAcceptable for: {} {} {}", side, worstAcceptedOdds, price);
            areAcceptable = false;
        } else if (!oddsAreUsable(worstAcceptedOdds)) {
            logger.error("unusable worstAcceptedOdds in oddsAreAcceptable for: {} {} {}", side, worstAcceptedOdds, price);
            areAcceptable = false;
        } else if (!oddsAreUsable(price)) { // acceptable error, won't print anything
//            logger.error("unusable price in oddsAreAcceptable for: {} {} {}", side, worstAcceptedOdds, price);
            areAcceptable = false;
        } else if (side == Side.B) {
            areAcceptable = DoubleMath.fuzzyCompare(price, worstAcceptedOdds, ODDS_TOLERANCE) >= 0;
        } else if (side == Side.L) {
            areAcceptable = DoubleMath.fuzzyCompare(price, worstAcceptedOdds, ODDS_TOLERANCE) <= 0;
        } else {
            logger.error("strange unsupported side in oddsAreAcceptable for: {} {} {}", side, worstAcceptedOdds, price);
            areAcceptable = false;
        }

        return areAcceptable;
    }

    public static boolean oddsAreWorse(final Side side, final double worstAcceptedOdds, final double price) {
        return oddsAreWorse(side, worstAcceptedOdds, price, false);
    }

    private static boolean oddsAreWorse(final Side side, final double worstAcceptedOdds, final double price, final boolean unusablePriceIsAcceptable) {
        final boolean areWorse;

        if (side == null) {
            logger.error("null side in oddsAreWorse for: {} {} {}", side, worstAcceptedOdds, price);
            areWorse = true;
        } else if (!oddsAreUsable(worstAcceptedOdds)) {
            logger.error("unusable worstAcceptedOdds in oddsAreWorse for: {} {} {}", side, worstAcceptedOdds, price);
            areWorse = false;
        } else if (!oddsAreUsable(price)) {
            if (unusablePriceIsAcceptable) { // acceptable, nothing to be done
            } else {
                logger.error("unusable price in oddsAreWorse for: {} {} {}", side, worstAcceptedOdds, price);
            }
            areWorse = true;
        } else if (side == Side.B) {
            areWorse = DoubleMath.fuzzyCompare(price, worstAcceptedOdds, ODDS_TOLERANCE) < 0;
        } else if (side == Side.L) {
            areWorse = DoubleMath.fuzzyCompare(price, worstAcceptedOdds, ODDS_TOLERANCE) > 0;
        } else {
            logger.error("strange unsupported side in oddsAreWorse for: {} {} {}", side, worstAcceptedOdds, price);
            areWorse = true;
        }

        return areWorse;
    }

    @Contract(value = "true, _, !null -> param3; false, _, null -> param2; false, _, !null -> param3", pure = true)
    public static double selectPrice(final boolean isImage, final double currentPrice, final Double newPrice) {
        final double returnValue;
        if (isImage) {
            returnValue = newPrice == null ? 0d : newPrice;
        } else {
            returnValue = newPrice == null ? currentPrice : newPrice;
        }
        return returnValue;
    }

    private static void removeOwnAmountsFromAvailableTreeMap(@NotNull final Map<Double, Double> availableAmounts, @NotNull final TreeMap<Double, Double> amountsFromMyUnmatchedOrders) {
        //noinspection ForLoopWithMissingComponent
        for (final Iterator<Map.Entry<Double, Double>> iterator = availableAmounts.entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<Double, Double> entry = iterator.next();
            final Double price = entry.getKey();
            if (price == null) {
                logger.error("null price in removeOwnAmountsFromAvailableTreeMap for: {} {}", Generic.objectToString(availableAmounts), Generic.objectToString(amountsFromMyUnmatchedOrders));
            } else {
                final Double availableAmount = entry.getValue();
                final double availableAmountPrimitive = availableAmount == null ? 0d : availableAmount;
                final Double myAmount = amountsFromMyUnmatchedOrders.get(price);
                final double myAmountPrimitive = myAmount == null ? 0d : myAmount;
                final double amountFromOthers = availableAmountPrimitive - myAmountPrimitive;
                if (amountFromOthers < 0.01d) {
//                    availableAmounts.remove(price);
                    iterator.remove();
                    if (amountFromOthers <= -0.01d) {
                        logger.error("negative amount from others {} in removeOwnAmountsFromAvailableTreeMap for: {} {} {} {} {}", amountFromOthers, price, myAmount, availableAmount, Generic.objectToString(amountsFromMyUnmatchedOrders),
                                     Generic.objectToString(availableAmounts));
                    } else { // no error, nothing to print
                    }
                } else {
//                    availableAmounts.replace(price, amountFromOthers);
                    entry.setValue(amountFromOthers);
                }
            }
        }
    }

    public static Market getMarket(final String marketId) {
        return SharedStatics.marketCache.markets.get(marketId);
    }

//    public static OrderMarket getOrderMarket(final String marketId) {
//        return SharedStatics.orderCache.markets.get(marketId);
//    }

    public static String getEventIdOfMarketCatalogue(final MarketCatalogue marketCatalogue) {
        @Nullable final String result;

        if (marketCatalogue != null) {
            final Event eventStump = marketCatalogue.getEventStump();
            if (eventStump == null) {
                logger.error("null event in marketCatalogue during getEventOfMarket: {}", Generic.objectToString(marketCatalogue));
                result = null;
            } else {
                result = eventStump.getId();
            }
        } else {
            logger.error("null marketCatalogue in Formulas.getEventIdOfMarketCatalogue");
            result = null;
        }

        return result;
    }

    public static String getEventIdOfMarketId(final String marketId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        @Nullable final String result;

        final MarketCatalogue marketCatalogue = marketCataloguesMap.get(marketId);
        if (marketCatalogue != null) {
            result = getEventIdOfMarketCatalogue(marketCatalogue);
        } else {
            if (marketCataloguesMap.isEmpty()) { // normal, before map got a chance to be updated
            } else {
                SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.DEBUG, "couldn't find marketId {} in Statics.marketCataloguesMap during getEventIdOfMarketId, maybe market is expired", marketId);
            }
            result = null;
        }
        return result;
    }

    public static Event getStoredEventOfMarketId(final String marketId, @NotNull final SynchronizedMap<? super String, ? extends Event> eventsMap, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        final String eventId = getEventIdOfMarketId(marketId, marketCataloguesMap);
        return eventsMap.get(eventId);
    }

    public static Event getStoredEventOfMarketCatalogue(final MarketCatalogue marketCatalogue, @NotNull final SynchronizedMap<? super String, ? extends Event> eventsMap) {
        final String eventId = getEventIdOfMarketCatalogue(marketCatalogue);
        return eventsMap.get(eventId);
    }

    public static boolean isMarketType(final MarketCatalogue marketCatalogue, final Collection<String> typesList) {
        final boolean result;

        if (marketCatalogue != null && typesList != null) {
            final EventType eventType = marketCatalogue.getEventType();
            if (eventType != null) {
                final String eventTypeId = eventType.getId();
                if (eventTypeId != null) {
                    result = typesList.contains(eventTypeId);
                } else {
                    logger.error("null eventType in isMarketType listArg for: {} {}", Generic.objectToString(typesList), Generic.objectToString(marketCatalogue));
                    result = false;
                }
            } else {
                logger.error("null eventTypeId in isMarketType listArg for: {} {}", Generic.objectToString(typesList), Generic.objectToString(marketCatalogue));
                result = false;
            }
        } else {
            logger.error("null arguments in isMarketType listArg for: {} {}", Generic.objectToString(typesList), Generic.objectToString(marketCatalogue));
            result = false;
        }

        return result;
    }

    public static boolean isMarketType(final MarketCatalogue marketCatalogue, final String... types) {
        return isMarketType(marketCatalogue, types == null ? null : Arrays.asList(types));
    }

    @Nullable
    public static String getEventName(final String eventId, @NotNull final StreamSynchronizedMap<? super String, ? extends Event> eventsMap) {
        final Event event = eventsMap.get(eventId);
        return event == null ? null : event.getName();
    }

    @Nullable
    public static String getMarketCatalogueName(final String marketId, @NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        final MarketCatalogue marketCatalogue = marketCataloguesMap.get(marketId);
        return marketCatalogue == null ? null : getMarketCatalogueName(marketCatalogue);
    }

    @Nullable
    public static String getMarketCatalogueName(@NotNull final MarketCatalogue marketCatalogue) {
        @Nullable final String result;
        final String marketName = marketCatalogue.getMarketName();
        if (marketName == null) {
            final MarketDescription marketDescription = marketCatalogue.getDescription();
            result = marketDescription == null ? null : marketDescription.getMarketType();
        } else {
            result = marketName;
        }
        return result;
    }

    public static boolean programHasRecentlyStarted(final long currentTime) {
        return currentTime - SharedStatics.PROGRAM_START_TIME <= Generic.MINUTE_LENGTH_MILLISECONDS;
    }

    public static double getBestOddsThatCanBeUsed(final String marketId, final RunnerId runnerId, @NotNull final Side side, final double exposureToBePlaced, @NotNull final TreeMap<Double, Double> myUnmatchedAmounts,
                                                  @NotNull final TreeMap<Double, Double> availableAmountsOnOppositeSide, final double totalValueMatched) {
        return getBestOddsThatCanBeUsed(marketId, runnerId, side, exposureToBePlaced, myUnmatchedAmounts, availableAmountsOnOppositeSide, totalValueMatched, true);
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public static double getBestOddsThatCanBeUsed(final String marketId, final RunnerId runnerId, @NotNull final Side side, final double exposureToBePlaced, @NotNull final TreeMap<Double, Double> myUnmatchedAmounts,
                                                  @NotNull final TreeMap<Double, Double> availableAmountsOnOppositeSide, final double totalValueMatched, final boolean collectionsNeedParsing) {
        if (collectionsNeedParsing) {
            removeOwnAmountsFromAvailableTreeMap(availableAmountsOnOppositeSide, myUnmatchedAmounts);
            SharedStatics.orderCache.addTemporaryAmountsToOwnAmounts(marketId, runnerId, side, myUnmatchedAmounts);
        } else { // collections are already parsed
        }
        final NavigableSet<Double> myUnmatchedPrices = myUnmatchedAmounts.descendingKeySet(); // results in descending order for back and ascending order for lay
        final NavigableSet<Double> availableNotOwnedPrices = availableAmountsOnOppositeSide.descendingKeySet(); // results in descending order when side argument is back and ascending order when lay
        final List<Integer> fullPricesList = side == Side.B ? Lists.reverse(pricesList) : pricesList;
        double bestOddsThatCanBeUsed = 0d;
        double myAmountsSum = exposureToBePlaced;
        double otherAmountsSum = 0d;
        for (@NotNull final Map.Entry<Double, Double> entry : availableAmountsOnOppositeSide.entrySet()) {
//            final Double price = entry.getKey();
//            final double pricePrimitive = price == null ? 1d : price;
            final Double amount = entry.getValue();
            final double amountPrimitive = amount == null ? 0d : amount;
//            otherAmountsSum += amountPrimitive * (pricePrimitive - 1d);
            otherAmountsSum += amountPrimitive;
        }
        final double myAmountsSumLimit = Math.max(10d, Math.min(totalValueMatched, otherAmountsSum / 4d));

        final Iterator<Double> myPricesIterator = myUnmatchedPrices.iterator();
        final Iterator<Double> otherPricesIterator = availableNotOwnedPrices.iterator();
        double currentMyPrice = side == Side.B ? 1_001d : 1d, currentOtherPrice = side == Side.B ? 1_001d : 1d;
        double previousDoublePrice = side == Side.B ? 1_001d : 1d;
        for (final int intPrice : fullPricesList) { // I'll check each price, starting with the best possible, and I'll pick the first that can be used
            final double doublePrice = intPrice / 100d;
            while (myPricesIterator.hasNext() && ((side == Side.B && DoubleMath.fuzzyCompare(currentMyPrice, doublePrice, ODDS_TOLERANCE) >= 0) || (side == Side.L && DoubleMath.fuzzyCompare(currentMyPrice, doublePrice, ODDS_TOLERANCE) <= 0))) {
                if (oddsAreEqual(currentMyPrice, doublePrice)) {
                    final Double currentMyAmount = myUnmatchedAmounts.get(currentMyPrice);
                    final double currentMyAmountPrimitive = currentMyAmount == null ? 0d : currentMyAmount;
//                    myAmountsSum += (currentMyPrice - 1d) * currentMyAmountPrimitive;
                    myAmountsSum += currentMyAmountPrimitive;
                } else { // currentMyPrice > doublePrice, which means currentMyPrice might have been used in a previous loop iteration, and is not relevant for the current iteration
                }
                currentMyPrice = myPricesIterator.next();
            }
            if (oddsAreEqual(currentMyPrice, doublePrice)) { // the case where iterator finished the elements, and this is the last element
                final Double currentMyAmount = myUnmatchedAmounts.get(currentMyPrice);
                final double currentMyAmountPrimitive = currentMyAmount == null ? 0d : currentMyAmount;
//                myAmountsSum += (currentMyPrice - 1d) * currentMyAmountPrimitive;
                myAmountsSum += currentMyAmountPrimitive;
            } else { // currentMyPrice != doublePrice, which means currentMyPrice is not relevant for the current iteration
            }

            while (otherPricesIterator.hasNext() &&
                   ((side == Side.B && DoubleMath.fuzzyCompare(currentOtherPrice, previousDoublePrice, ODDS_TOLERANCE) >= 0) || (side == Side.L && DoubleMath.fuzzyCompare(currentOtherPrice, previousDoublePrice, ODDS_TOLERANCE) <= 0))) {
                if (oddsAreEqual(currentOtherPrice, previousDoublePrice)) {
                    final Double currentOtherAmount = availableAmountsOnOppositeSide.get(currentOtherPrice);
                    final double currentOtherAmountPrimitive = currentOtherAmount == null ? 0d : currentOtherAmount;
//                    otherAmountsSum -= (currentOtherPrice - 1d) * currentOtherAmountPrimitive;
                    otherAmountsSum -= currentOtherAmountPrimitive;
                } else { // currentMyPrice > doublePrice, which means currentMyPrice might have been used in a previous loop iteration, and is not relevant for the current iteration
                }
                currentOtherPrice = otherPricesIterator.next();
            }
            if (oddsAreEqual(currentOtherPrice, previousDoublePrice)) { // the case where iterator finished the elements, and this is the last element
                final Double currentOtherAmount = availableAmountsOnOppositeSide.get(currentOtherPrice);
                final double currentOtherAmountPrimitive = currentOtherAmount == null ? 0d : currentOtherAmount;
//                otherAmountsSum -= (currentOtherPrice - 1d) * currentOtherAmountPrimitive;
                otherAmountsSum -= currentOtherAmountPrimitive;
            } else { // currentMyPrice != doublePrice, which means currentMyPrice is not relevant for the current iteration
            }

            if (otherAmountsSum <= ManagedRunner.PLACE_BET_THRESHOLD * Math.min(myAmountsSum, myAmountsSumLimit)) {
                // myAmountsSum always increases, otherAmountsSum always decreases, so when this branch is entered once, it would be entered on all next loop iterations
                bestOddsThatCanBeUsed = Generic.roundDoubleAmount(doublePrice);
                SharedStatics.alreadyPrintedMap.logOnce(logger, LogLevel.INFO, "bestOddsThatCanBeUsed {} for: {} {} {} {}", bestOddsThatCanBeUsed, marketId, runnerId, side, exposureToBePlaced);
                break;
            } else { // I'll keep iterating, nothing to be done
            }

            previousDoublePrice = doublePrice;
        }
        return bestOddsThatCanBeUsed;
    }

    public static double getWorstOddsThatCantBeReached(final String marketId, final RunnerId runnerId, @NotNull final Side side, @NotNull final TreeMap<Double, Double> myUnmatchedAmounts,
                                                       @NotNull final NavigableMap<Double, Double> availableAmountsOnOppositeSide) { // this form is used when checking if existing orders needs to be canceled
        return getWorstOddsThatCantBeReached(marketId, runnerId, side, myUnmatchedAmounts, availableAmountsOnOppositeSide, false, true);
    }

    public static double getWorstOddsThatCantBeReached(final String marketId, final RunnerId runnerId, @NotNull final Side side, @NotNull final TreeMap<Double, Double> myUnmatchedAmounts,
                                                       @NotNull final NavigableMap<Double, Double> availableAmountsOnOppositeSide, final boolean placingNewOrder, final boolean collectionsNeedParsing) {
        return getWorstOddsThatCantBeReached(marketId, runnerId, side, myUnmatchedAmounts, availableAmountsOnOppositeSide, placingNewOrder, collectionsNeedParsing, side == Side.L ? 1_001d : 1d);
    }

    public static double getWorstOddsThatCantBeReached(final String marketId, final RunnerId runnerId, @NotNull final Side side, @NotNull final TreeMap<Double, Double> myUnmatchedAmounts,
                                                       @NotNull final NavigableMap<Double, Double> availableAmountsOnOppositeSide, final boolean placingNewOrder, final boolean collectionsNeedParsing, final double worstOddsLimit) {
        // boolean placingNewOrder or canceling an existing order
        if (collectionsNeedParsing) {
            removeOwnAmountsFromAvailableTreeMap(availableAmountsOnOppositeSide, myUnmatchedAmounts);
            SharedStatics.orderCache.addTemporaryAmountsToOwnAmounts(marketId, runnerId, side, myUnmatchedAmounts);
        } else { // collections are already parsed
        }
        final NavigableSet<Double> myUnmatchedPrices = myUnmatchedAmounts.descendingKeySet(); // results in descending order for back and ascending order for lay
        double worstOddsThatCantBeReached = 0d;
        double myAmountsSum = 0d;
        for (final Double unmatchedPrice : myUnmatchedPrices) {
//            if (unmatchedPrice == null) {
//                logger.error("null unmatchedPrice in getWorstOddsThatCantBeReached for: {} {} {} {} {}", marketId, runnerId, side, Generic.objectToString(myUnmatchedAmounts), Generic.objectToString(availableAmountsOnOppositeSide));
//            } else {
            final Double unmatchedAmount = myUnmatchedAmounts.get(unmatchedPrice);
            final double unmatchedAmountPrimitive = unmatchedAmount == null ? 0d : unmatchedAmount;
//            myAmountsSum += unmatchedAmountPrimitive * (unmatchedPrice - 1d);
            myAmountsSum += unmatchedAmountPrimitive;
//            final SortedMap<Double, Double> worsePriceAvailableAmounts = side == Side.B ? availableAmountsOnOppositeSide.headMap(unmatchedPrice, placingNewOrder) : availableAmountsOnOppositeSide.tailMap(unmatchedPrice, placingNewOrder);
            final SortedMap<Double, Double> worsePriceAvailableAmounts = availableAmountsOnOppositeSide.headMap(unmatchedPrice, placingNewOrder); // headMap works for both, because the comparator of the map is used, and it's reversed between back and lay

            double worsePriceSum = 0d;
            for (final Map.Entry<Double, Double> worsePriceEntry : worsePriceAvailableAmounts.entrySet()) {
                final Double worsePriceAmount = worsePriceEntry.getValue();
                final double worseAmountPrimitive = worsePriceAmount == null ? 0d : worsePriceAmount;
//                final double worsePricePrimitive = worsePriceEntry.getKey();
//                worsePriceSum += worseAmountPrimitive * (worsePricePrimitive - 1d);
                worsePriceSum += worseAmountPrimitive;
            }

            // simple condition for deciding that my amount can't be reached
            if (worsePriceSum > ManagedRunner.HARD_TO_REACH_THRESHOLD * myAmountsSum) {
                if (oddsCompare(worstOddsLimit, unmatchedPrice, side) <= 0) {
                    worstOddsThatCantBeReached = unmatchedPrice; // only the last price will matter, all odds that are better or same will get canceled
                    logger.info("oddsThatCantBeReached {} for: {} {} {} {}", worstOddsThatCantBeReached, marketId, runnerId, side, placingNewOrder);
                } else { // case of mandatoryPlace with odds equal to the limit being protected; worse odds than the limit will be canceled later; nothing to be done on this branch
                }
            } else { // myAmountsSum always increases, worsePriceSum always decreases, so when this branch is entered once, it will be entered on all next loop iterations
                break; // breaks when 1 amount is not removed, and the next ones won't be removed
            }
//            }
        } // end for
        return worstOddsThatCantBeReached;
    }

    public static double getBestOddsWhereICanMoveAmountsToBetterOdds(final String marketId, final RunnerId runnerId, @NotNull final Side side, @NotNull final TreeMap<Double, Double> myUnmatchedAmounts,
                                                                     @NotNull final NavigableMap<Double, Double> availableAmountsOnOppositeSide) {
        return getBestOddsWhereICanMoveAmountsToBetterOdds(marketId, runnerId, side, myUnmatchedAmounts, availableAmountsOnOppositeSide, true);
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public static double getBestOddsWhereICanMoveAmountsToBetterOdds(final String marketId, final RunnerId runnerId, @NotNull final Side side, @NotNull final TreeMap<Double, Double> myUnmatchedAmounts,
                                                                     @NotNull final NavigableMap<Double, Double> availableAmountsOnOppositeSide, final boolean collectionsNeedParsing) {
        if (collectionsNeedParsing) {
            removeOwnAmountsFromAvailableTreeMap(availableAmountsOnOppositeSide, myUnmatchedAmounts);
//            SharedStatics.orderCache.addTemporaryAmountsToOwnAmounts(marketId, runnerId, side, myUnmatchedAmounts);
        } else { // collections are already parsed
        }
        final NavigableSet<Double> myUnmatchedPrices = myUnmatchedAmounts.descendingKeySet(); // results in descending order for back and ascending order for lay
        double bestOddsWhereICanMoveAmountsToBetterOdds = 0d;
        double myAmountsSum = 0d;
        for (final Double unmatchedPrice : myUnmatchedPrices) {
            final Double unmatchedAmount = myUnmatchedAmounts.get(unmatchedPrice);
            final double unmatchedAmountPrimitive = unmatchedAmount == null ? 0d : unmatchedAmount;
//            myAmountsSum += unmatchedAmountPrimitive * (unmatchedPrice - 1d);
            myAmountsSum += unmatchedAmountPrimitive;

            if ((oddsAreEqual(unmatchedPrice, 1_000d) && side == Side.B) || (oddsAreEqual(unmatchedPrice, 1.01d) && side == Side.L)) {
                continue;
            } else { // odds are not at maximum limit, so I'll see if they can be improved
            }
            // headMap works for both, because the comparator of the map is used, and it's reversed between back and lay
            final SortedMap<Double, Double> worsePriceAvailableAmounts =
                    side == Side.B ? availableAmountsOnOppositeSide.headMap(getNextOddsUp(unmatchedPrice, side), true) : availableAmountsOnOppositeSide.headMap(getNextOddsDown(unmatchedPrice, side), true);
            double worsePriceSum = 0d;
            for (final Map.Entry<Double, Double> worsePriceEntry : worsePriceAvailableAmounts.entrySet()) {
                final Double worsePriceAmount = worsePriceEntry.getValue();
                final double worseAmountPrimitive = worsePriceAmount == null ? 0d : worsePriceAmount;
//                final double worsePricePrimitive = worsePriceEntry.getKey();
//                worsePriceSum += worseAmountPrimitive * (worsePricePrimitive - 1d);
                worsePriceSum += worseAmountPrimitive;
            }

            // simple condition for deciding that my amount can't be reached
            if (worsePriceSum <= ManagedRunner.TINY_AMOUNTS_THRESHOLD * myAmountsSum) { // myAmountsSum always increases, worsePriceSum always decreases, so when this branch is entered once, it will be entered on all next loop iterations
                bestOddsWhereICanMoveAmountsToBetterOdds = unmatchedPrice; // only the last price will matter, all odds that are better or same will get canceled
                logger.info("getBestOddsWhereICanMoveAmountsToBetterOdds {} for: {} {} {}", bestOddsWhereICanMoveAmountsToBetterOdds, marketId, runnerId, side);
                break;
            } else { // odds I seek not found yet
            }
//            }
        } // end for
        return bestOddsWhereICanMoveAmountsToBetterOdds;
    }
}
