package info.fmro.shared.entities;

import org.jetbrains.annotations.Contract;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class RunnerId
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -8081544562430144958L;
    @SuppressWarnings("unused")
    private String marketId; // The id of the market bet on
    @SuppressWarnings("unused")
    private Long selectionId; // The id of the selection bet on
    @SuppressWarnings("unused")
    private Double handicap; // The handicap associated with the runner in case of asian handicap markets, otherwise returns '0.0'.

    public String getMarketId() {
        return this.marketId;
    }

    public Long getSelectionId() {
        return this.selectionId;
    }

    public Double getHandicap() {
        return this.handicap;
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RunnerId runnerId = (RunnerId) obj;
        return Objects.equals(this.marketId, runnerId.marketId) &&
               Objects.equals(this.selectionId, runnerId.selectionId) &&
               Objects.equals(this.handicap, runnerId.handicap);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(this.marketId, this.selectionId, this.handicap);
    }

    @Override
    public String toString() {
        return "RunnerId{marketId=" + this.marketId + ", selectionId=" + this.selectionId + ", handicap=" + this.handicap + '}';
    }
}
