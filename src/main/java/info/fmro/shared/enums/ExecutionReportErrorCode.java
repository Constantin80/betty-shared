package info.fmro.shared.enums;

import org.jetbrains.annotations.Contract;

public enum ExecutionReportErrorCode {
    ERROR_IN_MATCHER("The matcher's not healthy"),
    PROCESSED_WITH_ERRORS("The order itself has been accepted, but at least one (possibly all) actions have generated errors"),
    BET_ACTION_ERROR("There is an error with an action that has caused the entire order to be rejected"),
    INVALID_ACCOUNT_STATE("Order rejected due to the account's status (suspended, inactive, dup cards)"),
    INVALID_WALLET_STATUS("Order rejected due to the account's wallet's status"),
    INSUFFICIENT_FUNDS("Account has exceeded its exposure limit or available to bet limit"),
    LOSS_LIMIT_EXCEEDED("The account has exceed the self imposed loss limit"),
    MARKET_SUSPENDED("Market is suspended"),
    MARKET_NOT_OPEN_FOR_BETTING("Market is not open for betting, either inactive, suspended or closed"),
    DUPLICATE_TRANSACTION("duplicate customer reference data submitted"),
    INVALID_ORDER("Order cannot be accepted by the matcher due to the combination of actions. For example, bets being edited are not on the same market, or order includes both edits and placement"),
    INVALID_MARKET_ID("Market doesn't exist"),
    PERMISSION_DENIED("Business rules do not allow order to be placed"),
    DUPLICATE_BETIDS("Duplicate bet ids found"),
    NO_ACTION_REQUIRED("Order hasn't been passed to matcher as system detected there will be no state change"),
    SERVICE_UNAVAILABLE("The requested service is unavailable"),
    REJECTED_BY_REGULATOR("The regulator rejected the order"),
    NO_CHASING("Bet placed contravenes the Spanish regulatory rules relating to loss chasing"), // A specific error code that relates to Spanish Exchange markets only
    REGULATOR_IS_NOT_AVAILABLE("The underlying regulator service is not available."),
    TOO_MANY_INSTRUCTIONS("The amount of orders exceeded the maximum amount allowed to be executed"),
    INVALID_MARKET_VERSION("The supplied market version is invalid. Max length allowed for market version is 12.");

    private final String message;

    @Contract(pure = true)
    ExecutionReportErrorCode(final String message) {
        this.message = message;
    }

    @Contract(pure = true)
    public synchronized String getMessage() {
        return this.message;
    }
}
