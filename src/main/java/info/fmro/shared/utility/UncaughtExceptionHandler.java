package info.fmro.shared.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UncaughtExceptionHandler
        implements Thread.UncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(UncaughtExceptionHandler.class);

    // Implements Thread.UncaughtExceptionHandler.uncaughtException()
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        logger.error("Crashed thread: {}", new Object[]{thread.getName(), thread.getId(), thread}, throwable);
    }
}
