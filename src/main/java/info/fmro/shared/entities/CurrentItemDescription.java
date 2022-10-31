package info.fmro.shared.entities;

import java.io.Serial;
import java.io.Serializable;

public class CurrentItemDescription
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 9174908558736377196L;
    private MarketVersion marketVersion; // The relevant version of the market for this item

    public synchronized MarketVersion getMarketVersion() {
        return this.marketVersion;
    }

    public synchronized void setMarketVersion(final MarketVersion marketVersion) {
        this.marketVersion = marketVersion;
    }
}
