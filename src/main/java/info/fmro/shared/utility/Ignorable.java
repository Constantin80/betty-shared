package info.fmro.shared.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class Ignorable
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Ignorable.class);
    public static final long RECENT_PERIOD = 1_000L;
    private static final long serialVersionUID = 1721002836563166854L;
    private long ignoredExpiration;
    private boolean ignored;
    private long setIgnoredStamp, resetIgnoredStamp;

    public Ignorable() {
    }

//    public Ignorable(long ignoredExpiration) {
//        this.setIgnoredExpiration(ignoredExpiration);
//    }

    public synchronized long timeSinceSetIgnored() {
        final long currentTime = System.currentTimeMillis();
        return timeSinceSetIgnored(currentTime);
    }

    public synchronized long timeSinceSetIgnored(long currentTime) {
        return currentTime - this.setIgnoredStamp;
    }

    public synchronized long timeSinceResetIgnored() {
        final long currentTime = System.currentTimeMillis();
        return timeSinceResetIgnored(currentTime);
    }

    public synchronized long timeSinceResetIgnored(long currentTime) {
        return currentTime - this.resetIgnoredStamp;
    }

    public synchronized long getSetIgnoredStamp() {
        return setIgnoredStamp;
    }

    private synchronized int setSetIgnoredStamp(long stamp) { // private method
        final int modified;
        if (stamp > this.setIgnoredStamp) {
            this.setIgnoredStamp = stamp;
            modified = 1;
        } else {
            modified = 0;
        }

        return modified;
    }

    public synchronized boolean isSetIgnoredRecent() {
        final long currentTime = System.currentTimeMillis();
        return isSetIgnoredRecent(currentTime, Ignorable.RECENT_PERIOD);
    }

    public synchronized boolean isSetIgnoredRecent(long currentTime) {
        return isSetIgnoredRecent(currentTime, Ignorable.RECENT_PERIOD);
    }

    public synchronized boolean isSetIgnoredRecent(long currentTime, long recentPeriod) {
        return this.setIgnoredStamp + recentPeriod >= currentTime;
    }

    public synchronized long getResetIgnoredStamp() {
        return resetIgnoredStamp;
    }

    private synchronized int setResetIgnoredStamp(long stamp) { // private method
        final int modified;
        if (stamp > this.resetIgnoredStamp) {
            this.resetIgnoredStamp = stamp;
            modified = 1;
        } else {
            modified = 0;
        }

        return modified;
    }

    public synchronized boolean isResetIgnoredRecent() {
        final long currentTime = System.currentTimeMillis();
        return isResetIgnoredRecent(currentTime, Ignorable.RECENT_PERIOD);
    }

    public synchronized boolean isResetIgnoredRecent(long currentTime) {
        return isResetIgnoredRecent(currentTime, Ignorable.RECENT_PERIOD);
    }

    public synchronized boolean isResetIgnoredRecent(long currentTime, long recentPeriod) {
        return this.resetIgnoredStamp + recentPeriod >= currentTime;
    }

    public synchronized long getIgnoredExpiration() {
        return ignoredExpiration;
    }

    private synchronized int setIgnoredExpiration(long ignoredExpiration) { // private method; from outside setIgnored is used; new value will be set only if larger
        final int modified;

        if (ignoredExpiration > this.ignoredExpiration) {
            this.ignoredExpiration = ignoredExpiration;

            final long currentTime = System.currentTimeMillis();
            if (this.ignoredExpiration > currentTime) { // normal behaviour
                Generic.alreadyPrintedMap.logOnce(1L, logger, LogLevel.INFO, AlreadyPrintedMap.NOT_IMPORTANT_PREFIX + "ignored set: {}", this.ignoredExpiration);
//                logger.info("ignored set: {}", this.ignoredExpiration);
                this.ignored = true;
            } else {
                logger.error("new ignoredExpiration {} not higher than currentTime {}", this.ignoredExpiration, currentTime);
            }
            this.setSetIgnoredStamp(currentTime);
            modified = 1;
        } else if (ignoredExpiration == this.ignoredExpiration) {
            modified = 0;
        } else {
            if (ignoredExpiration != 0) { // will not set new ignoredExpiration smaller than previous value
//                if (Statics.debugLevel.check(2, 201)) {
//                    logger.info("will not set new ignoredExpiration {} smaller than previous value {}", ignoredExpiration, this.ignoredExpiration);
//                }
            } else { // ignoredExpiration == 0
                // nothing to be done, this might be normal during object updates
            }
            modified = 0;
        }

        return modified;
    }

    public synchronized boolean isIgnored() {
        if (!this.ignored) { // result will be false, nothing to be done
        } else { // this.ignored == true
            final long currentTime = System.currentTimeMillis();

            if (this.ignoredExpiration <= currentTime) {
                this.ignored = false;
//            logger.info("ignored expired: {}", this.ignoredExpiration);
                this.setResetIgnoredStamp(currentTime);
            } else { // result is true, nothing to be done
            }
        }

        return this.ignored;
    }

    public synchronized boolean isIgnored(long argumentTime) {
        final boolean result;
        if (this.ignoredExpiration <= argumentTime) {
            result = false;
        } else {
            result = true;
        }
        if (this.ignored && !result) { // ignored is true, but result is false; let's check if ignored needs reset (without this check, ignored will never get reset by this method)
            this.isIgnored();
        }

        return result;
    }

    public synchronized int setIgnored(long period) {
        final long currentTime = System.currentTimeMillis();
        logger.error("method Ignorable.setIgnored(period) should be overridden, not called directly: {}", this.getClass());

        return setIgnored(period, currentTime);
    }

    public synchronized int setIgnored(long period, long startTime) {
        final int modified;

        if (period > Generic.DAY_LENGTH_MILLISECONDS * 10_000L) {
            logger.error("trying to set too big period for ignored: {}", period);
            modified = -10000;
        } else {
            final long expiration = startTime + period;
            modified = this.setIgnoredExpiration(expiration);
        }

        return modified;
    }

    public synchronized int updateIgnorable(Ignorable ignorable) {
        int modified;
        if (this == ignorable) {
            logger.error("update from same object in Ignorable.updateIgnorable: {}", Generic.objectToString(this));
            modified = 0;
        } else {
            modified = 0; // initialized

            modified += this.setIgnoredExpiration(ignorable.getIgnoredExpiration()); // this should update ignored as well

            // stamps can only increase
            modified += this.setSetIgnoredStamp(ignorable.getSetIgnoredStamp());
            modified += this.setResetIgnoredStamp(ignorable.getResetIgnoredStamp());
        }
        return modified;
    }
}
