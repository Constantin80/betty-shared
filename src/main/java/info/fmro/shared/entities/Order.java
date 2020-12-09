package info.fmro.shared.entities;

import info.fmro.shared.enums.OrderStatus;
import info.fmro.shared.enums.OrderType;
import info.fmro.shared.enums.PersistenceType;
import info.fmro.shared.enums.Side;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@SuppressWarnings("unused")
class Order
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 3021807768896649660L;
    private String betId;
    private OrderType orderType;
    private OrderStatus status;
    private PersistenceType persistenceType;
    private Side side;
    private Double price;
    private Double size;
    private Double bspLiability;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private Date placedDate;
    private Double avgPriceMatched;
    private Double sizeMatched;
    private Double sizeRemaining;
    private Double sizeLapsed;
    private Double sizeCancelled;
    private Double sizeVoided;
    private String customerOrderRef; // The customer order reference sent for this bet
    private String customerStrategyRef; // The customer strategy reference sent for this bet

//    @SuppressWarnings("unused")
//    @Contract(pure = true)
//    private Order() {
//    }
//
//    @SuppressWarnings({"ConstructorWithTooManyParameters", "unused"})
//    @Contract(pure = true)
//    Order(final String betId, final OrderType orderType, final OrderStatus status, final PersistenceType persistenceType, final Side side, final Double price, final Double size, final Double bspLiability, @NotNull final Date placedDate,
//          final Double avgPriceMatched, final Double sizeMatched, final Double sizeRemaining, final Double sizeLapsed, final Double sizeCancelled, final Double sizeVoided) {
//        this.betId = betId;
//        this.orderType = orderType;
//        this.status = status;
//        this.persistenceType = persistenceType;
//        this.side = side;
//        this.price = price;
//        this.size = size;
//        this.bspLiability = bspLiability;
//        this.placedDate = (Date) placedDate.clone();
//        this.avgPriceMatched = avgPriceMatched;
//        this.sizeMatched = sizeMatched;
//        this.sizeRemaining = sizeRemaining;
//        this.sizeLapsed = sizeLapsed;
//        this.sizeCancelled = sizeCancelled;
//        this.sizeVoided = sizeVoided;
//    }

    public String getBetId() {
        return this.betId;
    }

    public OrderType getOrderType() {
        return this.orderType;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public PersistenceType getPersistenceType() {
        return this.persistenceType;
    }

    public Side getSide() {
        return this.side;
    }

    public Double getPrice() {
        return this.price;
    }

    public Double getSize() {
        return this.size;
    }

    public Double getBspLiability() {
        return this.bspLiability;
    }

    @Nullable
    public synchronized Date getPlacedDate() {
        return this.placedDate == null ? null : (Date) this.placedDate.clone();
    }

    public Double getAvgPriceMatched() {
        return this.avgPriceMatched;
    }

    public Double getSizeMatched() {
        return this.sizeMatched;
    }

    public Double getSizeRemaining() {
        return this.sizeRemaining;
    }

    public Double getSizeLapsed() {
        return this.sizeLapsed;
    }

    public Double getSizeCancelled() {
        return this.sizeCancelled;
    }

    public Double getSizeVoided() {
        return this.sizeVoided;
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Order order = (Order) obj;
        return Objects.equals(this.betId, order.betId) &&
               this.orderType == order.orderType &&
               this.status == order.status &&
               this.persistenceType == order.persistenceType &&
               this.side == order.side &&
               Objects.equals(this.price, order.price) &&
               Objects.equals(this.size, order.size) &&
               Objects.equals(this.bspLiability, order.bspLiability) &&
               Objects.equals(this.placedDate, order.placedDate) &&
               Objects.equals(this.avgPriceMatched, order.avgPriceMatched) &&
               Objects.equals(this.sizeMatched, order.sizeMatched) &&
               Objects.equals(this.sizeRemaining, order.sizeRemaining) &&
               Objects.equals(this.sizeLapsed, order.sizeLapsed) &&
               Objects.equals(this.sizeCancelled, order.sizeCancelled) &&
               Objects.equals(this.sizeVoided, order.sizeVoided) &&
               Objects.equals(this.customerOrderRef, order.customerOrderRef) &&
               Objects.equals(this.customerStrategyRef, order.customerStrategyRef);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(this.betId, this.orderType, this.status, this.persistenceType, this.side, this.price, this.size, this.bspLiability, this.placedDate, this.avgPriceMatched, this.sizeMatched, this.sizeRemaining, this.sizeLapsed,
                            this.sizeCancelled, this.sizeVoided, this.customerOrderRef, this.customerStrategyRef);
    }
}
