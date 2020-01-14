package info.fmro.shared.utility;

import info.fmro.shared.stream.enums.Side;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@SuppressWarnings("UtilityClass")
public final class Formulas {
    private static final Logger logger = LoggerFactory.getLogger(Formulas.class);
    //    public static final List<Double> pricesList = List.of(1.01, 1.02,1.03,1.04,1.05,1.06,1.07,1.08,1.09,1.1,1.11,1.12,1.13,1.14 ...);
    @SuppressWarnings("PublicStaticCollectionField")
    public static final List<Integer> pricesList; // odds prices, multiplied by 100, to have them stored as int
    @SuppressWarnings("PublicStaticCollectionField")
    public static final Map<String, String> charactersMap = Collections.synchronizedMap(new LinkedHashMap<>(128, 0.75f));

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

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static double getNStepDifferentOdds(final double baseOdds, final int nSteps) {
        //noinspection NumericCastThatLosesPrecision
        final int intBaseOdds = (int) (baseOdds * 100d);

        return getNStepDifferentOdds(intBaseOdds, nSteps);
    }

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    private static double getNStepDifferentOdds(final int baseOdds, final int nSteps) {
        final double result;
        final int baseOddsPosition = pricesList.indexOf(baseOdds);
        if (baseOddsPosition < 0) {
            logger.error("baseOdds {} not found in pricesList during getNStepDifferentOdds {}: {}", baseOdds, nSteps, baseOddsPosition);
            result = nSteps <= 0 ? 1.01d : 1_000d;
        } else {
            final int listSize = pricesList.size();
            final int resultPosition = Math.max(Math.min(baseOddsPosition + nSteps, listSize - 1), 0);
            result = (double) (pricesList.get(resultPosition)) / 100d;
        }

        return result;
    }

    @Contract(pure = true)
    public static boolean oddsAreUsable(final double odds) {
        return odds <= 1_000d && odds >= 1.01d;
    }

    private static double inverseOdds(final double odds) {
        final double returnValue;
        if (oddsAreUsable(odds)) {
            returnValue = 1d / (odds - 1d) + 1d;
        } else {
            logger.error("unusable odds in Formulas.inverseOdds for: {}", odds);
            returnValue = 0d;
        }
        return returnValue;
    }

    private static int getOddsPosition(final double odds) {
        @SuppressWarnings("NumericCastThatLosesPrecision") final int intOdds = (int) (odds * 100d);
        return pricesList.indexOf(intOdds);
    }

    @SuppressWarnings("OverlyNestedMethod")
    public static boolean oddsAreInverse(final double firstOdds, final double secondOdds) {
        final boolean areInverse;
        if (oddsAreUsable(firstOdds) && oddsAreUsable(secondOdds)) {
            final int secondOddsPosition = getOddsPosition(secondOdds);
            if (secondOddsPosition >= 0) {
                final double inverseFirstOdds = inverseOdds(firstOdds);
                @SuppressWarnings("NumericCastThatLosesPrecision") final int intInverseFirstOdds = (int) (inverseFirstOdds * 100d);
                if (intInverseFirstOdds > 10_100 || intInverseFirstOdds < 100) {
                    logger.error("bad value for intInverseFirstOdds in Formulas.oddsAreInverse for: {} {} {}", intInverseFirstOdds, firstOdds, secondOdds);
                    areInverse = false;
                } else {
                    if (inverseFirstOdds <= 101) {
                        areInverse = secondOddsPosition == 0;
                    } else if (intInverseFirstOdds == 10_100) {
                        areInverse = secondOdds >= inverseFirstOdds;
                    } else {
                        @SuppressWarnings("NumericCastThatLosesPrecision") final int intSecondOdds = (int) (secondOdds * 100d);
                        if (intInverseFirstOdds == intSecondOdds) {
                            areInverse = true;
                        } else {
                            if (intInverseFirstOdds > intSecondOdds) {
                                final int intNextOdds = pricesList.get(secondOddsPosition + 1);
                                areInverse = intInverseFirstOdds < intNextOdds;
                            } else {
                                final int intPreviousOdds = pricesList.get(secondOddsPosition - 1);
                                areInverse = intInverseFirstOdds > intPreviousOdds;
                            }
                        }
                    }
                }
            } else {
                logger.error("secondOddsPosition negative in Formulas.oddsAreInverse for: {} {}", firstOdds, secondOdds);
                areInverse = false;
            }
        } else {
            logger.error("unusable odds in Formulas.oddsAreInverse for: {} {}", firstOdds, secondOdds);
            areInverse = false;
        }

        return areInverse;
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

    public static boolean oddsAreWorse(final Side side, final double worstAcceptedOdds, final double price) {
        final boolean areWorse;

        if (side == null) {
            logger.error("null side in oddsAreWorse for: {} {} {}", side, worstAcceptedOdds, price);
            areWorse = false;
        } else if (!oddsAreUsable(worstAcceptedOdds)) {
            logger.error("unusable worstAcceptedOdds in oddsAreWorse for: {} {} {}", side, worstAcceptedOdds, price);
            areWorse = false;
        } else if (!oddsAreUsable(price)) {
            logger.error("unusable price in oddsAreWorse for: {} {} {}", side, worstAcceptedOdds, price);
            areWorse = true;
        } else if (side == Side.B) {
            areWorse = !(price >= worstAcceptedOdds);
        } else if (side == Side.L) {
            areWorse = !(price <= worstAcceptedOdds);
        } else {
            logger.error("strange unsupported side in oddsAreWorse for: {} {} {}", side, worstAcceptedOdds, price);
            areWorse = false;
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

    public static void removeOwnAmountsFromAvailableTreeMap(@NotNull final TreeMap<Double, Double> availableAmounts, @NotNull final TreeMap<Double, Double> amountsFromMyUnmatchedOrders) {
        for (final Map.Entry<Double, Double> entry : availableAmounts.entrySet()) {
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
                    availableAmounts.replace(price, 0d);
                } else {
                    availableAmounts.replace(price, amountFromOthers);
                }
            }
        }
    }
}
