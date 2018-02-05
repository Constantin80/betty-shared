package info.fmro.shared.utility;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public class AlreadyPrintedMap
        extends SynchronizedMap<String, Long>
        implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(AlreadyPrintedMap.class);
    private static final long serialVersionUID = -8093683012072045094L;
    public static final String NOT_IMPORTANT_PREFIX = "(notImportant)"; // matters for printing the properties or not when the value expires
    public static final long defaultExpirationPeriod = 4L * Generic.HOUR_LENGTH_MILLISECONDS;
    private final HashMap<String, AlreadyPrintedProperties> propertiesMap = new HashMap<>(16);
//
//    public AlreadyPrintedMap() {
//        super();
//    }
//
//    public AlreadyPrintedMap(int initialSize) {
//        super(initialSize);
//    }
//
//    public AlreadyPrintedMap(int initialSize, float loadFactor) {
//        super(initialSize, loadFactor);
//    }

    public synchronized String logOnce(Logger logger, LogLevel logLevel, String format, Object... objects) {
        return logOnce(false, 0L, null, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(long expiryPeriod, Logger logger, LogLevel logLevel, String format, Object... objects) {
        return logOnce(false, expiryPeriod, null, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(boolean printAnyway, Logger logger, LogLevel logLevel, String format, Object... objects) {
        return logOnce(printAnyway, 0L, null, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(boolean printAnyway, long expiryPeriod, Logger logger, LogLevel logLevel, String format, Object... objects) {
        return logOnce(printAnyway, expiryPeriod, null, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(SynchronizedWriter synchronizedWriter, Logger logger, LogLevel logLevel, String format, Object... objects) {
        return logOnce(false, 0L, synchronizedWriter, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(long expiryPeriod, SynchronizedWriter synchronizedWriter, Logger logger, LogLevel logLevel, String format, Object... objects) {
        return logOnce(false, expiryPeriod, synchronizedWriter, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(boolean printAnyway, SynchronizedWriter synchronizedWriter, Logger logger, LogLevel logLevel, String format, Object... objects) {
        return logOnce(printAnyway, 0L, synchronizedWriter, logger, logLevel, format, objects);
    }

    @SuppressWarnings("AssignmentToMethodParameter")
    public synchronized String logOnce(boolean printAnyway, long expiryPeriod, SynchronizedWriter synchronizedWriter, Logger logger, LogLevel logLevel, String format,
            Object... objects) {
        final Throwable throwable;
        if (logLevel == LogLevel.ERROR && objects != null && objects.length > 0) {
            final Object object = objects[objects.length - 1];
            if (object instanceof Throwable) {
                throwable = (Throwable) object;
                objects = ArrayUtils.remove(objects, objects.length - 1);
            } else {
                throwable = null;
            }
        } else {
            throwable = null;
        }
        final boolean isImportant;
        String printedString;
        if (format.startsWith(NOT_IMPORTANT_PREFIX)) {
            isImportant = false;
            printedString = MessageFormatter.arrayFormat(format.substring(NOT_IMPORTANT_PREFIX.length()), objects).getMessage();
        } else {
            isImportant = true;
            printedString = MessageFormatter.arrayFormat(format, objects).getMessage();
        }
        final long usedExpiryPeriod;
        if (expiryPeriod <= 0L) {
            usedExpiryPeriod = AlreadyPrintedMap.defaultExpirationPeriod;
        } else {
            usedExpiryPeriod = expiryPeriod;
        }

        final boolean alreadyPrinted = this.containsAndAdd(isImportant, printedString, usedExpiryPeriod);
        if (printAnyway || !alreadyPrinted) {
            switch (logLevel) {
                case ERROR:
                    if (throwable != null) {
                        logger.error(printedString, throwable);
                    } else {
                        logger.error(printedString);
                    }
                    break;
                case WARN:
                    logger.warn(printedString);
                    break;
                case INFO:
                    logger.info(printedString);
                    break;
                case DEBUG:
                    logger.debug(printedString);
                    break;
                case TRACE:
                    logger.trace(printedString);
                    break;
                default:
                    logger.error("STRANGE unknown logger level {} for: {}", logLevel, printedString);
                    break;
            }
            if (synchronizedWriter != null) {
                synchronizedWriter.writeAndFlush(Generic.properTimeStamp() + " " + printedString + "\r\n");
            }
        } else {
            printedString = null;
        }
        return printedString;
    }

    public synchronized Long add(String string) {
        return this.add(string, defaultExpirationPeriod);
    }

    public synchronized Long add(String string, long expiryPeriod) {
        return this.put(string, expiryPeriod); // current time will be added in the modified put method
    }

    public synchronized Long add(boolean isImportant, String string, long expiryPeriod) {
        return this.put(isImportant, string, expiryPeriod); // current time will be added in the modified put method
    }

    public synchronized boolean containsAndAdd(String string) {
        return this.containsAndAdd(string, defaultExpirationPeriod);
    }

    public synchronized boolean containsAndAdd(String string, long expiryPeriod) {
        final boolean contains = this.contains(string);
        this.add(string, expiryPeriod);

        return contains;
    }

    public synchronized boolean containsAndAdd(boolean isImportant, String string, long expiryPeriod) {
        final boolean contains = this.contains(string);
        this.add(isImportant, string, expiryPeriod);

        return contains;
    }

    public synchronized boolean contains(String string) {
        if (this.containsKey(string)) {
            final Long value = this.get(string);
            if (value != null) {
                final long currentTime = System.currentTimeMillis();
                if (value > currentTime) {
                    return true;
                } else {
                    this.remove(string);
                    return false;
                }
            } else {
                this.remove(string);
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public synchronized Long put(String string, Long expiryPeriod) {
        return put(true, string, expiryPeriod); // default is true for isImportant
    }

    public synchronized Long put(boolean isImportant, String string, Long expiryPeriod) { // boolean is first argument, as boolean as last argument exists in super.put overload
        final long currentTime = System.currentTimeMillis();
        final Long existingValue = super.put(string, currentTime + expiryPeriod);

        final AlreadyPrintedProperties properties;
        if (this.propertiesMap.containsKey(string)) {
            properties = this.propertiesMap.get(string);
            properties.appeared(currentTime);
        } else {
            properties = new AlreadyPrintedProperties(currentTime, isImportant);
            this.propertiesMap.put(string, properties);
        }

        return existingValue;
    }

    @Override
    public synchronized Long remove(String string) {
        final Long existingValue = super.remove(string);
        final AlreadyPrintedProperties properties = this.propertiesMap.remove(string);
        if (properties == null) {
            logger.error("null properties in AlreadyPrintedMap for: {} {}", existingValue, string);
        } else {
            if (properties.isImportant()) {
                properties.print();
            } else { // not important, will be discarded silently, nothing to be done
            }
        }

        return existingValue;
    }
}
