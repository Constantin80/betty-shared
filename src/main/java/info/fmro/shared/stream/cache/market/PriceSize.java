package info.fmro.shared.stream.cache.market;

import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class PriceSize
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(PriceSize.class);
    @Serial
    private static final long serialVersionUID = 6795917492745798841L;
    private final double price;
    private double size; // markets stream has amounts in GBP

    public PriceSize(final List<Double> priceSize) {
        if (priceSize != null) {
            final int listSize = priceSize.size();
            if (listSize == 2) {
                final Double priceObject = priceSize.get(0), sizeObject = priceSize.get(1);
                if (priceObject == null || sizeObject == null) {
                    logger.error("null Double in priceSize list in PriceSize object creation: {} {} {}", priceObject, sizeObject, Generic.objectToString(priceSize));
                    this.price = 0d;
                    this.size = 0d;
                } else {
                    this.price = priceObject;
                    this.size = sizeObject;
                }
            } else {
                logger.error("wrong size {} for priceSize list in PriceSize object creation: {}", listSize, Generic.objectToString(priceSize));
                this.price = 0d;
                this.size = 0d;
            }
        } else {
            logger.error("null priceSize list in PriceSize object creation");
            this.price = 0d;
            this.size = 0d;
        }
    }

    public double getPrice() {
        return this.price;
    }

    @Contract(pure = true)
    private synchronized double getSize() {
        return this.size;
    }

    synchronized double getSizeGBP() {
        return getSize();
    }

    synchronized double getSizeEUR(final double currencyRate) {
        return getSize() * currencyRate;
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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PriceSize priceSize = (PriceSize) obj;
        return Double.compare(priceSize.price, this.price) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.price);
    }
}
