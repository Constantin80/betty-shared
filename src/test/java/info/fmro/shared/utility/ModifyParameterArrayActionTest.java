package info.fmro.shared.utility;

import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.jmock.api.Invocation;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModifyParameterArrayActionTest {

    private static final Logger logger = LoggerFactory.getLogger(ModifyParameterArrayActionTest.class);

    @Rule
    @SuppressWarnings("PublicField")
    public TestRule watchman = new TestWatcher() {
        @Override
        public void starting(org.junit.runner.Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };

    public ModifyParameterArrayActionTest() {
    }

    @Test
    public void testDescribeTo() {
        Description description = new StringDescription();
        ModifyParameterArrayAction<Object> modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 0, null);
        modifyParameterArrayAction.describeTo(description);

        String expResult = "modifies array element";
        assertEquals(expResult, description.toString());
    }

    @Test
    public void testInvoke()
            throws Throwable {
        byte[] byteArray = new byte[]{4, 1, 5};
        Invocation invocation = new Invocation(null, null, byteArray);
        ModifyParameterArrayAction<?> modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 1, (byte) 3);

        byte[] expResult = new byte[]{4, 3, 5};
        modifyParameterArrayAction.invoke(invocation);
        assertArrayEquals(expResult, byteArray);

        modifyParameterArrayAction = new ModifyParameterArrayAction<>(0, 1, new byte[]{8, 6});
        expResult = new byte[]{4, 8, 6};
        modifyParameterArrayAction.invoke(invocation);
        assertArrayEquals(expResult, byteArray);
    }
}
