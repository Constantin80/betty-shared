package info.fmro.shared.stream.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Side { // BACK, LAY;
    B,
    L;

    private static final Logger logger = LoggerFactory.getLogger(Side.class);

    @Nullable
    public synchronized info.fmro.shared.enums.Side toStandardSide() {
        @Nullable final info.fmro.shared.enums.Side returnValue;
        if (this == B) {
            returnValue = info.fmro.shared.enums.Side.BACK;
        } else if (this == L) {
            returnValue = info.fmro.shared.enums.Side.LAY;
        } else {
            logger.error("strange unsupported value in toStandardSide: {}", this);
            returnValue = null;
        }

        return returnValue;
    }

    @NotNull
    public synchronized Side opposite() {
        @NotNull final Side returnValue;
        if (this == B) {
            returnValue = L;
        } else if (this == L) {
            returnValue = B;
        } else {
            logger.error("strange unsupported value in opposite: {}", this);
            returnValue = B; // this should never happen, I won't bother returning null
        }

        return returnValue;
    }
}
