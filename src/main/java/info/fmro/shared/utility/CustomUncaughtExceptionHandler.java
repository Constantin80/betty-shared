package info.fmro.shared.utility;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unused", "ClassOnlyUsedInOneModule"})
public class CustomUncaughtExceptionHandler
        implements Thread.UncaughtExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomUncaughtExceptionHandler.class);

    // Implements Thread.CustomUncaughtExceptionHandler.uncaughtException()
    @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
    @Override
    public void uncaughtException(@NotNull final Thread thread, final Throwable throwable) {
        logger.error("Crashed thread: {}", new Object[]{thread.getName(), thread.getId(), thread}, throwable);
    }
}
