package info.fmro.shared.entities;

import info.fmro.shared.stream.objects.RunnerId;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RunnerCatalog
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 8076707042221620993L;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private Long selectionId;
    private String runnerName;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private Double handicap;
    private Integer sortPriority;
    private Map<String, String> metadata;

    @Contract(pure = true)
    public RunnerCatalog() {
    }

    @Contract(pure = true)
    public RunnerCatalog(final Long selectionId, final String runnerName, final Double handicap, final Integer sortPriority, @NotNull final Map<String, String> metadata) {
        this.selectionId = selectionId;
        this.runnerName = runnerName;
        this.handicap = handicap;
        this.sortPriority = sortPriority;
        this.metadata = new HashMap<>(metadata);
    }

    public Long getSelectionId() {
        return this.selectionId;
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public synchronized String getRunnerName() {
        return this.runnerName;
    }

    public Double getHandicap() {
        return this.handicap;
    }

    public synchronized Integer getSortPriority() {
        return this.sortPriority;
    }

    @Nullable
    public synchronized Map<String, String> getMetadata() {
        return this.metadata == null ? null : new HashMap<>(this.metadata);
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    synchronized boolean runnerIdEquals(final RunnerId runnerId) {
        if (runnerId == null) {
            return false;
        }
        return Objects.equals(this.selectionId, runnerId.getSelectionId()) && Objects.equals(this.handicap, runnerId.getHandicap());
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final RunnerCatalog that = (RunnerCatalog) obj;
        return Objects.equals(this.selectionId, that.selectionId) &&
               Objects.equals(this.handicap, that.handicap);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(this.selectionId, this.handicap);
    }
}
