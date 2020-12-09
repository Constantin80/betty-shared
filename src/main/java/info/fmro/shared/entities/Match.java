package info.fmro.shared.entities;

import info.fmro.shared.enums.Side;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@SuppressWarnings("unused")
class Match
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 5267424955233956792L;
    private String betId;
    private String matchId;
    private Side side;
    private Double price;
    private Double size;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private Date matchDate;

//    @SuppressWarnings("unused")
//    @Contract(pure = true)
//    private Match() {
//    }
//
//    @SuppressWarnings({"ConstructorWithTooManyParameters", "unused"})
//    @Contract(pure = true)
//    Match(final String betId, final String matchId, final Side side, final Double price, final Double size, @NotNull final Date matchDate) {
//        this.betId = betId;
//        this.matchId = matchId;
//        this.side = side;
//        this.price = price;
//        this.size = size;
//        this.matchDate = (Date) matchDate.clone();
//    }

    public String getBetId() {
        return this.betId;
    }

    public String getMatchId() {
        return this.matchId;
    }

    public Side getSide() {
        return this.side;
    }

    public Double getPrice() {
        return this.price;
    }

    public Double getSize() {
        return this.size;
    }

    @Nullable
    public synchronized Date getMatchDate() {
        return this.matchDate == null ? null : (Date) this.matchDate.clone();
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Match match = (Match) obj;
        return Objects.equals(this.betId, match.betId) &&
               Objects.equals(this.matchId, match.matchId) &&
               this.side == match.side &&
               Objects.equals(this.price, match.price) &&
               Objects.equals(this.size, match.size) &&
               Objects.equals(this.matchDate, match.matchDate);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return Objects.hash(this.betId, this.matchId, this.side, this.price, this.size, this.matchDate);
    }
}
