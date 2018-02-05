package info.fmro.shared.utility;

import java.net.UnknownHostException;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetBytesTest {

    private static final Logger logger = LoggerFactory.getLogger(TargetBytesTest.class);

    public TargetBytesTest() {
    }
    @Rule
    @SuppressWarnings("PublicField")
    public TestRule watchman = new TestWatcher() {
        @Override
        public void starting(Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };

    @Test
    public void testGetIP()
            throws UnknownHostException {
        TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        byte[] expResult = new byte[]{-64, -88, 0, 1};
        byte[] result = instance.getIP();
        assertArrayEquals(expResult, result);
    }

    @Test
    public void testGetPort()
            throws UnknownHostException {
        TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        byte[] expResult = new byte[]{4, 56};
        byte[] result = instance.getPort();
        assertArrayEquals(expResult, result);
    }

    @Test
    public void testToString()
            throws UnknownHostException {
        TargetBytes instance = new TargetBytes("192.168.0.1", 1080);
        String expResult = "IP=192.168.0.1 port=1080";
        String result = instance.toString();
        assertEquals(expResult, result);
    }
}
