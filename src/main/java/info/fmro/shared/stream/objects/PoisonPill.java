package info.fmro.shared.stream.objects;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serial;
import java.io.Serializable;

public class PoisonPill
        implements StreamObjectInterface, Serializable {
    @Serial
    private static final long serialVersionUID = -4370780628656306418L;

    public synchronized PoisonPill getCopy() {
        return SerializationUtils.clone(this);
    }
}
