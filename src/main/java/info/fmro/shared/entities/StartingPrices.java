package info.fmro.shared.entities;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StartingPrices
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -2616193533595542784L;
    private Double nearPrice, farPrice;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private List<? extends PriceSize> backStakeTaken, layLiabilityTaken;
    private Double actualSP;

    @Contract(pure = true)
    public StartingPrices() {
    }

    @Contract(pure = true)
    public StartingPrices(final Double nearPrice, final Double farPrice, @NotNull final List<? extends PriceSize> backStakeTaken, @NotNull final List<? extends PriceSize> layLiabilityTaken, final Double actualSP) {
        this.nearPrice = nearPrice;
        this.farPrice = farPrice;
        this.backStakeTaken = new ArrayList<>(backStakeTaken);
        this.layLiabilityTaken = new ArrayList<>(layLiabilityTaken);
        this.actualSP = actualSP;
    }

    public Double getNearPrice() {
        return this.nearPrice;
    }

    public Double getFarPrice() {
        return this.farPrice;
    }

    @Nullable
    public synchronized List<PriceSize> getBackStakeTaken() {
        return this.backStakeTaken == null ? null : new ArrayList<>(this.backStakeTaken);
    }

    @Nullable
    public synchronized List<PriceSize> getLayLiabilityTaken() {
        return this.layLiabilityTaken == null ? null : new ArrayList<>(this.layLiabilityTaken);
    }

    public Double getActualSP() {
        return this.actualSP;
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
        final StartingPrices that = (StartingPrices) obj;
        return Objects.equals(this.nearPrice, that.nearPrice) &&
               Objects.equals(this.farPrice, that.farPrice) &&
               Objects.equals(this.backStakeTaken, that.backStakeTaken) &&
               Objects.equals(this.layLiabilityTaken, that.layLiabilityTaken) &&
               Objects.equals(this.actualSP, that.actualSP);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(this.nearPrice, this.farPrice, this.backStakeTaken, this.layLiabilityTaken, this.actualSP);
    }
}
