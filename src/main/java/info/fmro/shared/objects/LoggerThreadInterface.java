package info.fmro.shared.objects;

@SuppressWarnings({"InterfaceNeverImplemented", "RedundantSuppression"})
@FunctionalInterface
public interface LoggerThreadInterface {
    void addLogEntry(final String messageFormat, final long timeDifference);
}
