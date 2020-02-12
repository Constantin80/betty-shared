package info.fmro.shared.stream.objects;

import info.fmro.shared.enums.MatchStatus;

import java.io.Serializable;

@SuppressWarnings("ClassWithTooManyMethods")
public interface ScraperEventInterface
        extends Serializable {
    long getIgnoredExpiration();

    long getTimeStamp();

    boolean isTooOldForMatching(long currentTime);

    String getMatchedEventId();

    long getEventId();

    String getHomeTeam();

    String getAwayTeam();

    MatchStatus getMatchStatus();

    int setMatchedEventId(String newMatchedEventId);

    long getMatchedTimeStamp();

    int setIgnored(long period);

    int getHomeScore();

    int getAwayScore();

    int getHomeHtScore();

    int getAwayHtScore();

    int getMinutesPlayed();

    int getStoppageTime();

    int getHomeRedCards();

    int getAwayRedCards();

    boolean isIgnored();

    long errors();

    boolean hasStarted();
}
