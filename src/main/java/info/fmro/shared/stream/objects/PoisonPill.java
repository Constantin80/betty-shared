package info.fmro.shared.stream.objects;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class PoisonPill
        implements StreamObjectInterface, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(PoisonPill.class);
    private static final long serialVersionUID = -4370780628656306418L;

    public synchronized PoisonPill getCopy() {
        return SerializationUtils.clone(this);
    }
}
