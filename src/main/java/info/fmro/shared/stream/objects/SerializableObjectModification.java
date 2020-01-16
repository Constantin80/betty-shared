package info.fmro.shared.stream.objects;

import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class SerializableObjectModification<T extends Enum<T>>
        implements StreamObjectInterface, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(SerializableObjectModification.class);
    private static final long serialVersionUID = 140177397632319612L;
    private final T command;
    @Nullable
    private final Serializable[] objectsToModify;

    public SerializableObjectModification(final T command, final Serializable... objectsToModify) {
        this.command = command;
        if (objectsToModify == null) {
            logger.error("null objectsToModify in SerializableObjectModification constructor for: {} {}", command == null ? null : command.getClass(), command);
            this.objectsToModify = null;
        } else {
            final int size = objectsToModify.length;
            this.objectsToModify = new Serializable[size];
            for (int i = 0; i < size; i++) {
                this.objectsToModify[i] = SerializationUtils.clone(objectsToModify[i]);
            }
        }
    }

    @Nullable
    public synchronized Serializable[] getArray() { // right now used for testing
        @Nullable final Serializable[] returnValue;
        if (this.objectsToModify == null) {
            returnValue = null;
        } else {
            final int size = this.objectsToModify.length;
            returnValue = new Serializable[size];
            System.arraycopy(this.objectsToModify, 0, returnValue, 0, size);
        }

        return returnValue;
    }

    public synchronized SerializableObjectModification<T> getCopy() {
        return SerializationUtils.clone(this);
    }

    public synchronized int runAfterReceive() {
        return 0;
    }

    public synchronized void runBeforeSend() {
    }
}