package info.fmro.shared.utility;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class ComparatorMarketPrices
        implements Comparator<Double>, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ComparatorMarketPrices.class);
    @Serial
    private static final long serialVersionUID = 3658851992618501057L;

    @Override
    public synchronized int compare(final Double o1, final Double o2) {
        final int result;
        if (Objects.equals(o1, o2)) {
            result = 0;
        } else if (o1 == null) {
            logger.error("null left value in ComparatorMarketPrices: {} {}", o1, o2);
            result = 1;
        } else if (o2 == null) {
            logger.error("null right value in ComparatorMarketPrices: {} {}", o1, o2);
            result = -1;
        } else if (o1.isNaN()) {
            logger.error("NaN left value in ComparatorMarketPrices: {} {}", o1, o2);
            result = 1;
        } else if (o2.isNaN()) {
            logger.error("NaN right value in ComparatorMarketPrices: {} {}", o1, o2);
            result = -1;
        } else {
            final boolean o1Valid = isValidRangePrice(o1), o2Valid = isValidRangePrice(o2);
            if (o1Valid && !o2Valid) {
                logger.error("invalid o2 in ComparatorMarketPrices: {} {}", o1, o2);
                result = -1;
            } else if (!o1Valid && o2Valid) {
                logger.error("invalid o1 in ComparatorMarketPrices: {} {}", o1, o2);
                result = 1;
            } else if (!o1Valid) {
                logger.error("invalid o1 and o2 in ComparatorMarketPrices: {} {}", o1, o2);
                result = 0;
            } else if (o1 == 0d) {
                result = 1;
            } else if (o2 == 0d) {
                result = -1;
            } else {
                result = Double.compare(o1, o2);
            }
        }
        return result;
    }

    private synchronized boolean isValidRangePrice(@NotNull final Double value) {
        return value == 0d || (value >= 1.01d && value <= 1_000d);
    }
}
