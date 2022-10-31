package info.fmro.shared.stream.cache;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record RunnerOrderModificationsList(@Nullable List<RunnerOrderModification> modificationsList, double worstOddsWhereIHaveKnowledge)
        implements Serializable {

    @Contract(pure = true)
    public RunnerOrderModificationsList(final List<RunnerOrderModification> modificationsList, final double worstOddsWhereIHaveKnowledge) {
        this.modificationsList = modificationsList == null ? null : new ArrayList<>(modificationsList);
        this.worstOddsWhereIHaveKnowledge = worstOddsWhereIHaveKnowledge;
    }

    @Override
    @Nullable
    public List<RunnerOrderModification> modificationsList() {
        return this.modificationsList == null ? null : new ArrayList<>(this.modificationsList);
    }
}
