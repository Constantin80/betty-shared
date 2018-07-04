package info.fmro.shared.utility;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.lang.reflect.Field;

public class UncaughtExceptionHandlerTest {
    public UncaughtExceptionHandlerTest() {
    }

    // Test of uncaughtException method, of class UncaughtExceptionHandler.
  @Test
    void uncaughtException()
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final Mockery context = new Mockery();

        Thread thread = new Thread();
        Throwable throwable = new Throwable();
        UncaughtExceptionHandler instance = new UncaughtExceptionHandler();
        final Logger mockLogger = context.mock(Logger.class);

        Field field = UncaughtExceptionHandler.class.getDeclaredField("logger");
        Generic.setFinalStatic(field, mockLogger);

        // expectations
        context.checking(new Expectations() {
            {
                oneOf(mockLogger).error(with(any(String.class)), with(any(Object[].class)), with(any(Throwable.class)));
            }
        });

        instance.uncaughtException(thread, throwable);

        // verify
        context.assertIsSatisfied();
    }
}
