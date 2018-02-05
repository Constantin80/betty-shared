package info.fmro.shared.utility;

import java.lang.reflect.Field;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UncaughtExceptionHandlerTest
{
    private static final Logger logger = LoggerFactory.getLogger (UncaughtExceptionHandlerTest.class);

    public UncaughtExceptionHandlerTest ()
    {
    }
    @Rule
    @SuppressWarnings ("PublicField")
    public TestRule watchman = new TestWatcher ()
    {
        @Override
        public void starting (Description description)
        {
            logger.info ("{} being run...", description.getMethodName ());
        }
    };

    // Test of uncaughtException method, of class UncaughtExceptionHandler.
    @Test
    public void testUncaughtException ()
        throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException
    {
        final Mockery context = new Mockery ();

        Thread thread = new Thread ();
        Throwable throwable = new Throwable ();
        UncaughtExceptionHandler instance = new UncaughtExceptionHandler ();
        final Logger mockLogger = context.mock (Logger.class);

        Field field = UncaughtExceptionHandler.class.getDeclaredField ("logger");
        Generic.setFinalStatic (field, mockLogger);

        // expectations
        context.checking (new Expectations ()
        {
            {
                oneOf (mockLogger).error (with (any (String.class)), with (any (Object[].class)), with (any (Throwable.class)));
            }
        });

        instance.uncaughtException (thread, throwable);

        // verify
        context.assertIsSatisfied ();
    }
}
