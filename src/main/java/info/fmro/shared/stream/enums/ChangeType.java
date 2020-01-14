package info.fmro.shared.stream.enums;

// Common change type (as change type is local to market / order in swagger).
public enum ChangeType {
    // Update
    UPDATE,
    // Initial subscription image
    SUB_IMAGE,
    // Resubscription delta image
    RESUB_DELTA,
    // Heartbeat
    HEARTBEAT
}
