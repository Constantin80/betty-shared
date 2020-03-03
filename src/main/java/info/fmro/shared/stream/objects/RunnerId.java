package info.fmro.shared.stream.objects;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

public class RunnerId
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(RunnerId.class);
    private static final long serialVersionUID = -4753325449542276275L;
    private final Long selectionId; // the id of the runner
    private Double handicap; // the handicap of the runner (null if not applicable)

    public RunnerId(final Long selectionId, final Double handicap) {
        if (selectionId == null) {
            logger.error("null selectionId when creating RunnerId: {} {}", selectionId, handicap);
        } else { // no error message, constructor continues normally
        }
        this.selectionId = selectionId;
        this.handicap = handicap == null ? 0d : handicap;
    }

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.handicap == null) {
            this.handicap = 0d;
        }
    }

    public synchronized Long getSelectionId() {
        return this.selectionId;
    }

    public synchronized Double getHandicap() {
        return this.handicap;
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Contract(value = "null -> false", pure = true)
    @Override
    public synchronized boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RunnerId runnerId = (RunnerId) obj;
        if (this.handicap == null) {
            this.handicap = 0d;
        }
        if (runnerId.handicap == null) {
            runnerId.handicap = 0d;
        }
        return Objects.equals(this.selectionId, runnerId.selectionId) &&
               Objects.equals(this.handicap, runnerId.handicap);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public synchronized int hashCode() {
        if (this.handicap == null) {
            this.handicap = 0d;
        }
        return Objects.hash(this.selectionId, this.handicap);
    }

    @Override
    public synchronized String toString() {
        return "RunnerId{" +
               "selectionId=" + this.selectionId +
               ", handicap=" + this.handicap +
               '}';
    }
}
