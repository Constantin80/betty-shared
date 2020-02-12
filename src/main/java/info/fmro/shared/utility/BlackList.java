package info.fmro.shared.utility;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings({"UtilityClass", "ClassWithTooManyMethods"})
public final class BlackList {
    private static final Logger logger = LoggerFactory.getLogger(BlackList.class);

    private BlackList() {
    }

    @SuppressWarnings("unchecked")
    public static <T> void printNotExistOrBannedErrorMessages(final Class<? extends Ignorable> clazz, final T key, final String format, final long DEFAULT_REMOVE_OR_BAN_SAFETY_PERIOD, @NotNull final Method getIgnorableMap) {
        SynchronizedMap<T, ? extends Ignorable> synchronizedMap = null;
        try {
            synchronizedMap = (SynchronizedMap<T, ? extends Ignorable>) getIgnorableMap.invoke(null, clazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("exception in getIgnorableMap.invoke for {}", clazz, e);
        }
        final long currentTime = System.currentTimeMillis();

        printNotExistOrBannedErrorMessages(synchronizedMap, key, currentTime, DEFAULT_REMOVE_OR_BAN_SAFETY_PERIOD, format);
    }

    public static <T> void printNotExistOrBannedErrorMessages(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final T key, final String format, final long DEFAULT_REMOVE_OR_BAN_SAFETY_PERIOD) {
        final long currentTime = System.currentTimeMillis();
        printNotExistOrBannedErrorMessages(synchronizedMap, key, currentTime, DEFAULT_REMOVE_OR_BAN_SAFETY_PERIOD, format);
    }

    @SuppressWarnings("unchecked")
    public static <T> void printNotExistOrBannedErrorMessages(final Class<? extends Ignorable> clazz, final T key, final long currentTime, final String format, final long DEFAULT_REMOVE_OR_BAN_SAFETY_PERIOD, @NotNull final Method getIgnorableMap) {
        SynchronizedMap<T, ? extends Ignorable> synchronizedMap = null;
        try {
            synchronizedMap = (SynchronizedMap<T, ? extends Ignorable>) getIgnorableMap.invoke(null, clazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("exception in getIgnorableMap.invoke for {}", clazz, e);
        }
        printNotExistOrBannedErrorMessages(synchronizedMap, key, currentTime, DEFAULT_REMOVE_OR_BAN_SAFETY_PERIOD, format);
    }

    public static <T> void printNotExistOrBannedErrorMessages(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final T key, final long currentTime, final String format, final long DEFAULT_REMOVE_OR_BAN_SAFETY_PERIOD) {
        printNotExistOrBannedErrorMessages(synchronizedMap, key, currentTime, DEFAULT_REMOVE_OR_BAN_SAFETY_PERIOD, format);
    }

    @SuppressWarnings("unchecked")
    public static <T> void printNotExistOrBannedErrorMessages(final Class<? extends Ignorable> clazz, final T key, final long currentTime, final long safetyPeriod, final String format, @NotNull final Method getIgnorableMap) {
        SynchronizedMap<T, ? extends Ignorable> synchronizedMap = null;
        try {
            synchronizedMap = (SynchronizedMap<T, ? extends Ignorable>) getIgnorableMap.invoke(null, clazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("exception in getIgnorableMap.invoke for {}", clazz, e);
        }
        printNotExistOrBannedErrorMessages(synchronizedMap, key, currentTime, safetyPeriod, format);
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> void printNotExistOrBannedErrorMessages(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final T key, final long currentTime, final long safetyPeriod, final String format) {
        if (notExist(synchronizedMap, key)) {
            final long timeSinceLastRemoved = timeSinceRemovalFromMap(synchronizedMap, currentTime);
            final String printedString =
                    MessageFormatter.arrayFormat("{} no value in map, timeSinceLastRemoved: {} for key: {}", new Object[]{format, timeSinceLastRemoved, key}).getMessage();
            if (timeSinceLastRemoved <= safetyPeriod) {
                logger.info(printedString);
            } else {
                logger.error(printedString);
            }
        } else {
            final long timeSinceBan = timeSinceBan(synchronizedMap, key, currentTime);
            final String printedString = MessageFormatter.arrayFormat("{} ignored for key: {} {}", new Object[]{format, timeSinceBan, key}).getMessage();
            if (timeSinceBan <= safetyPeriod) {
                logger.info(printedString);
            } else {
                logger.error(printedString);
            }
        }
    }

    public static <T> long timeSinceBan(final Ignorable ignorable, final T key, @NotNull final Method getIgnorableMap) {
        final long currentTime = System.currentTimeMillis();
        return timeSinceBan(ignorable, key, currentTime, getIgnorableMap);
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> long timeSinceBan(final Ignorable ignorable, final T key, final long currentTime, @NotNull final Method getIgnorableMap) {
        final long result;
        if (ignorable == null) {
            logger.error("null ignorable in timeSinceBan for {} {}", key, currentTime);
            result = Long.MAX_VALUE;
        } else {
            final Class<? extends Ignorable> clazz = ignorable.getClass();
            result = timeSinceBan(clazz, key, currentTime, getIgnorableMap);
        }

        return result;
    }

    public static <T> long timeSinceBan(final Class<? extends Ignorable> clazz, final T key, @NotNull final Method getIgnorableMap) {
        final long currentTime = System.currentTimeMillis();
        return timeSinceBan(clazz, key, currentTime, getIgnorableMap);
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static <T> long timeSinceBan(final Class<? extends Ignorable> clazz, final T key, final long currentTime, @NotNull final Method getIgnorableMap) {
        SynchronizedMap<T, ? extends Ignorable> synchronizedMap = null;
        try {
            synchronizedMap = (SynchronizedMap<T, ? extends Ignorable>) getIgnorableMap.invoke(null, clazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("exception in getIgnorableMap.invoke for {}", clazz, e);
        }
        return timeSinceBan(synchronizedMap, key, currentTime);
    }

    public static <T> long timeSinceBan(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final T key) {
        final long currentTime = System.currentTimeMillis();
        return timeSinceBan(synchronizedMap, key, currentTime);
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> long timeSinceBan(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final T key, final long currentTime) {
        final long result;
        if (synchronizedMap == null) {
            logger.error("null synchronizedMap in timeSinceBan for {} {}", key, currentTime);
            result = Long.MAX_VALUE;
        } else if (synchronizedMap.containsKey(key)) {
            final Ignorable value = synchronizedMap.get(key);
            if (value == null) {
                logger.error("null value in timeSinceBan for key {} {}", key, currentTime);
                result = Long.MAX_VALUE;
            } else {
                result = value.timeSinceSetIgnored(currentTime); // it does exist
            }
        } else {
            result = Long.MAX_VALUE;
        }

        return result;
    }

    public static <T> long timeSinceRemovalFromMap(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap) {
        final long currentTime = System.currentTimeMillis();
        return timeSinceRemovalFromMap(synchronizedMap, currentTime);
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> long timeSinceRemovalFromMap(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final long currentTime) {
        final long result;
        if (synchronizedMap == null) {
            logger.error("null synchronizedMap in timeSinceRemovalFromMap: {}", currentTime);
            result = Long.MAX_VALUE;
        } else {
            final long timeStampRemoved = synchronizedMap.getTimeStampRemoved();
//            final long currentTime = System.currentTimeMillis();
            result = currentTime - timeStampRemoved;
        }

        return result;
    }

    public static long timeSinceRemovalFromMap(final Ignorable ignorable, @NotNull final Method getIgnorableMap) {
        final long currentTime = System.currentTimeMillis();
        return timeSinceRemovalFromMap(ignorable, currentTime, getIgnorableMap);
    }

    @SuppressWarnings("WeakerAccess")
    public static long timeSinceRemovalFromMap(final Ignorable ignorable, final long currentTime, @NotNull final Method getIgnorableMap) {
        final long result;
        if (ignorable == null) {
            logger.error("null ignorable in timeSinceRemovalFromMap: {}", currentTime);
            result = Long.MAX_VALUE;
        } else {
            final Class<? extends Ignorable> clazz = ignorable.getClass();
            result = timeSinceRemovalFromMap(clazz, currentTime, getIgnorableMap);
        }

        return result;
    }

    public static long timeSinceRemovalFromMap(final Class<? extends Ignorable> clazz, @NotNull final Method getIgnorableMap) {
        final long currentTime = System.currentTimeMillis();
        return timeSinceRemovalFromMap(clazz, currentTime, getIgnorableMap);
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static long timeSinceRemovalFromMap(final Class<? extends Ignorable> clazz, final long currentTime, @NotNull final Method getIgnorableMap) {
        SynchronizedMap<?, ? extends Ignorable> synchronizedMap = null;
        try {
            synchronizedMap = (SynchronizedMap<?, ? extends Ignorable>) getIgnorableMap.invoke(null, clazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("exception in getIgnorableMap.invoke for {}", clazz, e);
        }
        return timeSinceRemovalFromMap(synchronizedMap, currentTime);
    }

    public static <T> boolean notExist(final Ignorable ignorable, final T key, @NotNull final Method getIgnorableMap) {
        final boolean result;
        if (ignorable == null) {
            logger.error("null ignorable in notExist for {}", key);
            result = true; // it's true that there's an error
        } else {
            final Class<? extends Ignorable> clazz = ignorable.getClass();
            result = notExist(clazz, key, getIgnorableMap);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean notExist(final Class<? extends Ignorable> clazz, final T key, @NotNull final Method getIgnorableMap) {
        SynchronizedMap<T, ? extends Ignorable> synchronizedMap = null;
        try {
            synchronizedMap = (SynchronizedMap<T, ? extends Ignorable>) getIgnorableMap.invoke(null, clazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("exception in getIgnorableMap.invoke for {}", clazz, e);
        }
        return notExist(synchronizedMap, key);
    }

    public static <T> boolean notExist(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final T key) {
        final boolean result;

        if (synchronizedMap == null) {
            logger.error("null synchronizedMap in notExist for {}", key);
            result = true; // it's true that it doesn't exist
        } else if (synchronizedMap.containsKey(key)) {
            final Ignorable value = synchronizedMap.get(key);
            if (value == null) {
                logger.error("null value in notExist for key {}", key);
                result = true; // it's true that there's an error
            } else {
//                result = value.isIgnored(currentTime);
                result = false; // it does exist
            }
        } else {
            result = true; // it's true that it doesn't exist
        }

        return result;
    }

    public static <T> boolean notExistOrIgnored(final Ignorable ignorable, final T key, @NotNull final Method getIgnorableMap) {
        final long currentTime = System.currentTimeMillis();
        return notExistOrIgnored(ignorable, key, currentTime, getIgnorableMap);
    }

    @SuppressWarnings("WeakerAccess")
    public static <T> boolean notExistOrIgnored(final Ignorable ignorable, final T key, final long currentTime, @NotNull final Method getIgnorableMap) {
        final boolean result;
        if (ignorable == null) {
            logger.error("null ignorable in notExistOrIgnored for {} {}", key, currentTime);
            result = true; // it's true that there's an error
        } else {
            final Class<? extends Ignorable> clazz = ignorable.getClass();
            result = notExistOrIgnored(clazz, key, currentTime, getIgnorableMap);
        }

        return result;
    }

    public static <T> boolean notExistOrIgnored(final Class<? extends Ignorable> clazz, final T key, @NotNull final Method getIgnorableMap) {
        final long currentTime = System.currentTimeMillis();
        return notExistOrIgnored(clazz, key, currentTime, getIgnorableMap);
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static <T> boolean notExistOrIgnored(final Class<? extends Ignorable> clazz, final T key, final long currentTime, @NotNull final Method getIgnorableMap) {
        SynchronizedMap<T, ? extends Ignorable> synchronizedMap = null;
        try {
            synchronizedMap = (SynchronizedMap<T, ? extends Ignorable>) getIgnorableMap.invoke(null, clazz);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("exception in getIgnorableMap.invoke for {}", clazz, e);
        }
        return notExistOrIgnored(synchronizedMap, key, currentTime);
    }

    public static <T> boolean notExistOrIgnored(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final T key) {
        final long currentTime = System.currentTimeMillis();
        return notExistOrIgnored(synchronizedMap, key, currentTime);
    }

    public static <T> boolean notExistOrIgnored(final SynchronizedMap<T, ? extends Ignorable> synchronizedMap, final T key, final long currentTime) {
        final boolean result;

        if (synchronizedMap == null) {
            logger.error("null synchronizedMap in notExistOrIgnored for {} {}", key, currentTime);
            result = true; // it's true that it doesn't exist
        } else if (synchronizedMap.containsKey(key)) {
            final Ignorable value = synchronizedMap.get(key);
            if (value == null) {
                logger.error("null value in notExistOrIgnored for key {}", key);
                result = true; // it's true that there's an error
            } else {
                result = value.isIgnored(currentTime);
            }
        } else {
            result = true; // it's true that it doesn't exist
        }

        return result;
    }
}
