package info.fmro.shared.logic;

public interface SafetyLimitsInterface {
    double getTotalLimit();

    double getDefaultMarketLimit(final String marketId);

    double getDefaultEventLimit(final String eventId);
}
