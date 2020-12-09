package info.fmro.shared.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

class AlreadyPrintedProperties
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AlreadyPrintedProperties.class);
    @Serial
    private static final long serialVersionUID = 1139020933506122249L;
    private final boolean propertiesAreImportant;
    private final String id = UUID.randomUUID().toString();
    private final long initialTimeStamp;
    private long lastTimeStamp;
    private int nAppeared;
    private final LogLevel logLevel;

    //    public AlreadyPrintedProperties(long timeStamp) {
//        this.timeStamp = timeStamp;
//        this.lastTimeStamp = timeStamp;
//        this.nAppeared = 1;
//        this.propertiesImportant = true; // true is default
//
//        logger.info("new AlreadyPrintedProperties created with id: {}", this.id);
//    }
    AlreadyPrintedProperties(final long timeStamp, final boolean propertiesImportant, final LogLevel logLevel) {
        this.initialTimeStamp = timeStamp;
        this.lastTimeStamp = timeStamp;
        this.nAppeared = 1;
        this.propertiesAreImportant = propertiesImportant;
        this.logLevel = logLevel;

        logger.debug("new AlreadyPrintedProperties created with id: {}", this.id);
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public synchronized boolean propertiesAreImportant() {
        return this.propertiesAreImportant;
    }

    public synchronized void appeared(final long timeStamp) {
        this.nAppeared++;
        this.lastTimeStamp = Math.max(this.lastTimeStamp, timeStamp);
    }

    synchronized void print(final String printedString) {
        if (this.nAppeared > 1) {
            if (this.logLevel == LogLevel.DEBUG || this.logLevel == LogLevel.TRACE) {
                logger.debug("AlreadyPrintedProperties with id {} was seen {} times during a {}s period: {}", this.id, Generic.addCommas(this.nAppeared), Generic.millisecondsToSecondsString(this.lastTimeStamp - this.initialTimeStamp), printedString);
            } else {
                logger.info("AlreadyPrintedProperties with id {} was seen {} times during a {}s period: {}", this.id, Generic.addCommas(this.nAppeared), Generic.millisecondsToSecondsString(this.lastTimeStamp - this.initialTimeStamp), printedString);
            }
        } else {
            logger.debug("AlreadyPrintedProperties with id {} was seen {} times during a {}s period", this.id, this.nAppeared, Generic.millisecondsToSecondsString(this.lastTimeStamp - this.initialTimeStamp));
        }
    }
}
