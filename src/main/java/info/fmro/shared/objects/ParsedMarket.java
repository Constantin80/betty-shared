package info.fmro.shared.objects;

import info.fmro.shared.enums.ParsedMarketType;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;

public class ParsedMarket
        implements Serializable, Comparable<ParsedMarket> {
    public static final int BEFORE = -1, EQUAL = 0, AFTER = 1;
    @Serial
    private static final long serialVersionUID = -2616558897137266131L;
    private final String marketId;
    private final ParsedMarketType parsedMarketType;
    private final HashSet<ParsedRunner> parsedRunnersSet; // the only place where ParsedRunners are stored

    public ParsedMarket(final String marketId, final ParsedMarketType parsedMarketType, final HashSet<ParsedRunner> parsedRunnersSet) {
        this.marketId = marketId;
        this.parsedMarketType = parsedMarketType;
        this.parsedRunnersSet = new HashSet<>(parsedRunnersSet);
    }

    public String getMarketId() {
        return this.marketId;
    }

    public ParsedMarketType getParsedMarketType() {
        return this.parsedMarketType;
    }

    public synchronized HashSet<ParsedRunner> getParsedRunnersSet() {
        return new HashSet<>(this.parsedRunnersSet);
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public int compareTo(@NotNull final ParsedMarket o) {
        //noinspection ConstantConditions
        if (o == null) {
            return AFTER;
        }
        if (this == o) {
            return EQUAL;
        }

        if (this.getClass() != o.getClass()) {
            return this.getClass().hashCode() < o.getClass().hashCode() ? BEFORE : AFTER;
        }
        if (!Objects.equals(this.marketId, o.marketId)) {
            if (this.marketId == null) {
                return BEFORE;
            }
            if (o.marketId == null) {
                return AFTER;
            }
            return this.marketId.compareTo(o.marketId);
        }
        if (this.parsedMarketType != o.parsedMarketType) {
            if (this.parsedMarketType == null) {
                return BEFORE;
            }
            if (o.parsedMarketType == null) {
                return AFTER;
            }
            return this.parsedMarketType.compareTo(o.parsedMarketType);
        }
        if (!Objects.equals(this.parsedRunnersSet, o.parsedRunnersSet)) {
            //noinspection ConstantConditions
            if (this.parsedRunnersSet == null) {
                return BEFORE;
            }
            //noinspection ConstantConditions
            if (o.parsedRunnersSet == null) {
                return AFTER;
            }

            return BEFORE; // very primitive, hopefully it won't cause bugs; implementing compareTo for sets is not easy and it would likely consume too much resources
        }

        return EQUAL;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ParsedMarket that = (ParsedMarket) obj;
        return Objects.equals(this.marketId, that.marketId) &&
               this.parsedMarketType == that.parsedMarketType &&
               Objects.equals(this.parsedRunnersSet, that.parsedRunnersSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.marketId, this.parsedMarketType, this.parsedRunnersSet);
    }
}
