package info.fmro.shared.entities;

import info.fmro.shared.enums.OrderStatus;
import info.fmro.shared.enums.OrderType;
import info.fmro.shared.enums.PersistenceType;
import info.fmro.shared.enums.Side;
import info.fmro.shared.stream.objects.StreamSynchronizedMap;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public final class CurrentOrderSummary
        implements Serializable, Comparable<CurrentOrderSummary> {
    private static final Logger logger = LoggerFactory.getLogger(CurrentOrderSummary.class);
    public static final int BEFORE = -1, EQUAL = 0, AFTER = 1;
    @Serial
    private static final long serialVersionUID = -2498492858943069726L;
    private final String betId;
    private final String marketId;
    private final Long selectionId;
    private Double handicap;
    private PriceSize priceSize;
    private Double bspLiability;
    private Side side;
    private OrderStatus status;
    private PersistenceType persistenceType;
    private OrderType orderType;
    @Nullable
    private Date placedDate;
    @Nullable
    private Date matchedDate;
    private Double averagePriceMatched;
    private Double sizeMatched;
    private Double sizeRemaining;
    private Double sizeLapsed;
    private Double sizeCancelled;
    private Double sizeVoided;
    private String regulatorAuthCode;
    private String regulatorCode;
    private String customerOrderRef; // The order reference defined by the customer for this bet
    private String customerStrategyRef; // The strategy reference defined by the customer for this bet
    @Nullable
    private CurrentItemDescription currentItemDescription; // A container for all the ancillary data for this Item
    @SuppressWarnings("FieldHasSetterButNoGetter")
    private String eventId; // created using the marketId

    @Contract(pure = true)
    private CurrentOrderSummary(final String betId, final String marketId, final Long selectionId) {
        this.betId = betId;
        this.marketId = marketId;
        this.selectionId = selectionId;
    }

    public synchronized double getPlacedAmount() { // returns total amount placed on this order, unmatched included
        final double amount;

        if (this.priceSize == null) {
            amount = 0d;
        } else {
            final Double sizeObject = this.priceSize.getSize();
            final double size = sizeObject == null ? 0d : sizeObject;
            final Double priceObject = this.priceSize.getPrice();
            final double price = priceObject == null ? 0d : priceObject;

            if (this.side == null) {
                logger.error("null side in CurrentOrderSummary placedAmount: {}", Generic.objectToString(this));
                amount = Math.max(size, Formulas.calculateLayExposure(price, size)); // assume the worst
            } else {
                amount = switch (this.side) {
                    case BACK -> size;
                    case LAY -> Formulas.calculateLayExposure(price, size);
                    //noinspection UnnecessaryDefault
                    default -> {
                        logger.error("unknown side {} in CurrentOrderSummary placedAmount: {}", this.side, Generic.objectToString(this));
                        yield Math.max(size, Formulas.calculateLayExposure(price, size)); // assume the worst
                    }
                }; // end switch
            }
        }
        return amount;
    }

    public synchronized String getEventId(@NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        if (this.eventId == null) {
            createEventId(marketCataloguesMap);
        }
        return this.eventId;
    }

    private synchronized void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    private synchronized void createEventId(@NotNull final StreamSynchronizedMap<? super String, ? extends MarketCatalogue> marketCataloguesMap) {
        this.setEventId(Formulas.getEventIdOfMarketId(this.marketId, marketCataloguesMap));
        if (this.eventId == null) {
            logger.info("null eventId after creation in CurrentOrderSummary: {}", Generic.objectToString(this));
        }
    }

    public String getBetId() {
        return this.betId;
    }

    public synchronized String getMarketId() {
        return this.marketId;
    }

    public synchronized Long getSelectionId() {
        return this.selectionId;
    }

    public synchronized Double getHandicap() {
        return this.handicap;
    }

    public synchronized void setHandicap(final Double handicap) {
        this.handicap = handicap;
    }

    public synchronized PriceSize getPriceSize() {
        return this.priceSize;
    }

    public synchronized void setPriceSize(final PriceSize priceSize) {
        this.priceSize = priceSize;
    }

    public synchronized Double getBspLiability() {
        return this.bspLiability;
    }

    public synchronized void setBspLiability(final Double bspLiability) {
        this.bspLiability = bspLiability;
    }

    public synchronized Side getSide() {
        return this.side;
    }

    public synchronized void setSide(final Side side) {
        this.side = side;
    }

    public synchronized OrderStatus getStatus() {
        return this.status;
    }

    public synchronized void setStatus(final OrderStatus status) {
        this.status = status;
    }

    public synchronized PersistenceType getPersistenceType() {
        return this.persistenceType;
    }

    public synchronized void setPersistenceType(final PersistenceType persistenceType) {
        this.persistenceType = persistenceType;
    }

    public synchronized OrderType getOrderType() {
        return this.orderType;
    }

    public synchronized void setOrderType(final OrderType orderType) {
        this.orderType = orderType;
    }

    @Nullable
    public synchronized Date getPlacedDate() {
        return this.placedDate == null ? null : (Date) this.placedDate.clone();
    }

    public synchronized void setPlacedDate(final Date placedDate) {
        this.placedDate = placedDate == null ? null : (Date) placedDate.clone();
    }

    @Nullable
    public synchronized Date getMatchedDate() {
        return this.matchedDate == null ? null : (Date) this.matchedDate.clone();
    }

    public synchronized void setMatchedDate(final Date matchedDate) {
        this.matchedDate = matchedDate == null ? null : (Date) matchedDate.clone();
    }

    public synchronized Double getAveragePriceMatched() {
        return this.averagePriceMatched;
    }

    public synchronized void setAveragePriceMatched(final Double averagePriceMatched) {
        this.averagePriceMatched = averagePriceMatched;
    }

    public synchronized Double getSizeMatched() {
        return this.sizeMatched;
    }

    public synchronized void setSizeMatched(final Double sizeMatched) {
        this.sizeMatched = sizeMatched;
    }

    public synchronized Double getSizeRemaining() {
        return this.sizeRemaining;
    }

    public synchronized void setSizeRemaining(final Double sizeRemaining) {
        this.sizeRemaining = sizeRemaining;
    }

    public synchronized Double getSizeLapsed() {
        return this.sizeLapsed;
    }

    public synchronized void setSizeLapsed(final Double sizeLapsed) {
        this.sizeLapsed = sizeLapsed;
    }

    public synchronized Double getSizeCancelled() {
        return this.sizeCancelled;
    }

    public synchronized void setSizeCancelled(final Double sizeCancelled) {
        this.sizeCancelled = sizeCancelled;
    }

    public synchronized Double getSizeVoided() {
        return this.sizeVoided;
    }

    public synchronized void setSizeVoided(final Double sizeVoided) {
        this.sizeVoided = sizeVoided;
    }

    public synchronized String getRegulatorAuthCode() {
        return this.regulatorAuthCode;
    }

    public synchronized void setRegulatorAuthCode(final String regulatorAuthCode) {
        this.regulatorAuthCode = regulatorAuthCode;
    }

    public synchronized String getRegulatorCode() {
        return this.regulatorCode;
    }

    public synchronized void setRegulatorCode(final String regulatorCode) {
        this.regulatorCode = regulatorCode;
    }

    public synchronized String getCustomerOrderRef() {
        return this.customerOrderRef;
    }

    public synchronized void setCustomerOrderRef(final String customerOrderRef) {
        this.customerOrderRef = customerOrderRef;
    }

    public synchronized String getCustomerStrategyRef() {
        return this.customerStrategyRef;
    }

    public synchronized void setCustomerStrategyRef(final String customerStrategyRef) {
        this.customerStrategyRef = customerStrategyRef;
    }

    @Nullable
    public synchronized CurrentItemDescription getCurrentItemDescription() {
        return this.currentItemDescription;
    }

    public synchronized void setCurrentItemDescription(@Nullable final CurrentItemDescription currentItemDescription) {
        this.currentItemDescription = currentItemDescription;
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public int compareTo(@NotNull final CurrentOrderSummary o) {
        //noinspection ConstantConditions
        if (o == null) {
            return AFTER;
        }
        if (this == o) {
            return EQUAL;
        }

        //noinspection ConstantConditions
        if (this.getClass() != o.getClass()) {
            return this.getClass().hashCode() < o.getClass().hashCode() ? BEFORE : AFTER;
        }
        if (!Objects.equals(this.betId, o.betId)) {
            if (this.betId == null) {
                return BEFORE;
            }
            if (o.betId == null) {
                return AFTER;
            }
            return this.betId.compareTo(o.betId);
        }

        return EQUAL;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CurrentOrderSummary that = (CurrentOrderSummary) obj;
        return Objects.equals(this.betId, that.betId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.betId);
    }
}
