package info.fmro.shared.stream.definitions;

import java.io.Serial;
import java.io.Serializable;

public class HeartbeatMessage
        extends RequestMessage
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -4123532806201471490L;
}
