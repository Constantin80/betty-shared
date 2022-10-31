package info.fmro.shared.stream.definitions;

import info.fmro.shared.enums.PriceLadderType;

import java.io.Serial;
import java.io.Serializable;

// objects of this class are read from the stream
public class PriceLadderDefinition
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 6910728478048295440L;
    @SuppressWarnings("unused")
    private PriceLadderType type;

    public synchronized PriceLadderType getType() {
        return this.type;
    }
}
