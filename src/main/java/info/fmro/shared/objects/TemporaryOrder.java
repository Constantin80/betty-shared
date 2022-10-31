package info.fmro.shared.objects;

import info.fmro.shared.enums.TemporaryOrderType;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class TemporaryOrder
        implements Serializable {
    //    public static final long defaultTooOldPeriod = Generic.MINUTE_LENGTH_MILLISECONDS * 10L;
    @Serial
    private static final long serialVersionUID = -6977868264246613172L;
    private static final Logger logger = LoggerFactory.getLogger(TemporaryOrder.class);
    @NotNull
    private final TemporaryOrderType type;
    private final String marketId;
    private final RunnerId runnerId;
    private final Side side;
    private final double price, size;
    @Nullable
    private final Double sizeReduction;
    private final long creationTime;
    private String betId;
    private final String reasonId; // id for identifying where in my program the order originated
    private long expirationTime;

    @SuppressWarnings("ConstructorWithTooManyParameters")
    public TemporaryOrder(final String marketId, final RunnerId runnerId, final Side side, final double price, final double size, final boolean bettingDisabled, final String reasonId) {
        this.type = TemporaryOrderType.PLACE;
        this.marketId = marketId;
        this.runnerId = runnerId;
        this.side = side;
        this.price = price;
        this.size = Generic.roundDouble(size);
        this.sizeReduction = null;
        this.reasonId = reasonId;
        this.creationTime = System.currentTimeMillis();
        this.expirationTime = this.creationTime + (bettingDisabled ? Generic.DAY_LENGTH_MILLISECONDS : (Generic.MINUTE_LENGTH_MILLISECONDS << 1)); // default, I can't let the default be 0
    }

    @SuppressWarnings("ConstructorWithTooManyParameters")
    public TemporaryOrder(final String marketId, final RunnerId runnerId, final Side side, final double price, final double size, final String betId, @Nullable final Double sizeReduction, final boolean bettingDisabled, final String reasonId) {
        this.type = TemporaryOrderType.CANCEL;
        this.marketId = marketId;
        this.betId = betId;
        this.runnerId = runnerId;
        this.side = side;
        this.price = price;
        this.size = Generic.roundDouble(size);
        this.reasonId = reasonId;
        this.creationTime = System.currentTimeMillis();
        this.expirationTime = this.creationTime + (bettingDisabled ? Generic.DAY_LENGTH_MILLISECONDS : (Generic.MINUTE_LENGTH_MILLISECONDS << 1)); // default, I can't let the default be 0

        if (sizeReduction != null && sizeReduction > size) {
            this.sizeReduction = null;
            //noinspection ThisEscapedInObjectConstruction
            logger.error("temporaryOrder sizeReduction {} larger than size {} for: {}", sizeReduction, size, Generic.objectToString(this));
        } else {
            this.sizeReduction = sizeReduction == null ? null : Generic.roundDouble(sizeReduction);
        }
    }

    public synchronized boolean placePriceEquals(final String marketIdToCheck, final RunnerId runnerIdToCheck, final Side sideToCheck, final double priceToCheck) {
        return this.type == TemporaryOrderType.PLACE && runnerEquals(marketIdToCheck, runnerIdToCheck) && sideToCheck == this.side && Double.compare(priceToCheck, this.price) == 0;
    }

    public synchronized boolean runnerEquals(final String marketIdToCheck, final RunnerId runnerIdToCheck) {
        return marketIdToCheck != null && marketIdToCheck.equals(this.marketId) && runnerIdToCheck != null && runnerIdToCheck.equals(this.runnerId);
    }

    public synchronized void updateExposure(@NotNull final Exposure exposure) {
        if (this.type == TemporaryOrderType.CANCEL) {
            if (this.side == Side.B) {
                exposure.addBackTempCancelExposure(this.sizeReduction == null ? Exposure.HUGE_AMOUNT : this.sizeReduction);
            } else if (this.side == Side.L) {
                exposure.addLayTempCancelExposure(Formulas.calculateLayExposure(this.price, this.sizeReduction == null ? Exposure.HUGE_AMOUNT : this.sizeReduction));
            } else {
                logger.error("unknown side in temporaryOrder updateExposure for: {} {}", this.side, Generic.objectToString(this));
            }
        } else if (this.type == TemporaryOrderType.PLACE) {
            if (this.side == Side.B) {
                exposure.addBackTempExposure(this.size);
                exposure.addBackPotentialTempProfit(Formulas.calculateLayExposure(this.price, this.size));
            } else if (this.side == Side.L) {
                exposure.addLayTempExposure(Formulas.calculateLayExposure(this.price, this.size));
                exposure.addLayPotentialTempProfit(this.size);
            } else {
                logger.error("unknown side in temporaryOrder updateExposure for: {} {}", this.side, Generic.objectToString(this));
            }
        } else {
            logger.error("unknown temporaryOrder type {} in updateExposure: {}", this.type, Generic.objectToString(this));
        }
    }

    @NotNull
    public TemporaryOrderType getType() {
        return this.type;
    }

    public String getMarketId() {
        return this.marketId;
    }

    public RunnerId getRunnerId() {
        return this.runnerId;
    }

    public Side getSide() {
        return this.side;
    }

    public double getPrice() {
        return this.price;
    }

    public double getSize() {
        return this.size;
    }

    @Nullable
    public Double getSizeReduction() {
        return this.sizeReduction;
    }

    public String getReasonId() {
        return this.reasonId;
    }

    public synchronized long getCreationTime() {
        return this.creationTime;
    }

    public synchronized String getBetId() {
        return this.betId;
    }

    public synchronized void setBetId(final String betId) {
        this.betId = betId;
    }

    public synchronized long getExpirationTime() {
        return this.expirationTime;
    }

    public synchronized void setExpirationTime(final long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public synchronized boolean isExpired() {
        final long currentTime = System.currentTimeMillis();
        return isExpired(currentTime);
    }

    @Contract(pure = true)
    private synchronized boolean isExpired(final long currentTime) {
        return this.expirationTime > 0L && currentTime >= this.expirationTime;
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
        final TemporaryOrder that = (TemporaryOrder) obj;
        return Double.compare(that.price, this.price) == 0 &&
               Double.compare(that.size, this.size) == 0 &&
               this.type == that.type &&
               Objects.equals(this.marketId, that.marketId) &&
               Objects.equals(this.runnerId, that.runnerId) &&
               this.side == that.side &&
               Objects.equals(this.sizeReduction, that.sizeReduction);
        // && Objects.equals(this.betId, that.betId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.marketId, this.runnerId, this.side, this.price, this.size, this.sizeReduction); // , this.betId
    }
}
