package info.fmro.shared.enums;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Side {
    BACK,
    LAY;

    private static final Logger logger = LoggerFactory.getLogger(Side.class);

    @Nullable
    public synchronized info.fmro.shared.stream.enums.Side toStreamSide() {
        @Nullable final info.fmro.shared.stream.enums.Side returnValue;
        if (this == BACK) {
            returnValue = info.fmro.shared.stream.enums.Side.B;
        } else if (this == LAY) {
            returnValue = info.fmro.shared.stream.enums.Side.L;
        } else {
            logger.error("strange unsupported value in toStreamSide: {}", this);
            returnValue = null;
        }

        return returnValue;
    }
}
