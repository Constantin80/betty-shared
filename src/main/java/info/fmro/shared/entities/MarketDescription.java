package info.fmro.shared.entities;

import info.fmro.shared.enums.MarketBettingType;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@SuppressWarnings({"unused", "FieldAccessedSynchronizedAndUnsynchronized"})
public class MarketDescription
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -2877536783333417777L;
    private Boolean persistenceEnabled;
    private Boolean bspMarket;
    private Date marketTime;
    private Date suspendTime;
    private Date settleTime;
    private MarketBettingType bettingType;
    private Boolean turnInPlayEnabled;
    private String marketType;
    private String regulator;
    private Double marketBaseRate;
    private Boolean discountAllowed;
    private String wallet;
    private String rules;
    private Boolean rulesHasDate;
    private Double eachWayDivisor; // The divisor is returned for the marketType EACH_WAY only
    private String clarifications;
    private MarketLineRangeInfo lineRangeInfo; // Line range info for line markets
    private String raceType; // An external identifier of a race type
    private PriceLadderDescription priceLadderDescription; // Details about the price ladder in use for this market.

//    @Contract(pure = true)
//    public MarketDescription() {
//    }

//    @SuppressWarnings("ConstructorWithTooManyParameters")
//    @Contract(pure = true)
//    public MarketDescription(final Boolean persistenceEnabled, final Boolean bspMarket, @NotNull final Date marketTime, @NotNull final Date suspendTime, @NotNull final Date settleTime, final MarketBettingType bettingType, final Boolean turnInPlayEnabled,
//                             final String marketType, final String regulator, final Double marketBaseRate, final Boolean discountAllowed, final String wallet, final String rules, final Boolean rulesHasDate, final String clarifications,
//                             final MarketLineRangeInfo lineRangeInfo) {
//        this.persistenceEnabled = persistenceEnabled;
//        this.bspMarket = bspMarket;
//        this.marketTime = (Date) marketTime.clone();
//        this.suspendTime = (Date) suspendTime.clone();
//        this.settleTime = (Date) settleTime.clone();
//        this.bettingType = bettingType;
//        this.turnInPlayEnabled = turnInPlayEnabled;
//        this.marketType = marketType;
//        this.regulator = regulator;
//        this.marketBaseRate = marketBaseRate;
//        this.discountAllowed = discountAllowed;
//        this.wallet = wallet;
//        this.rules = rules;
//        this.rulesHasDate = rulesHasDate;
//        this.clarifications = clarifications;
//        this.lineRangeInfo = lineRangeInfo;
//    }

    public synchronized Boolean getPersistenceEnabled() {
        return this.persistenceEnabled;
    }

    public synchronized Boolean getBspMarket() {
        return this.bspMarket;
    }

    @Nullable
    public synchronized Date getMarketTime() {
        return this.marketTime == null ? null : (Date) this.marketTime.clone();
    }

    @Nullable
    public synchronized Date getSuspendTime() {
        return this.suspendTime == null ? null : (Date) this.suspendTime.clone();
    }

    @Nullable
    public synchronized Date getSettleTime() {
        return this.settleTime == null ? null : (Date) this.settleTime.clone();
    }

    public synchronized MarketBettingType getBettingType() {
        return this.bettingType;
    }

    public synchronized Boolean getTurnInPlayEnabled() {
        return this.turnInPlayEnabled;
    }

    public synchronized String getMarketType() {
        return this.marketType;
    }

    public synchronized String getRegulator() {
        return this.regulator;
    }

    public synchronized Double getMarketBaseRate() {
        return this.marketBaseRate;
    }

    public synchronized Boolean getDiscountAllowed() {
        return this.discountAllowed;
    }

    public synchronized String getWallet() {
        return this.wallet;
    }

    public synchronized String getRules() {
        return this.rules;
    }

    public synchronized Boolean getRulesHasDate() {
        return this.rulesHasDate;
    }

    public synchronized Double getEachWayDivisor() {
        return this.eachWayDivisor;
    }

    public synchronized String getClarifications() {
        return this.clarifications;
    }

    public synchronized MarketLineRangeInfo getLineRangeInfo() {
        return this.lineRangeInfo;
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
        final MarketDescription that = (MarketDescription) obj;
        return Objects.equals(this.persistenceEnabled, that.persistenceEnabled) &&
               Objects.equals(this.bspMarket, that.bspMarket) &&
               Objects.equals(this.marketTime, that.marketTime) &&
               Objects.equals(this.suspendTime, that.suspendTime) &&
               Objects.equals(this.settleTime, that.settleTime) &&
               this.bettingType == that.bettingType &&
               Objects.equals(this.turnInPlayEnabled, that.turnInPlayEnabled) &&
               Objects.equals(this.marketType, that.marketType) &&
               Objects.equals(this.regulator, that.regulator) &&
               Objects.equals(this.marketBaseRate, that.marketBaseRate) &&
               Objects.equals(this.discountAllowed, that.discountAllowed) &&
               Objects.equals(this.wallet, that.wallet) &&
               Objects.equals(this.rulesHasDate, that.rulesHasDate) &&
               Objects.equals(this.eachWayDivisor, that.eachWayDivisor) &&
               Objects.equals(this.lineRangeInfo, that.lineRangeInfo) &&
               Objects.equals(this.raceType, that.raceType) &&
               Objects.equals(this.priceLadderDescription, that.priceLadderDescription);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(this.persistenceEnabled, this.bspMarket, this.marketTime, this.suspendTime, this.settleTime, this.bettingType, this.turnInPlayEnabled, this.marketType, this.regulator, this.marketBaseRate, this.discountAllowed, this.wallet,
                            this.rulesHasDate, this.eachWayDivisor, this.lineRangeInfo, this.raceType, this.priceLadderDescription);
    }
}
