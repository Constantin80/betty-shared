package info.fmro.shared.logic;

import com.google.common.util.concurrent.AtomicDouble;
import info.fmro.shared.entities.CurrencyRate;
import info.fmro.shared.enums.SafetyLimitsModificationCommand;
import info.fmro.shared.stream.objects.ListOfQueues;
import info.fmro.shared.stream.objects.SerializableObjectModification;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.utility.Generic;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExistingFunds
        implements Serializable, StreamObjectInterface {
    private static final Logger logger = LoggerFactory.getLogger(ExistingFunds.class);
    private static final long serialVersionUID = 4629311467792245314L;
    public transient ListOfQueues listOfQueues = new ListOfQueues();
    public final AtomicDouble currencyRate = new AtomicDouble(1d); // GBP/EUR, 1.1187000274658203 right now, on 13-08-2018; default 1d
    private double reserve = 5_000d; // default value; will always be truncated to int; can only increase
    private double availableFunds; // total amount available on the account; it includes the reserve
    private double exposure; // total exposure on the account; it's a negative number

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();
    }

    public synchronized ExistingFunds getCopy() {
        return SerializationUtils.clone(this);
    }

    public synchronized double getReserve() {
//        if (!tempReserveMap.isEmpty()) {
//            // long currentTime = System.currentTimeMillis();
//            final Set<AtomicDouble> keys = tempReserveMap.keySet();
//            for (AtomicDouble key : keys) {
//                returnValue += key.get();
//            }
//
//            // Iterator<Entry<AtomicDouble, Long>> iterator = tempReserveMap.entrySet().iterator();
//            // while (iterator.hasNext()) {
//            //     Entry<AtomicDouble, Long> entry = iterator.next();
//            // if (entry.getValue() < currentTime) {
//            // iterator.remove(); // I remove when I process funds; this partly avoids errors with tempReserve removed and availableFunds not updated
//            // } else {
//            //     returnValue += entry.getKey().get();
//            // removed logging as well in order to optimise speed
//            //     if (Statics.debugLevel.check(3, 159)) {
//            //         logger.info("tempReserve: {} {}", returnValue, this.reserve);
//            //     }
//            // }
//            // }
//        }
        // if (!localUsedBalanceSet.isEmpty()) {
        //     for (AtomicDouble localUsedBalance : localUsedBalanceSet) {
        //         returnValue += localUsedBalance.get();
        //         logger.info("localReserve: {} {}", returnValue, this.reserve);
        //     }
        // }
        return this.reserve;
    }

    public synchronized boolean setReserve(final double newReserve) {
        final boolean modified;

        final double truncatedValue = Math.floor(newReserve);
        if (truncatedValue > this.reserve) {
            logger.info("modifying reserve value {} to {}", this.reserve, truncatedValue);
            this.listOfQueues.send(new SerializableObjectModification<>(SafetyLimitsModificationCommand.setReserve, truncatedValue));
            this.reserve = truncatedValue;
            modified = true;
        } else {
            modified = false;
        }
        return modified;
    }

    public synchronized double getAvailableFunds() {
        return this.availableFunds;
    }

    public synchronized void setAvailableFunds(final double newAvailableFunds) {
        this.listOfQueues.send(new SerializableObjectModification<>(SafetyLimitsModificationCommand.setAvailableFunds, newAvailableFunds));
        this.availableFunds = newAvailableFunds;
    }

    public synchronized double getExposure() {
        return this.exposure;
    }

    public synchronized void setExposure(final double newExposure) {
        this.listOfQueues.send(new SerializableObjectModification<>(SafetyLimitsModificationCommand.setExposure, newExposure));
        this.exposure = newExposure;
    }

    public synchronized void setCurrencyRate(final Iterable<? extends CurrencyRate> currencyRates, @NotNull final AtomicBoolean mustStop, @NotNull final AtomicBoolean needSessionToken) {
        // Market subscriptions - are always in underlying exchange currency - GBP
        // Orders subscriptions - are provided in the currency of the account that the orders are placed in
        if (currencyRates != null) {
            for (final CurrencyRate newCurrencyRate : currencyRates) {
                final String currencyCode = newCurrencyRate.getCurrencyCode();
                if (Objects.equals(currencyCode, "EUR")) {
                    final Double rate = newCurrencyRate.getRate();
                    if (rate != null) {
                        this.listOfQueues.send(new SerializableObjectModification<>(SafetyLimitsModificationCommand.setCurrencyRate, rate));
                        this.currencyRate.set(rate);
                    } else {
                        logger.error("null rate for: {}", Generic.objectToString(currencyRates));
                    }
                    break;
                } else { // I only need EUR rate, nothing to be done with the rest
                }
            } // end for
        } else {
            if (mustStop.get() && needSessionToken.get()) { // normal to happen during program stop, if not logged in
            } else {
                logger.error("currencyRates null");
            }
        }
    }
}
