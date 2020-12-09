package info.fmro.shared.utility;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

@SuppressWarnings("WeakerAccess")
public class AlreadyPrintedMap
//        extends SynchronizedMap<String, Long>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(AlreadyPrintedMap.class);
    @Serial
    private static final long serialVersionUID = -8093683012072045094L;
    public static final String NOT_IMPORTANT_PREFIX = "(notImportant)"; // matters for printing the properties or not when the value expires
    public static final boolean defaultIsImportant = true;
    public static final long defaultExpirationPeriod = 4L * Generic.HOUR_LENGTH_MILLISECONDS;
    private final HashMap<String, Long> expirationTimeMap = new HashMap<>(16);
    private final HashMap<String, AlreadyPrintedProperties> propertiesMap = new HashMap<>(16);

    public synchronized String logOnce(final Logger methodArgumentLogger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(false, 0L, null, methodArgumentLogger, logLevel, format, objects);
    }

    public synchronized String logOnce(final long expiryPeriod, final Logger methodArgumentLogger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(false, expiryPeriod, null, methodArgumentLogger, logLevel, format, objects);
    }

    public synchronized String logOnce(final boolean printAnyway, final Logger methodArgumentLogger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(printAnyway, 0L, null, methodArgumentLogger, logLevel, format, objects);
    }

    public synchronized String logOnce(final boolean printAnyway, final long expiryPeriod, final Logger methodArgumentLogger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(printAnyway, expiryPeriod, null, methodArgumentLogger, logLevel, format, objects);
    }

    public synchronized String logOnce(final SynchronizedWriter synchronizedWriter, final Logger methodArgumentLogger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(false, 0L, synchronizedWriter, methodArgumentLogger, logLevel, format, objects);
    }

    public synchronized String logOnce(final long expiryPeriod, final SynchronizedWriter synchronizedWriter, final Logger methodArgumentLogger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(false, expiryPeriod, synchronizedWriter, methodArgumentLogger, logLevel, format, objects);
    }

    public synchronized String logOnce(final boolean printAnyway, final SynchronizedWriter synchronizedWriter, final Logger methodArgumentLogger, final LogLevel logLevel, final String format, final Object... objects) {
        return logOnce(printAnyway, 0L, synchronizedWriter, methodArgumentLogger, logLevel, format, objects);
    }

    private synchronized String logOnce(final boolean printAnyway, final long expiryPeriod, final SynchronizedWriter synchronizedWriter, final Logger methodArgumentLogger, final LogLevel logLevel, final String format, final Object... objects) {
        @Nullable final Throwable throwable;
        final Object[] objectsWithoutThrowable;
        if (logLevel == LogLevel.ERROR && objects != null && objects.length > 0) {
            final Object lastObject = objects[objects.length - 1];
            if (lastObject instanceof Throwable) {
                throwable = (Throwable) lastObject;
                objectsWithoutThrowable = ArrayUtils.remove(objects, objects.length - 1);
            } else {
                throwable = null;
                objectsWithoutThrowable = objects;
            }
        } else {
            throwable = null;
            objectsWithoutThrowable = objects;
        }
        final boolean isImportant;
        @Nullable String printedString;
        if (format.startsWith(NOT_IMPORTANT_PREFIX)) {
            isImportant = false;
            printedString = MessageFormatter.arrayFormat(format.substring(NOT_IMPORTANT_PREFIX.length()), objectsWithoutThrowable).getMessage();
        } else {
            isImportant = true;
            printedString = MessageFormatter.arrayFormat(format, objectsWithoutThrowable).getMessage();
        }
        final long usedExpiryPeriod;
        usedExpiryPeriod = expiryPeriod <= 0L ? AlreadyPrintedMap.defaultExpirationPeriod : expiryPeriod;

        final boolean notAlreadyPrinted = !this.containsOrAdd(isImportant, printedString, usedExpiryPeriod, logLevel);
        if (printAnyway || notAlreadyPrinted) {
            switch (logLevel) {
                case ERROR:
                    if (throwable != null) {
                        methodArgumentLogger.error(printedString, throwable);
                    } else {
                        methodArgumentLogger.error(printedString);
                    }
                    break;
                case WARN:
                    methodArgumentLogger.warn(printedString);
                    break;
                case INFO:
                    methodArgumentLogger.info(printedString);
                    break;
                case DEBUG:
                    methodArgumentLogger.debug(printedString);
                    break;
                case TRACE:
                    methodArgumentLogger.trace(printedString);
                    break;
                default:
                    methodArgumentLogger.error("STRANGE unknown logger level {} for: {}", logLevel, printedString);
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

    //    private synchronized Long add(String s) {
//        return this.add(s, defaultExpirationPeriod);
//    }
//    private synchronized Long add(String s, long expiryPeriod) {
//        return this.put(s, expiryPeriod); // current time will be added in the modified put method
//    }
    @SuppressWarnings("UnusedReturnValue")
    private synchronized Long add(final boolean isImportant, final String s, final long expiryPeriod, final LogLevel logLevel) {
        return this.put(isImportant, s, expiryPeriod, logLevel); // current time will be added in the modified put method
    }

    public synchronized boolean containsOrAdd(final String s, final LogLevel logLevel) {
        return this.containsOrAdd(defaultIsImportant, s, defaultExpirationPeriod, logLevel);
    }

    public synchronized boolean containsOrAdd(final String s, final long expiryPeriod, final LogLevel logLevel) {
        return containsOrAdd(defaultIsImportant, s, expiryPeriod, logLevel);
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized boolean containsOrAdd(final boolean isImportant, final String s, final long expiryPeriod, final LogLevel logLevel) {
        final boolean contains = this.contains(s, logLevel);
        if (contains) { // contains already, which already checks for expiration of the period, nothing to be done
        } else {
            this.add(isImportant, s, expiryPeriod, logLevel);
        }

        return contains;
    }

    private synchronized boolean contains(final String s, final LogLevel logLevel) { // also updates properties
        final boolean result;
        if (this.expirationTimeMap.containsKey(s)) {
            final Long value = this.expirationTimeMap.get(s);
            if (value != null) {
                final long currentTime = System.currentTimeMillis();
                if (value > currentTime) {
                    updateProperties(defaultIsImportant, s, currentTime, true, logLevel);
                    result = true;
                } else {
                    this.remove(s);
                    result = false;
                }
            } else {
                logger.error("null expiration found in AlreadyPrintedMap.contains for: {}", s);
                this.remove(s);
                result = false;
            }
        } else {
            result = false;
        }

        return result;
    }

    private synchronized void updateProperties(final boolean isImportant, final String s, final long currentTime, final boolean shouldExist, final LogLevel logLevel) {
        final AlreadyPrintedProperties properties;
        if (this.propertiesMap.containsKey(s)) {
            properties = this.propertiesMap.get(s);
            properties.appeared(currentTime);
            if (!shouldExist) {
                logger.error("properties exists in AlreadyPrintedMap.updateProperties for: {} {} {} {} {}", isImportant, s, currentTime, shouldExist, Generic.objectToString(properties));
            }
        } else {
            if (shouldExist) {
                logger.error("properties doesn't exist in AlreadyPrintedMap.updateProperties for: {} {} {} {}", isImportant, s, currentTime, shouldExist);
            }
            properties = new AlreadyPrintedProperties(currentTime, isImportant, logLevel);
            this.propertiesMap.put(s, properties);
        }
    }

    @SuppressWarnings("unused")
    private synchronized Long put(final String s, final Long expiryPeriod, final LogLevel logLevel) {
        return put(defaultIsImportant, s, expiryPeriod, logLevel);
    }

    private synchronized Long put(final boolean isImportant, final String s, final Long expiryPeriod, final LogLevel logLevel) { // boolean is first argument, as boolean as last argument exists in super.put overload
        final long currentTime = System.currentTimeMillis();
        final Long existingValue = this.expirationTimeMap.put(s, currentTime + expiryPeriod);
        if (existingValue != null) {
            logger.error("value already existed in AlreadyPrintedMap.put for: {} {} {} {} {}", isImportant, s, expiryPeriod, currentTime, existingValue);
        } else { // value didn't exist, this is the normal case, nothing more to be done
        }

        updateProperties(isImportant, s, currentTime, false, logLevel);

        return existingValue;
    }

    @SuppressWarnings("UnusedReturnValue")
    private synchronized Long remove(final String s) {
        final Long existingValue = this.expirationTimeMap.remove(s);
        final AlreadyPrintedProperties properties = this.propertiesMap.remove(s);
        if (properties == null) {
            logger.error("null properties in AlreadyPrintedMap for: {} {}", existingValue, s);
        } else {
            if (properties.propertiesAreImportant()) {
                properties.print(s);
            } else { // not important, will be discarded silently, nothing to be done
            }
        }

        return existingValue;
    }

    public synchronized void clear() {
        final Iterable<String> keySetCopy = new HashSet<>(this.expirationTimeMap.keySet());
        for (final String key : keySetCopy) {
            this.remove(key);
        }
        if (!this.expirationTimeMap.isEmpty() || !this.propertiesMap.isEmpty()) {
            logger.error("stuff remains after clear in AlreadyPrintedMap: {}", Generic.objectToString(this));
        } else { // normal case, all cleared, nothing to be done
        }
    }

    private synchronized int size() {
        final int result;
        final int mapSize = this.expirationTimeMap.size();
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
        final Iterable<String> keySetCopy = new HashSet<>(this.expirationTimeMap.keySet());
        for (final String key : keySetCopy) {
            final Long value = this.expirationTimeMap.get(key);
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
