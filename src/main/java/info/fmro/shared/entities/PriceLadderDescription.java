package info.fmro.shared.entities;

import info.fmro.shared.enums.PriceLadderType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("unused")
class PriceLadderDescription
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -6000098777316431103L;
    @SuppressWarnings("unused")
    private PriceLadderType type; // The type of price ladder.

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PriceLadderDescription that = (PriceLadderDescription) obj;
        return this.type == that.type;
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(this.type);
    }
}
