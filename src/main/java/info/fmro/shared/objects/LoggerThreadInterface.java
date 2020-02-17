package info.fmro.shared.objects;

@FunctionalInterface
public interface LoggerThreadInterface {
    void addLogEntry(final String messageFormat, final long timeDifference);
}
