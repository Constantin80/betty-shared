package info.fmro.shared.stream.objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class RunnerId
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(RunnerId.class);
    @Serial
    private static final long serialVersionUID = -4753325449542276275L;
    private final Long selectionId; // the id of the runner
    private Double handicap; // the handicap of the runner (null if not applicable)

    @JsonCreator
    public RunnerId(@JsonProperty("selectionId") final Long selectionId, @JsonProperty("handicap") final Double handicap) {
        if (selectionId == null) {
            logger.error("null selectionId when creating RunnerId: {} {}", selectionId, handicap);
        } else { // no error message, constructor continues normally
        }
        this.selectionId = selectionId;
        this.handicap = handicap == null ? 0d : handicap;
    }

    @Serial
    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (this.handicap == null) {
            this.handicap = 0d;
        }
    }

    public Long getSelectionId() {
        return this.selectionId;
    }

    public Double getHandicap() {
        return this.handicap;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RunnerId runnerId = (RunnerId) obj;
        //noinspection NonFinalFieldReferenceInEquals
        return Objects.equals(this.selectionId, runnerId.selectionId) &&
               Objects.equals(this.handicap, runnerId.handicap);
    }

    @Override
    public int hashCode() {
        //noinspection NonFinalFieldReferencedInHashCode
        return Objects.hash(this.selectionId, this.handicap);
    }

    @Override
    public String toString() {
        return "RunnerId{selectionId=" + this.selectionId + ", handicap=" + this.handicap + '}';
    }
}
