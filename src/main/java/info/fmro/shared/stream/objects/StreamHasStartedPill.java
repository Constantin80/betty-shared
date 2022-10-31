package info.fmro.shared.stream.objects;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serial;
import java.io.Serializable;

public class StreamHasStartedPill
        implements StreamObjectInterface, Serializable {
    @Serial
    private static final long serialVersionUID = -7919480629258219129L;

    public synchronized StreamHasStartedPill getCopy() {
        return SerializationUtils.clone(this);
    }
}
