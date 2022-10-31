package info.fmro.shared.stream.definitions;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

class LevelPriceSize
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(LevelPriceSize.class);
    @Serial
    private static final long serialVersionUID = 996457594745721075L;
    private final int level; // 0 - 9, 0 best odds, 9 last odds
    private final double price;
    private double size; // markets stream has amounts in GBP

    LevelPriceSize(final List<Double> levelPriceSize) {
        if (levelPriceSize != null) {
            final int listSize = levelPriceSize.size();
            if (listSize == 3) {
                final Double levelObject = levelPriceSize.get(0), priceObject = levelPriceSize.get(1), sizeObject = levelPriceSize.get(2);
                if (levelObject == null || priceObject == null || sizeObject == null) {
                    logger.error("null Double in levelPriceSize list in LevelPriceSize object creation: {} {} {} {}", levelObject, priceObject, sizeObject, Generic.objectToString(levelPriceSize));
                    this.level = 0;
                    this.price = 0d;
                    this.size = 0d;
                } else {
                    this.level = levelObject.intValue();
                    this.price = priceObject;
                    this.size = sizeObject;
                }
            } else {
                logger.error("wrong size {} for levelPriceSize list in LevelPriceSize object creation: {}", listSize, Generic.objectToString(levelPriceSize));
                this.level = 0;
                this.price = 0d;
                this.size = 0d;
            }
        } else {
            logger.error("null levelPriceSize list in LevelPriceSize object creation");
            this.level = 0;
            this.price = 0d;
            this.size = 0d;
        }
    }

    int getLevel() {
        return this.level;
    }

    public double getPrice() {
        return this.price;
    }

    @Contract(pure = true)
    private double getSize() {
        return this.size;
    }

    synchronized double getSizeGBP() {
        return getSize();
    }

    @SuppressWarnings("unused")
    public synchronized double getSizeEUR(@NotNull final AtomicDouble currencyRate) {
        return getSize() * currencyRate.get();
    }

    synchronized void removeAmountGBP(final double sizeToRemove) { // package private method
        if (this.size < 0d) {
            logger.error("negative size {} in PriceSize for: {}", this.size, Generic.objectToString(this));
        } else if (sizeToRemove < 0d) {
            logger.error("negative sizeToRemove {} in PriceSize.removeAmount for: {}", sizeToRemove, Generic.objectToString(this));
        } else {
            this.size -= sizeToRemove;
            if (this.size < 0d) {
                this.size = 0d;
            } else { // new size is fine, nothing to be done
            }
        }
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
        final LevelPriceSize that = (LevelPriceSize) obj;
        return this.level == that.level &&
               Double.compare(that.price, this.price) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.level, this.price);
    }

    //    @Override
//    public String toString() {
//        return this.level + ":" + this.size + "@" + this.price;
//    }
}
