package info.fmro.shared.stream.cache.order;

import info.fmro.shared.objects.Exposure;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
    private static final long serialVersionUID = -3286453082383433322L;
    private final double price;
    private double size; // my orders stream has amounts in EUR

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

    synchronized double getSize() {
        return this.size;
    }

    synchronized void updateBackProfitExposure(@NotNull final Exposure exposure) {
        if (this.price == 0d || this.size == 0d) { // error message was probably printed during creation
        } else if (this.price <= 1d) {
            logger.error("bogus price {} in PriceSize for: {}", this.price, Generic.objectToString(this));
            this.size = 0d;
        } else {
            exposure.addBackMatchedProfit(Formulas.layExposure(this.price, getSize()));
            exposure.addBackMatchedExposure(getSize());
        }
    }

    synchronized void updateLayProfitExposure(@NotNull final Exposure exposure) {
        if (this.price == 0d || this.size == 0d) { // error message was probably printed during creation
        } else if (this.price <= 1d) {
            logger.error("bogus price {} in PriceSize for: {}", this.price, Generic.objectToString(this));
            this.size = 0d;
        } else {
            exposure.addLayMatchedProfit(getSize());
            exposure.addLayMatchedExposure(Formulas.layExposure(this.price, getSize()));
        }
    }
//    synchronized TwoDoubles getBackProfitExposurePair() { // this works for back; for lay profit and exposure are reversed
//        final double profit, exposure;
//
//        if (this.price == 0d || this.size == 0d) { // error message was probably printed during creation
//            profit = 0d;
//            exposure = 0d;
//        } else if (this.price <= 1d) {
//            logger.error("bogus price {} in PriceSize for: {}", this.price, Generic.objectToString(this));
//            this.size = 0d;
//            profit = 0d;
//            exposure = 0d;
//        } else {
//            profit = Formulas.layExposure(this.price, this.size);
//            exposure = this.size;
//        }
//
//        return new TwoDoubles(profit, exposure);
//    }

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
        final PriceSize priceSize = (PriceSize) obj;
        return Double.compare(priceSize.price, this.price) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.price);
    }
}
