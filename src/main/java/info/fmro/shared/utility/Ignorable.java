package info.fmro.shared.utility;

//import info.fmro.betty.objects.Statics;
import java.io.Serializable;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ignorable
        implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Ignorable.class);
//    public static final long TEMP_REMOVED = Long.MAX_VALUE - 1001L;
    public static final long RECENT_PERIOD = 1_000L;
    public static final IgnorableDatabase database = new IgnorableDatabase();
    private static final long serialVersionUID = 1721002836563166854L;
    private long ignoredExpiration;
    private boolean tempRemoved;
    private long tempRemovedStamp, resetTempRemovedStamp, setIgnoredStamp, resetIgnoredStamp;
//    private String ignorableId;

    public Ignorable() {
//        this.initializeIgnorableId();
    }

    public Ignorable(long ignoredExpiration) {
        this.ignoredExpiration = ignoredExpiration;
//        this.initializeIgnorableId();
//        final long currentTime = System.currentTimeMillis();
//        if (currentTime < this.ignoredExpiration) {
//            this.ignored = true;
//        }
//        if (this.ignoredExpiration == TEMP_REMOVED) {
//            this.tempRemoved = true;
//        }
    }

//    public synchronized Class<? extends Ignorable> getIgnorableClass() { // only used for a test
//        return this.getClass();
//    }
//    private void initializeIgnorableId() {
//        this.ignorableId = UUID.randomUUID().toString();
//    }
//    public synchronized String getIgnorableId() {
//        if (this.ignorableId == null) {
//            this.initializeIgnorableId();
//        }
//        return this.ignorableId;
//    }
    public synchronized boolean isBanRecent() {
        final long currentTime = System.currentTimeMillis();
        return this.isSetIgnoredRecent(currentTime) || this.isTempRemovedRecent(currentTime);
    }

    public synchronized long timeSinceBan() {
        final long currentTime = System.currentTimeMillis();
        return timeSinceBan(currentTime);
    }

    public synchronized long timeSinceBan(long currentTime) {
        return Math.min(this.timeSinceSetIgnored(currentTime), this.timeSinceTempRemoved(currentTime));
    }

    public synchronized long timeSinceSetIgnored() {
        final long currentTime = System.currentTimeMillis();
        return timeSinceSetIgnored(currentTime);
    }

    public synchronized long timeSinceSetIgnored(long currentTime) {
        return currentTime - this.setIgnoredStamp;
    }

    public synchronized long timeSinceTempRemoved() {
        final long currentTime = System.currentTimeMillis();
        return timeSinceTempRemoved(currentTime);
    }

    public synchronized long timeSinceTempRemoved(long currentTime) {
        return currentTime - this.tempRemovedStamp;
    }

    public synchronized boolean isResetRecent() {
        final long currentTime = System.currentTimeMillis();
        return this.isResetIgnoredRecent(currentTime) || this.isResetTempRemovedRecent(currentTime);
    }

    public synchronized long timeSinceReset() {
        final long currentTime = System.currentTimeMillis();
        return timeSinceReset(currentTime);
    }

    public synchronized long timeSinceReset(long currentTime) {
        return Math.min(this.timeSinceResetIgnored(currentTime), this.timeSinceResetTempRemoved(currentTime));
    }

    public synchronized long timeSinceResetIgnored() {
        final long currentTime = System.currentTimeMillis();
        return timeSinceResetIgnored(currentTime);
    }

    public synchronized long timeSinceResetIgnored(long currentTime) {
        return currentTime - this.resetIgnoredStamp;
    }

    public synchronized long timeSinceResetTempRemoved() {
        final long currentTime = System.currentTimeMillis();
        return timeSinceResetTempRemoved(currentTime);
    }

    public synchronized long timeSinceResetTempRemoved(long currentTime) {
        return currentTime - this.resetTempRemovedStamp;
    }

    public synchronized long getTempRemovedStamp() {
        return tempRemovedStamp;
    }

    public synchronized int setTempRemovedStamp(long stamp) {
        final int modified;
        if (stamp > this.tempRemovedStamp) {
            this.tempRemovedStamp = stamp;
            modified = 1;
        } else {
            modified = 0;
        }

        return modified;
    }

    public synchronized boolean isTempRemovedRecent() {
        final long currentTime = System.currentTimeMillis();
        return isTempRemovedRecent(currentTime, Ignorable.RECENT_PERIOD);
    }

    public synchronized boolean isTempRemovedRecent(long currentTime) {
        return isTempRemovedRecent(currentTime, Ignorable.RECENT_PERIOD);
    }

    public synchronized boolean isTempRemovedRecent(long currentTime, long recentPeriod) {
        return this.tempRemovedStamp + recentPeriod <= currentTime;
    }

    public synchronized long getResetTempRemovedStamp() {
        return resetTempRemovedStamp;
    }

    public synchronized int setResetTempRemovedStamp(long stamp) {
        final int modified;
        if (stamp > this.resetTempRemovedStamp) {
            this.resetTempRemovedStamp = stamp;
            modified = 1;
        } else {
            modified = 0;
        }

        return modified;
    }

    public synchronized boolean isResetTempRemovedRecent() {
        final long currentTime = System.currentTimeMillis();
        return isResetTempRemovedRecent(currentTime, Ignorable.RECENT_PERIOD);
    }

    public synchronized boolean isResetTempRemovedRecent(long currentTime) {
        return isResetTempRemovedRecent(currentTime, Ignorable.RECENT_PERIOD);
    }

    public synchronized boolean isResetTempRemovedRecent(long currentTime, long recentPeriod) {
        return this.resetTempRemovedStamp + recentPeriod <= currentTime;
    }

    public synchronized long getSetIgnoredStamp() {
        return setIgnoredStamp;
    }

    public synchronized int setSetIgnoredStamp(long stamp) {
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
        return this.setIgnoredStamp + recentPeriod <= currentTime;
    }

    public synchronized long getResetIgnoredStamp() {
        return resetIgnoredStamp;
    }

    public synchronized int setResetIgnoredStamp(long stamp) {
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
        return this.resetIgnoredStamp + recentPeriod <= currentTime;
    }

    public synchronized long getIgnoredExpiration() {
        return ignoredExpiration;
    }

    private synchronized int setIgnoredExpiration(long ignoredExpiration) {
        int modified;

        if (ignoredExpiration > this.ignoredExpiration) {
            this.ignoredExpiration = ignoredExpiration;

//            if (this.ignoredExpiration == TEMP_REMOVED) {
//                this.tempRemoved = true;
//                this.ignored = true;
//            } else {
            final long currentTime = System.currentTimeMillis();
            if (this.ignoredExpiration > currentTime) { // normal behaviour
                Generic.alreadyPrintedMap.logOnce(1L, logger, LogLevel.INFO, AlreadyPrintedMap.NOT_IMPORTANT_PREFIX + "ignored set: {}", this.ignoredExpiration);
//                logger.info("ignored set: {}", this.ignoredExpiration);
//                this.ignored = true;
            } else {
                logger.error("new ignoredExpiration {} not higher than currentTime {}", this.ignoredExpiration, currentTime);
            }
//            }
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

    public synchronized boolean isTempRemoved() {
        if (this.tempRemoved) {
//            if (this.ignoredExpiration != TEMP_REMOVED) {
//                this.tempRemoved = false;
//            } else { // nothing to be done, tempRemoved stays true and is returned
//            }
        } else { // nothing to be done, tempRemoved is false
        }

        return this.tempRemoved;
    }

    public synchronized int resetTempRemoved() {
        int modified;

        if (this.isTempRemoved()) {
            this.tempRemoved = false;
            this.setResetTempRemovedStamp(System.currentTimeMillis());
            Ignorable.database.addResetIgnorable(this); //
//            this.ignoredExpiration = 0L;
            modified = 1;
        } else {
            modified = 0;
        }

        return modified;
    }

    public synchronized int setTempRemoved() {
        int modified;
        if (this.isTempRemoved()) {
            modified = 0;
        } else {
            this.tempRemoved = true;
            this.setTempRemovedStamp(System.currentTimeMillis());
            modified = 1;
        }
//        modified = this.setIgnoredExpiration(TEMP_REMOVED);

        return modified;
    }

    public synchronized boolean isIgnoredOrTempRemoved() {
        return this.isIgnored() || this.isTempRemoved();
    }

    public synchronized boolean isIgnored() {
        final long currentTime = System.currentTimeMillis();
        return isIgnored(currentTime);
    }

    public synchronized boolean isIgnored(long currentTime) {
//        if (this.ignored) {

        final boolean isIgnored;
        if (this.ignoredExpiration == 0L) {
            isIgnored = false;
        } else if (this.ignoredExpiration <= currentTime) {
            isIgnored = false;
//            logger.info("ignored expired: {}", this.ignoredExpiration);
            resetIgnored();
        } else { // nothing to be done, ignored stays true and is returned
            isIgnored = true;
        }
//        } else { // nothing to be done, ignored is false
//        }

        return isIgnored;
    }

    public synchronized int resetIgnored() {
        int modified;

        if (this.ignoredExpiration != 0L) {
//            this.ignored = false;
            logger.info("ignored reset: {}", this.ignoredExpiration);
            this.ignoredExpiration = 0L;
            this.setResetIgnoredStamp(System.currentTimeMillis());
//            Ignorable.database;
            modified = 1;
        } else {
            modified = 0;
        }

        return modified;
    }

    public synchronized int setIgnored(long period) {
        final long currentTime = System.currentTimeMillis();
        return setIgnored(period, currentTime);
    }

    public synchronized int setIgnored(long period, long currentTime) {
        int modified;

        if (period > Generic.DAY_LENGTH_MILLISECONDS * 10_000L) {
            logger.error("trying to set too big period for ignored: {}", period);
            modified = -10000;
        } else {
            final long expiration = currentTime + period;
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
            if (ignorable.isTempRemoved()) {
                modified += this.setTempRemoved();
            }
            // stamps can only increase; ignorableId is not updated
            modified += this.setTempRemovedStamp(ignorable.getTempRemovedStamp());
            modified += this.setResetTempRemovedStamp(ignorable.getResetTempRemovedStamp());
            modified += this.setSetIgnoredStamp(ignorable.getSetIgnoredStamp());
            modified += this.setResetIgnoredStamp(ignorable.getResetIgnoredStamp());
        }
        return modified;
    }
}
