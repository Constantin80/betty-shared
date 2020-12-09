package info.fmro.shared.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;

public class HighestLongContainer
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(HighestLongContainer.class);
    @Serial
    private static final long serialVersionUID = 4504552178441704439L;
    private long value;

    public synchronized boolean add(final Long newValue) {
        final boolean modified;
        if (newValue == null) {
            logger.error("null newValue in HighestLongContainer.add, existing value: {}", this.value);
            modified = false;
        } else {
            if (newValue > this.value) {
                this.value = newValue;
                modified = true;
            } else {
                modified = false;
            }
        }
        return modified;
    }

    public synchronized boolean remove(final Long highValue) {
        final boolean modified;
        if (highValue == null) {
            logger.error("null highValue in HighestLongContainer.remove, existing value: {}", this.value);
            modified = false;
        } else {
            if (highValue >= this.value) {
                this.value = 0L;
                modified = true;
            } else {
                modified = false;
            }
        }
        return modified;
    }

    public synchronized long get() {
        return this.value;
    }
}
