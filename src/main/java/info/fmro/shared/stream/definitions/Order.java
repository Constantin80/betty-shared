package info.fmro.shared.stream.definitions;

import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.stream.enums.LapseStatusReasonCode;
import info.fmro.shared.stream.enums.OrderStatus;
import info.fmro.shared.stream.enums.OrderType;
import info.fmro.shared.stream.enums.PersistenceType;
import info.fmro.shared.stream.enums.Side;
import info.fmro.shared.stream.objects.RunnerId;
import info.fmro.shared.utility.Formulas;
import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;

// objects of this class are read from the stream
@SuppressWarnings("OverlyComplexClass")
public class Order
        implements Serializable { // amounts are in account currency (EUR)
    @Serial
    private static final long serialVersionUID = 3021807768896649660L;
    private static final Logger logger = LoggerFactory.getLogger(Order.class);
    private Double avp; // Average Price Matched - the average price the order was matched at (null if the order is not matched).
    //                     This value is not meaningful for activity on Line markets and is not guaranteed to be returned or maintained for these markets.
    private Double bsp; // BSP Liability - the BSP liability of the order (null if the order is not a BSP order)
    private String id; // Bet Id - the id of the order
    @Nullable
    private Date ld; // Lapsed Date - the date the order was lapsed (null if the order is not lapsed)
    @Nullable
    private Date md; //  Matched Date - the date the order was matched (null if the order is not matched)
    private OrderType ot; // Order Type - the type of the order (L = LIMIT, MOC = MARKET_ON_CLOSE, LOC = LIMIT_ON_CLOSE)
    private LapseStatusReasonCode lsrc; // Lapse Status Reason Code - the reason that some or all of this order has been lapsed (null if no portion of the order is lapsed
    private Double p; // Price - the original placed price of the order. Line markets operate at even-money odds of 2.0. However, price for these markets refers to the line positions available as defined by the markets min-max range and interval steps
    @Nullable
    private Date pd; // Placed Date - the date the order was placed
    private PersistenceType pt; // Persistence Type - whether the order will persist at in play or not (L = LAPSE, P = PERSIST, MOC = Market On Close)
    private String rac; // Regulator Auth Code - the auth code returned by the regulator
    private String rc; // Regulator Code - the regulator of the order
    private String rfo; // Order Reference - the customer's order reference for this order (empty string if one was not set)
    private String rfs; // Strategy Reference - the customer's strategy reference for this order (empty string if one was not set)
    private Double s; // Size - the original placed size of the order
    private Double sc; // Size Cancelled - the amount of the order that has been cancelled
    private Side side; // Side - the side of the order. For Line markets a 'B' bet refers to a SELL line and an 'L' bet refers to a BUY line.
    private Double sl; // Size Lapsed - the amount of the order that has been lapsed
    private Double sm; // Size Matched - the amount of the order that has been matched
    private Double sr; // Size Remaining - the amount of the order that is remaining unmatched
    private OrderStatus status; // Status - the status of the order (E = EXECUTABLE, EC = EXECUTION_COMPLETE)
    private Double sv; // Size Voided - the amount of the order that has been voided
    //    private double backExposure, layExposure, backProfit, layProfit;
    private transient double sizeTempCanceled;

    public synchronized void calculateExposureAndProfit(@NotNull final Exposure exposure) {
        if (this.p == null || this.p <= 1d || this.sr == null || this.status == null || this.side == null) {
            logger.error("null or bogus fields in Order calculateExposureAndProfit: {} {} {} {} {}", this.p, this.sr, this.status, this.side, Generic.objectToString(this));
        } else if (this.status == OrderStatus.EC) { // execution complete, nothing should be remaining
        } else {
            switch (this.side) {
                case B -> {
                    exposure.addBackUnmatchedExposure(this.sr);
                    exposure.addBackPotentialUnmatchedProfit(Formulas.calculateLayExposure(this.p, this.sr));
                }
                case L -> {
                    exposure.addLayUnmatchedExposure(Formulas.calculateLayExposure(this.p, this.sr));
                    exposure.addLayPotentialUnmatchedProfit(this.sr);
                }
                default -> logger.error("strange side in Order calculateExposureAndProfit: {} {}", this.side, Generic.objectToString(this));
            }
        }
//        if (this.backExposure != 0d && this.layExposure != 0d) { // this condition should always be false, only one type of exposure should exist
//            logger.error("strange exposure in Order calculateExposureAndProfit: {} {} {}", this.backExposure, this.layExposure, Generic.objectToString(this));
//        }
    }

    public synchronized double cancelOrder(final String marketId, final RunnerId runnerId, @NotNull final ManagedRunner managedRunner, @NotNull final Method sendPostRequestRescriptMethod, final String reason) { // full cancel
        return cancelOrder(marketId, runnerId, null, managedRunner, sendPostRequestRescriptMethod, reason);
    }

    public synchronized double cancelOrder(final String marketId, final RunnerId runnerId, final Double sizeReduction, @NotNull final ManagedRunner managedRunner, @NotNull final Method sendPostRequestRescriptMethod, final String reason) {
        if (this.p == null || this.side == null || this.id == null) {
            logger.error("null variables during cancelOrder for: {} {} {} {} {}", this.p, this.side, this.id, reason, Generic.objectToString(this));
            if (this.p == null) { // avoids exception when converting to primitive
                this.p = 1.01d;
            } else { // proper price exists, nothing to be done
            }
        } else { // no error, nothing to be done, method will continue
        }
        final double sizeRemaining = this.sr == null ? 0d : this.sr;
        return SharedStatics.orderCache.addCancelOrder(marketId, runnerId, this.side, this.p, sizeRemaining, this.getSrConsideringTempCancel(), this.id, sizeReduction, managedRunner, sendPostRequestRescriptMethod, reason);
    }

    public synchronized double removeExposure(final String marketId, final RunnerId runnerId, final Side sideToRemove, final double excessExposure, @NotNull final ManagedRunner managedRunner, @NotNull final Method sendPostRequestRescriptMethod,
                                              final String reason) {
        final double exposureReduction;
        if (this.p == null || this.side == null || this.id == null || !Formulas.oddsAreUsable(this.p)) {
            logger.error("null variables or bad odds during removeExposure for: {} {} {} {} {} {} {} {} {}", marketId, runnerId, sideToRemove, excessExposure, this.p, this.side, this.id, reason, Generic.objectToString(this));
            exposureReduction = 0d;
        } else if (Generic.roundDoubleAmount(excessExposure) == 0d) {
            exposureReduction = 0d;
        } else if (sideToRemove == this.side && (sideToRemove == Side.B || sideToRemove == Side.L)) {
            final double sizeRemaining = this.sr == null ? 0d : this.sr;
            @Nullable final Double sizeReduction;
            if (this.side == Side.B) {
                if (excessExposure > sizeRemaining) {
                    sizeReduction = null;
                } else {
                    sizeReduction = excessExposure;
                }
            } else {
                if (excessExposure > Formulas.calculateLayExposure(this.p, sizeRemaining)) {
                    sizeReduction = null;
                } else {
                    sizeReduction = excessExposure / (this.p - 1d);
                }
            }
            exposureReduction = SharedStatics.orderCache.addCancelOrder(marketId, runnerId, this.side, this.p, sizeRemaining, this.getSrConsideringTempCancel(), this.id, sizeReduction, managedRunner, sendPostRequestRescriptMethod, reason);
        } else {
            logger.error("wrong side in removeExposure for: {} {} {} {} {} {} {}", sideToRemove, this.side, marketId, runnerId, excessExposure, reason, Generic.objectToString(this));
            exposureReduction = 0d;
        }
        return exposureReduction;
    }

    public synchronized double getSrConsideringTempCancel() {
        return this.sr == null ? 0d : Math.max(0d, this.sr - this.sizeTempCanceled);
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized double getSizeTempCanceled() {
        final double sizeRemaining = this.sr == null ? 0d : this.sr;
        return Math.max(0d, Math.min(this.sizeTempCanceled, sizeRemaining));
    }

    public synchronized void resetSizeTempCanceled() {
        this.sizeTempCanceled = 0d;
    }

    public synchronized void addSizeTempCanceled(final Double size) {
        this.sizeTempCanceled += size == null ? Exposure.HUGE_AMOUNT : size;
    }

    public synchronized void removeSizeTempCanceled(final Double size) {
        this.sizeTempCanceled -= size == null ? Exposure.HUGE_AMOUNT : size;
    }

    @Nullable
    public synchronized Date getLd() {
        return this.ld == null ? null : (Date) this.ld.clone();
    }

    public synchronized void setLd(final Date ld) {
        this.ld = ld == null ? null : (Date) ld.clone();
    }

    @Nullable
    public synchronized Date getMd() {
        return this.md == null ? null : (Date) this.md.clone();
    }

    public synchronized void setMd(final Date md) {
        this.md = md == null ? null : (Date) md.clone();
    }

    public synchronized String getRac() {
        return this.rac;
    }

    public synchronized void setRac(final String rac) {
        this.rac = rac;
    }

    public synchronized String getRc() {
        return this.rc;
    }

    public synchronized void setRc(final String rc) {
        this.rc = rc;
    }

    public synchronized String getRfo() {
        return this.rfo;
    }

    public synchronized void setRfo(final String rfo) {
        this.rfo = rfo;
    }

    public synchronized String getRfs() {
        return this.rfs;
    }

    public synchronized void setRfs(final String rfs) {
        this.rfs = rfs;
    }

    public synchronized String getId() {
        return this.id;
    }

    public synchronized void setId(final String id) {
        this.id = id;
    }

    public synchronized OrderType getOt() {
        return this.ot;
    }

    public synchronized void setOt(final OrderType ot) {
        this.ot = ot;
    }

    public synchronized OrderStatus getStatus() {
        return this.status;
    }

    public synchronized void setStatus(final OrderStatus status) {
        this.status = status;
    }

    public synchronized PersistenceType getPt() {
        return this.pt;
    }

    public synchronized void setPt(final PersistenceType pt) {
        this.pt = pt;
    }

    public synchronized Side getSide() {
        return this.side;
    }

    public synchronized void setSide(final Side side) {
        this.side = side;
    }

    public synchronized LapseStatusReasonCode getLsrc() {
        return this.lsrc;
    }

    public synchronized void setLsrc(final LapseStatusReasonCode lsrc) {
        this.lsrc = lsrc;
    }

    public synchronized Double getP() {
        return this.p;
    }

    public synchronized void setP(final Double p) {
        this.p = p;
    }

    public synchronized Double getS() {
        return this.s;
    }

    public synchronized void setS(final Double s) {
        this.s = s;
    }

    public synchronized Double getBsp() {
        return this.bsp;
    }

    public synchronized void setBsp(final Double bsp) {
        this.bsp = bsp;
    }

    @Nullable
    public synchronized Date getPd() {
        return this.pd == null ? null : (Date) this.pd.clone();
    }

    public synchronized void setPd(final Date pd) {
        this.pd = pd == null ? null : (Date) pd.clone();
    }

    public synchronized Double getAvp() {
        return this.avp;
    }

    public synchronized void setAvp(final Double avp) {
        this.avp = avp;
    }

    public synchronized Double getSm() {
        return this.sm;
    }

    public synchronized void setSm(final Double sm) {
        this.sm = sm;
    }

    public synchronized Double getSr() {
        return this.sr;
    }

    public synchronized void setSr(final Double sr) {
        this.sr = sr;
    }

    public synchronized Double getSl() {
        return this.sl;
    }

    public synchronized void setSl(final Double sl) {
        this.sl = sl;
    }

    public synchronized Double getSc() {
        return this.sc;
    }

    public synchronized void setSc(final Double sc) {
        this.sc = sc;
    }

    public synchronized Double getSv() {
        return this.sv;
    }

    public synchronized void setSv(final Double sv) {
        this.sv = sv;
    }
}
