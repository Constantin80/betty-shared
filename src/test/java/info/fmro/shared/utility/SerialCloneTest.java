package info.fmro.shared.utility;

import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialCloneTest {

    private static final Logger logger = LoggerFactory.getLogger(SerialCloneTest.class);

    @Rule
    @SuppressWarnings("PublicField")
    public TestRule watchman = new TestWatcher() {
        @Override
        public void starting(Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };

    @Test
    public void testClone() {
        String input = "abc";
        Object expResult = "abc";
        Object result = SerialClone.clone(input);
        assertEquals("first", expResult, result);

        input = "";
        expResult = "";
        result = SerialClone.clone(input);
        assertEquals("second", expResult, result);

        input = null;
        expResult = null;
        result = SerialClone.clone(input);
        assertEquals("third", expResult, result);
    }
}
