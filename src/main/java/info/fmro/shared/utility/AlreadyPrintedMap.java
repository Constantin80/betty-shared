package info.fmro.shared.utility;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

public class AlreadyPrintedMap
//        extends SynchronizedMap<String, Long>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AlreadyPrintedMap.class);
    private static final long serialVersionUID = -8093683012072045094L;
    public static final String NOT_IMPORTANT_PREFIX = "(notImportant)"; // matters for printing the properties or not when the value expires
    public static final boolean defaultIsImportant = true;
    public static final long defaultExpirationPeriod = 4L * Generic.HOUR_LENGTH_MILLISECONDS;
    private final HashMap<String, Long> map = new HashMap<>();
    private final HashMap<String, AlreadyPrintedProperties> propertiesMap = new HashMap<>(16);

    public AlreadyPrintedMap() {
    }

    public synchronized String logOnce(final Logger logger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(false, 0L, null, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(final long expiryPeriod, final Logger logger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(false, expiryPeriod, null, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(final boolean printAnyway, final Logger logger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(printAnyway, 0L, null, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(final boolean printAnyway, final long expiryPeriod, final Logger logger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(printAnyway, expiryPeriod, null, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(final SynchronizedWriter synchronizedWriter, final Logger logger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(false, 0L, synchronizedWriter, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(final long expiryPeriod, final SynchronizedWriter synchronizedWriter, final Logger logger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(false, expiryPeriod, synchronizedWriter, logger, logLevel, format, objects);
    }

    public synchronized String logOnce(final boolean printAnyway, final SynchronizedWriter synchronizedWriter, final Logger logger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(printAnyway, 0L, synchronizedWriter, logger, logLevel, format, objects);
    }

    @SuppressWarnings("AssignmentToMethodParameter")
    public synchronized String logOnce(final boolean printAnyway, final long expiryPeriod, final SynchronizedWriter synchronizedWriter, final Logger logger, final LogLevel logLevel, final String format, Object... objects) {
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

        final boolean alreadyPrinted = this.containsOrAdd(isImportant, printedString, usedExpiryPeriod);
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

    //    private synchronized Long add(String string) {
//        return this.add(string, defaultExpirationPeriod);
//    }
//    private synchronized Long add(String string, long expiryPeriod) {
//        return this.put(string, expiryPeriod); // current time will be added in the modified put method
//    }
    private synchronized Long add(final boolean isImportant, final String string, final long expiryPeriod) {
        return this.put(isImportant, string, expiryPeriod); // current time will be added in the modified put method
    }

    public synchronized boolean containsOrAdd(final String string) {
        return this.containsOrAdd(defaultIsImportant, string, defaultExpirationPeriod);
    }

    public synchronized boolean containsOrAdd(final String string, final long expiryPeriod) {
        return containsOrAdd(defaultIsImportant, string, expiryPeriod);
    }

    public synchronized boolean containsOrAdd(final boolean isImportant, final String string, final long expiryPeriod) {
        final boolean contains = this.contains(string);
        if (!contains) {
            this.add(isImportant, string, expiryPeriod);
        } else { // contains already, which already checks for expiration of the period, nothing to be done
        }

        return contains;
    }

    public synchronized boolean contains(final String string) { // also updates properties
        final boolean result;
        if (this.map.containsKey(string)) {
            final Long value = this.map.get(string);
            if (value != null) {
                final long currentTime = System.currentTimeMillis();
                if (value > currentTime) {
                    updateProperties(defaultIsImportant, string, currentTime, true);
                    result = true;
                } else {
                    this.remove(string);
                    result = false;
                }
            } else {
                logger.error("null expiration found in AlreadyPrintedMap.contains for: {}", string);
                this.remove(string);
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    private synchronized void updateProperties(final boolean isImportant, final String string, final long currentTime, final boolean shouldExist) {
        final AlreadyPrintedProperties properties;
        if (this.propertiesMap.containsKey(string)) {
            properties = this.propertiesMap.get(string);
            properties.appeared(currentTime);
            if (!shouldExist) {
                logger.error("properties exists in AlreadyPrintedMap.updateProperties for: {} {} {} {} {}", isImportant, string, currentTime, shouldExist, Generic.objectToString(properties));
            }
        } else {
            if (shouldExist) {
                logger.error("properties doesn't exist in AlreadyPrintedMap.updateProperties for: {} {} {} {}", isImportant, string, currentTime, shouldExist);
            }
            properties = new AlreadyPrintedProperties(currentTime, isImportant);
            this.propertiesMap.put(string, properties);
        }
    }

    public synchronized Long put(final String string, final Long expiryPeriod) {
        return put(defaultIsImportant, string, expiryPeriod);
    }

    public synchronized Long put(final boolean isImportant, final String string, final Long expiryPeriod) { // boolean is first argument, as boolean as last argument exists in super.put overload
        final long currentTime = System.currentTimeMillis();
        final Long existingValue = this.map.put(string, currentTime + expiryPeriod);
        if (existingValue != null) {
            logger.error("value already existed in AlreadyPrintedMap.put for: {} {} {} {} {}", isImportant, string, expiryPeriod, currentTime, existingValue);
        } else { // value didn't exist, this is the normal case, nothing more to be done
        }

        updateProperties(isImportant, string, currentTime, false);

        return existingValue;
    }

    public synchronized Long remove(final String string) {
        final Long existingValue = this.map.remove(string);
        final AlreadyPrintedProperties properties = this.propertiesMap.remove(string);
        if (properties == null) {
            logger.error("null properties in AlreadyPrintedMap for: {} {}", existingValue, string);
        } else {
            if (properties.isImportant()) {
                properties.print(string);
            } else { // not important, will be discarded silently, nothing to be done
            }
        }

        return existingValue;
    }

    public synchronized void clear() {
        final HashSet<String> keySetCopy = new HashSet<>(this.map.keySet());
        for (String key : keySetCopy) {
            this.remove(key);
        }
        if (!this.map.isEmpty() || !propertiesMap.isEmpty()) {
            logger.error("stuff remains after clear in AlreadyPrintedMap: {}", Generic.objectToString(this));
        } else { // normal case, all cleared, nothing to be done
        }
    }

    public synchronized int size() {
        final int result;
        final int mapSize = this.map.size();
        final int propertiesSize = this.propertiesMap.size();
        if (mapSize == propertiesSize) {
            result = mapSize;
        } else {
            logger.error("map and properties have different sizes in AlreadyPrintedMap: {} {} {}", mapSize, propertiesSize, Generic.objectToString(this));
            result = 0;
        }

        return result;
    }

    public synchronized void clean() {
        final long currentTime = System.currentTimeMillis();
        final int initialSize = this.size();
        final HashSet<String> keySetCopy = new HashSet<>(this.map.keySet());
        for (String key : keySetCopy) {
            final Long value = this.map.get(key);
            if (value == null) {
                logger.error("null value during clean alreadyPrintedMap for: {} {}", key, Generic.objectToString(this.propertiesMap.get(key)));
                this.remove(key);
            } else {
                final long primitive = value;
                if (currentTime >= primitive) {
                    this.remove(key);
                } else { // nothing to be done, entry is not yet obsolete
                }
            }
        }
        final int newSize = this.size();
        if (newSize != initialSize) {
            logger.info("cleaned alreadyPrintedMap, initialSize: {} newSize: {} in {} ms", initialSize, newSize, System.currentTimeMillis() - currentTime);
        }
    }
}
