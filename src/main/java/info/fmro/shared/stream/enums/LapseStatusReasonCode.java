package info.fmro.shared.stream.enums;

// This field will now be present in some cases on the Order object of the Order Stream to denote the reason that some or all of the order is lapsed.
// It will be null if no portion of the order is lapsed or if the order lapsed for some reason other than those listed below.
public enum LapseStatusReasonCode {
    MKT_UNKNOWN, // The market was unknown, presumably removed from the matcher (closed) between bet placement and matching.
    MKT_INVALID, // The market was known about, but in an invalid state.
    RNR_UNKNOWN, // The runner was unknown, presumably removed between bet placement and matching.
    TIME_ELAPSED, // The bet was waiting in the queue too long, so was lapsed for safety.
    CURRENCY_UNKNOWN, // The bet's currency ID was not recognised by the matcher.
    PRICE_INVALID, // The bet's price was invalid, e.g. outside the defined ladder for the market.
    MKT_SUSPENDED, // The market was suspended at the time the bet came to be matched.
    MKT_VERSION, // The bet had a maximum market version set, and the market's version on matching was greater than this.
    LINE_TARGET, // The bet was on a line market, but was requested targeting profit or payout.
    LINE_SP, // The bet was on a line market, but was either a BSP bet directly, or requested to PERSIST_TO_SP.
    SP_IN_PLAY, // The bet was a BSP bet that had somehow come to be placed after turn-in-play.
    SMALL_STAKE, // The bet's stake was worth less than half a penny in GBP.
    PRICE_IMP_TOO_LARGE // When the bet came to be matched, the price available was better than its best permitted price, suggesting a significant shift in the market, presumably due to a major incident, which may have rendered the bet unwanted.
}
