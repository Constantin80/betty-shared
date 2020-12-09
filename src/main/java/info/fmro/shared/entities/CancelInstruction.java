package info.fmro.shared.entities;

import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.Nullable;

public class CancelInstruction {
    private String betId;
    @Nullable
    private Double sizeReduction;

    public synchronized String getBetId() {
        return this.betId;
    }

    public synchronized void setBetId(final String betId) {
        this.betId = betId;
    }

    @Nullable
    public synchronized Double getSizeReduction() {
        return this.sizeReduction;
    }

    public synchronized void setSizeReduction(@Nullable final Double sizeReduction) {
        this.sizeReduction = sizeReduction == null ? null : Generic.roundDoubleAmount(sizeReduction);
    }
}
