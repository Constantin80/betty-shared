package info.fmro.shared.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.UUID;

class AlreadyPrintedProperties
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AlreadyPrintedProperties.class);
    private static final long serialVersionUID = 1139020933506122249L;
    private final boolean propertiesAreImportant;
    private final String id = UUID.randomUUID().toString();
    private final long initialTimeStamp;
    private long lastTimeStamp;
    private int nAppeared;

    //    public AlreadyPrintedProperties(long timeStamp) {
//        this.timeStamp = timeStamp;
//        this.lastTimeStamp = timeStamp;
//        this.nAppeared = 1;
//        this.propertiesImportant = true; // true is default
//
//        logger.info("new AlreadyPrintedProperties created with id: {}", this.id);
//    }
    @SuppressWarnings("WeakerAccess")
    AlreadyPrintedProperties(final long timeStamp, final boolean propertiesImportant) {
        this.initialTimeStamp = timeStamp;
        this.lastTimeStamp = timeStamp;
        this.nAppeared = 1;
        this.propertiesAreImportant = propertiesImportant;

        logger.info("new AlreadyPrintedProperties created with id: {}", this.id);
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized boolean propertiesAreImportant() {
        return this.propertiesAreImportant;
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized void appeared(final long timeStamp) {
        this.nAppeared++;
        this.lastTimeStamp = Math.max(this.lastTimeStamp, timeStamp);
    }

    public synchronized void print(final String printedString) {
        if (this.nAppeared > 1) {
            logger.info("AlreadyPrintedProperties with id {} was seen {} times during a {} ms period: {}", this.id, Generic.addCommas(this.nAppeared), Generic.addCommas(this.lastTimeStamp - this.initialTimeStamp), printedString);
        } else {
            logger.info("AlreadyPrintedProperties with id {} was seen {} times during a {} ms period", this.id, this.nAppeared, Generic.addCommas(this.lastTimeStamp - this.initialTimeStamp));
        }
    }
}
