package info.fmro.shared.entities;

import info.fmro.shared.enums.OrderType;
import info.fmro.shared.enums.Side;
import info.fmro.shared.utility.Generic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class PlaceInstruction
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(PlaceInstruction.class);
    private static final long serialVersionUID = -1136840098955839521L;
    private OrderType orderType;
    private Long selectionId;
    private Double handicap;
    private Side side;
    private LimitOrder limitOrder;
    private LimitOnCloseOrder limitOnCloseOrder;
    private MarketOnCloseOrder marketOnCloseOrder;
    @SuppressWarnings("unused")
    private String customerOrderRef; // An optional reference customers can set to identify instructions.. No validation will be done on uniqueness and the string is limited to 32 characters. If an empty string is provided it will be treated as null.

    public synchronized double getPlacedAmount(final boolean isEachWayMarket) {
        double amount;

        if (this.orderType == null) {
            logger.error("null orderType in PlaceInstruction getPlacedAmount for: {}", Generic.objectToString(this));
            amount = 0d; // initialized

            if (this.limitOrder != null) {
                amount += this.limitOrder.getLiability(this.side, isEachWayMarket);
            }
            if (this.limitOnCloseOrder != null) {
                final Double limitOnCloseOrderLiabilityObject = this.limitOnCloseOrder.getLiability();
                final double limitOnCloseOrderLiability = limitOnCloseOrderLiabilityObject == null ? 0d : limitOnCloseOrderLiabilityObject;
                amount += limitOnCloseOrderLiability;
            }
            if (this.marketOnCloseOrder != null) {
                final Double marketOnCloseOrderLiabilityObject = this.marketOnCloseOrder.getLiability();
                final double marketOnCloseOrderLiability = marketOnCloseOrderLiabilityObject == null ? 0d : marketOnCloseOrderLiabilityObject;
                amount += marketOnCloseOrderLiability;
            }
        } else if (this.orderType == OrderType.LIMIT) {
            amount = this.limitOrder.getLiability(this.side, isEachWayMarket);
        } else if (this.orderType == OrderType.LIMIT_ON_CLOSE) {
            final Double limitOnCloseOrderLiabilityObject = this.limitOnCloseOrder.getLiability();
            amount = limitOnCloseOrderLiabilityObject == null ? 0d : limitOnCloseOrderLiabilityObject;
        } else if (this.orderType == OrderType.MARKET_ON_CLOSE) {
            final Double marketOnCloseOrderLiabilityObject = this.marketOnCloseOrder.getLiability();
            amount = marketOnCloseOrderLiabilityObject == null ? 0d : marketOnCloseOrderLiabilityObject;
        } else { // unsupported OrderType
            logger.error("unsupported orderType {} in PlaceInstruction getPlacedAmount for: {}", this.orderType, Generic.objectToString(this));
            amount = 0d; // initialized

            if (this.limitOrder != null) {
                amount += this.limitOrder.getLiability(this.side, isEachWayMarket);
            }
            if (this.limitOnCloseOrder != null) {
                final Double limitOnCloseOrderLiabilityObject = this.limitOnCloseOrder.getLiability();
                final double limitOnCloseOrderLiability = limitOnCloseOrderLiabilityObject == null ? 0d : limitOnCloseOrderLiabilityObject;
                amount += limitOnCloseOrderLiability;
            }
            if (this.marketOnCloseOrder != null) {
                final Double marketOnCloseOrderLiabilityObject = this.marketOnCloseOrder.getLiability();
                final double marketOnCloseOrderLiability = marketOnCloseOrderLiabilityObject == null ? 0d : marketOnCloseOrderLiabilityObject;
                amount += marketOnCloseOrderLiability;
            }
        }

        return amount;
    }

    public synchronized OrderType getOrderType() {
        return this.orderType;
    }

    public synchronized void setOrderType(final OrderType orderType) {
        this.orderType = orderType;
    }

    public synchronized Long getSelectionId() {
        return this.selectionId;
    }

    public synchronized void setSelectionId(final Long selectionId) {
        this.selectionId = selectionId;
    }

    public synchronized Double getHandicap() {
        return this.handicap;
    }

    public synchronized void setHandicap(final Double handicap) {
        this.handicap = handicap;
    }

    public synchronized Side getSide() {
        return this.side;
    }

    public synchronized void setSide(final Side side) {
        this.side = side;
    }

    public synchronized LimitOrder getLimitOrder() {
        return this.limitOrder;
    }

    public synchronized void setLimitOrder(final LimitOrder limitOrder) {
        this.limitOrder = limitOrder;
    }

    public synchronized LimitOnCloseOrder getLimitOnCloseOrder() {
        return this.limitOnCloseOrder;
    }

    public synchronized void setLimitOnCloseOrder(final LimitOnCloseOrder limitOnCloseOrder) {
        this.limitOnCloseOrder = limitOnCloseOrder;
    }

    public synchronized MarketOnCloseOrder getMarketOnCloseOrder() {
        return this.marketOnCloseOrder;
    }

    public synchronized void setMarketOnCloseOrder(final MarketOnCloseOrder marketOnCloseOrder) {
        this.marketOnCloseOrder = marketOnCloseOrder;
    }
}
