package info.fmro.shared.entities;

import info.fmro.shared.enums.InstructionReportErrorCode;
import info.fmro.shared.enums.InstructionReportStatus;

class UpdateInstructionReport {
    private InstructionReportStatus status;
    private InstructionReportErrorCode errorCode;
    private UpdateInstruction instruction;

    public synchronized InstructionReportStatus getStatus() {
        return this.status;
    }

    public synchronized void setStatus(final InstructionReportStatus status) {
        this.status = status;
    }

    public synchronized InstructionReportErrorCode getErrorCode() {
        return this.errorCode;
    }

    public synchronized void setErrorCode(final InstructionReportErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public synchronized UpdateInstruction getInstruction() {
        return this.instruction;
    }

    public synchronized void setInstruction(final UpdateInstruction instruction) {
        this.instruction = instruction;
    }
}
