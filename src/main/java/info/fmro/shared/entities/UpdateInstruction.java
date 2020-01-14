package info.fmro.shared.entities;

import info.fmro.shared.enums.PersistenceType;

public class UpdateInstruction {
    private String betId;
    private PersistenceType newPersistenceType;

    public synchronized String getBetId() {
        return this.betId;
    }

    public synchronized void setBetId(final String betId) {
        this.betId = betId;
    }

    public synchronized PersistenceType getNewPersistenceType() {
        return this.newPersistenceType;
    }

    public synchronized void setNewPersistenceType(final PersistenceType newPersistenceType) {
        this.newPersistenceType = newPersistenceType;
    }
}
