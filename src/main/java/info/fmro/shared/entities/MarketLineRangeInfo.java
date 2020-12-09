package info.fmro.shared.entities;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("unused")
class MarketLineRangeInfo
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -4018323838006232985L;
    private Double maxUnitValue;
    private Double minUnitValue;
    private Double interval;
    private String marketUnit;

    public Double getMaxUnitValue() {
        return this.maxUnitValue;
    }

    public Double getMinUnitValue() {
        return this.minUnitValue;
    }

    public Double getInterval() {
        return this.interval;
    }

    public String getMarketUnit() {
        return this.marketUnit;
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
        final MarketLineRangeInfo that = (MarketLineRangeInfo) obj;
        return Objects.equals(this.maxUnitValue, that.maxUnitValue) &&
               Objects.equals(this.minUnitValue, that.minUnitValue) &&
               Objects.equals(this.interval, that.interval) &&
               Objects.equals(this.marketUnit, that.marketUnit);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(this.maxUnitValue, this.minUnitValue, this.interval, this.marketUnit);
    }
}
