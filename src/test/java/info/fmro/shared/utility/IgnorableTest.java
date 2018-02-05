package info.fmro.shared.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IgnorableTest {

    private static final Logger logger = LoggerFactory.getLogger(IgnorableTest.class);

    @Rule
    @SuppressWarnings("PublicField")
    public TestRule watchman = new TestWatcher() {
        @Override
        public void starting(Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };

//    @Test
//    public void testGetIgnorableClass() {
//        SynchronizedSet<String> instance = new SynchronizedSet<>();
//
//        Class<SynchronizedSet> clazz = SynchronizedSet.class;
//        Class<? extends Ignorable> result = instance.getIgnorableClass();
//        assertEquals(clazz, result);
//    }
    @Test
    public void testGetIgnoredExpiration() {
        Ignorable instance = new Ignorable();
        long expResult = 0L;
        long result = instance.getIgnoredExpiration();
        assertEquals(expResult, result);
    }

    @Test
    public void testIsTempDeleted() {
        Ignorable instance = new Ignorable();
        boolean expResult = false;
        boolean result = instance.isTempRemoved();
        assertEquals(expResult, result);
    }

    @Test
    public void testResetTempDeleted() {
        Ignorable instance = new Ignorable();
        instance.setTempRemoved();
        int expResult = 1;
        int result = instance.resetTempRemoved();
        assertEquals("modified", expResult, result);

        boolean expBooleanResult = false;
        boolean booleanResult = instance.isTempRemoved();
        assertEquals("isTempDeleted", expBooleanResult, booleanResult);
    }

    @Test
    public void testSetTempDeleted() {
        Ignorable instance = new Ignorable();
        int expResult = 1;
        int result = instance.setTempRemoved();
        assertEquals("modified", expResult, result);

//        long expLongResult = Long.MAX_VALUE;
//        long longResult = instance.getIgnoredExpiration();
//        assertEquals("ignoredExpiration", expLongResult, longResult);
        boolean expBooleanResult = true;
        boolean booleanResult = instance.isTempRemoved();
        assertEquals("isTempDeleted", expBooleanResult, booleanResult);
    }

    @Test
    public void testIsIgnoredOrTempRemoved() {
        Ignorable instance = new Ignorable();
        instance.setIgnored(Long.MAX_VALUE); // value too big, it won't be set
        boolean result = instance.isIgnoredOrTempRemoved();
        assertFalse("ignored too big period", result);

        instance.setIgnored(3_600_000L);
        result = instance.isIgnoredOrTempRemoved();
        assertTrue("ignored", result);

        instance.resetIgnored();
        result = instance.isIgnoredOrTempRemoved();
        assertFalse("not ignored", result);

        instance.setTempRemoved();
        result = instance.isIgnoredOrTempRemoved();
        assertTrue("tempRemoved", result);
    }

    @Test
    public void testIsIgnored_0args() {
        Ignorable instance = new Ignorable();
        boolean expResult = false;
        boolean result = instance.isIgnored();
        assertEquals(expResult, result);
    }

    @Test
    public void testIsIgnored_long() {
        Ignorable instance = new Ignorable();
        instance.setIgnored(60_000L);

        long currentTime = System.currentTimeMillis() + 10_000L;
        boolean expResult = true;
        boolean result = instance.isIgnored(currentTime);
        assertEquals(expResult, result);
    }

    @Test
    public void testResetIgnored() {
        Ignorable instance = new Ignorable();
        instance.setIgnored(60_000L);

        int expResult = 1;
        int result = instance.resetIgnored();
        assertEquals("modified", expResult, result);

        boolean expBooleanResult = false;
        boolean booleanResult = instance.isIgnored();
        assertEquals("isIgnored", expBooleanResult, booleanResult);
    }

    @Test
    public void testSetIgnored_long() {
        long period = 60_000L;
        Ignorable instance = new Ignorable();
        int expResult = 1;
        int result = instance.setIgnored(period);
        assertEquals("modifed", expResult, result);

        boolean expBooleanResult = true;
        boolean booleanResult = instance.isIgnored();
        assertEquals("isIgnored", expBooleanResult, booleanResult);
    }

    @Test
    public void testSetIgnored_long_long() {
        long period = 60_000L;
        long currentTime = System.currentTimeMillis();
        Ignorable instance = new Ignorable();

        int expResult = 1;
        int result = instance.setIgnored(period, currentTime);
        assertEquals("modifed", expResult, result);

        result = instance.setIgnored(currentTime, period);
        assertTrue("error modifed", result < 0);

        boolean expBooleanResult = true;
        boolean booleanResult = instance.isIgnored();
        assertEquals("isIgnored", expBooleanResult, booleanResult);
    }

    @Test
    public void testUpdateIgnorable() {
        Ignorable newIgnorable = new Ignorable(Long.MAX_VALUE);
        newIgnorable.setTempRemoved();
        Ignorable instance = new Ignorable();
        int expResult = 2;
        int result = instance.updateIgnorable(newIgnorable);
        assertEquals("modified", expResult, result);

        boolean expBooleanResult = true;
        boolean booleanResult = instance.isIgnored();
        assertEquals("isIgnored", expBooleanResult, booleanResult);

        expBooleanResult = true;
        booleanResult = instance.isTempRemoved();
        assertEquals("isTempDeleted", expBooleanResult, booleanResult);
    }
}
