package info.fmro.shared.entities;

import java.io.Serial;
import java.io.Serializable;

public class MarketVersion
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 316671092641368253L;
    private Long version; // A non-monotonically increasing number indicating market changes

    public synchronized Long getVersion() {
        return this.version;
    }

    public synchronized void setVersion(final Long version) {
        this.version = version;
    }
}
