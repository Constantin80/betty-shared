package info.fmro.shared.entities;

import java.io.Serial;
import java.io.Serializable;

public class MarketOnCloseOrder
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -8981250957935689408L;
    private Double liability;

    synchronized Double getLiability() {
        return this.liability;
    }

    public synchronized void setLiability(final Double liability) {
        this.liability = liability;
    }
}
