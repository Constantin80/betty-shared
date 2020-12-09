package info.fmro.shared.entities;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExchangePrices
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -5031011635000589001L;
    private final ArrayList<PriceSize> availableToBack;
    private final ArrayList<PriceSize> availableToLay;
    private final ArrayList<PriceSize> tradedVolume;

    public ExchangePrices(@NotNull final List<? extends PriceSize> availableToBack, @NotNull final List<? extends PriceSize> availableToLay, @NotNull final List<? extends PriceSize> tradedVolume) {
        this.availableToBack = new ArrayList<>(availableToBack);
        this.availableToLay = new ArrayList<>(availableToLay);
        this.tradedVolume = new ArrayList<>(tradedVolume);
    }

    public synchronized List<PriceSize> getAvailableToBack() {
        return new ArrayList<>(this.availableToBack);
    }

    public synchronized List<PriceSize> getAvailableToLay() {
        return new ArrayList<>(this.availableToLay);
    }

    public synchronized List<PriceSize> getTradedVolume() {
        return new ArrayList<>(this.tradedVolume);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ExchangePrices that = (ExchangePrices) obj;
        return Objects.equals(this.availableToBack, that.availableToBack) &&
               Objects.equals(this.availableToLay, that.availableToLay) &&
               Objects.equals(this.tradedVolume, that.tradedVolume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.availableToBack, this.availableToLay, this.tradedVolume);
    }
}
