package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class IgnorableTest {
    @Test
    void timeSinceSetIgnored() {
        final long currentTime = System.currentTimeMillis();
        final Ignorable instance = new Ignorable();
        instance.setIgnored(1_000L);
        long result = instance.timeSinceSetIgnored();
        assertTrue(Math.abs(result) < 1_000L, "no argument");
        result = instance.timeSinceSetIgnored(currentTime);
        assertTrue(Math.abs(result) < 1_000L, "with argument");
    }

    @Test
    void timeSinceResetIgnored()
            throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        final Ignorable instance = new Ignorable();
        instance.setIgnored(10L);
        long result = instance.timeSinceResetIgnored();
        assertTrue(Math.abs(result - currentTime) < 1_000L, "before sleep, no argument");
        result = instance.timeSinceResetIgnored(currentTime);
        assertEquals(currentTime, result, "before sleep, with argument");

        Thread.sleep(1_000);
        currentTime = System.currentTimeMillis();
        instance.isIgnored();
        result = instance.timeSinceResetIgnored();
        assertTrue(Math.abs(result) < 1_000L, "after sleep, no argument");
        result = instance.timeSinceResetIgnored(currentTime);
        assertTrue(Math.abs(result) < 1_000L, "after sleep, with argument");
    }

    @Test
    void getSetIgnoredStamp() {
        final Ignorable instance = new Ignorable();
        long result = instance.getSetIgnoredStamp();
        assertEquals(0L, result, "default");

        instance.setIgnored(10L);
        final long currentTime = System.currentTimeMillis();
        result = instance.getSetIgnoredStamp();
        assertTrue(Math.abs(result - currentTime) < 1_000L, "modified");
    }

    @Test
    void isSetIgnoredRecent() {
        final Ignorable instance = new Ignorable();
        final long currentTime = System.currentTimeMillis();
        instance.setIgnored(10L);

        boolean result = instance.isSetIgnoredRecent();
        assertTrue(result, "no argument");
        result = instance.isSetIgnoredRecent(currentTime);
        assertTrue(result, "1 argument");
        result = instance.isSetIgnoredRecent(currentTime + 10_000L, 2_000L);
        assertFalse(result, "2 arguments");
    }

    @Test
    void getResetIgnoredStamp()
            throws InterruptedException {
        final Ignorable instance = new Ignorable();
        instance.setIgnored(10L);
        long result = instance.getResetIgnoredStamp();
        assertEquals(0L, result, "before sleep");

        Thread.sleep(1_000);
        long currentTime = System.currentTimeMillis();
        instance.isIgnored();
        result = instance.getResetIgnoredStamp();
        assertTrue(Math.abs(result - currentTime) < 1_000L, "after sleep");
    }

    @Test
    void isResetIgnoredRecent()
            throws InterruptedException {
        final Ignorable instance = new Ignorable();
        instance.setIgnored(10L);
        long currentTime = System.currentTimeMillis();

        boolean result = instance.isResetIgnoredRecent();
        assertFalse(result, "no argument");
        result = instance.isResetIgnoredRecent(currentTime);
        assertFalse(result, "1 argument");
        result = instance.isResetIgnoredRecent(currentTime + 10_000L, 2_000L);
        assertFalse(result, "2 arguments");

        Thread.sleep(1_000);
        currentTime = System.currentTimeMillis();
        instance.isIgnored();

        result = instance.isResetIgnoredRecent();
        assertTrue(result, "after sleep, no argument");
        result = instance.isResetIgnoredRecent(currentTime);
        assertTrue(result, "after sleep, 1 argument");
        result = instance.isResetIgnoredRecent(currentTime + 10_000L, 2_000L);
        assertFalse(result, "after sleep, 2 arguments");
    }

    @Test
    void getIgnoredExpiration() {
        Ignorable instance = new Ignorable();
        long expResult = 0L;
        long result = instance.getIgnoredExpiration();
        assertEquals(expResult, result);
    }

    @Test
    void isIgnored_0args() {
        Ignorable instance = new Ignorable();
        boolean expResult = false;
        boolean result = instance.isIgnored();
        assertEquals(expResult, result);
    }

    @Test
    void isIgnored_long() {
        Ignorable instance = new Ignorable();
        instance.setIgnored(60_000L);

        long currentTime = System.currentTimeMillis() + 10_000L;
        boolean expResult = true;
        boolean result = instance.isIgnored(currentTime);
        assertEquals(expResult, result);
    }

    @Test
    void setIgnored_long() {
        long period = 60_000L;
        Ignorable instance = new Ignorable();
        int expResult = 1;
        int result = instance.setIgnored(period);
        assertEquals(expResult, result, "modifed");

        boolean expBooleanResult = true;
        boolean booleanResult = instance.isIgnored();
        assertEquals(expBooleanResult, booleanResult, "isIgnored");
    }

    @Test
    void setIgnored_long_long() {
        long period = 60_000L;
        long currentTime = System.currentTimeMillis();
        Ignorable instance = new Ignorable();

        int expResult = 1;
        int result = instance.setIgnored(period, currentTime);
        assertEquals(expResult, result, "modifed");

        result = instance.setIgnored(currentTime, period);
        assertTrue(result < 0, "error modifed");

        boolean expBooleanResult = true;
        boolean booleanResult = instance.isIgnored();
        assertEquals(expBooleanResult, booleanResult, "isIgnored");
    }

    @Test
    void updateIgnorable() {
        try {
            final Ignorable firstIgnorable = Generic.createAndFill(Ignorable.class), secondIgnorable = Generic.createAndFill(Ignorable.class);
            firstIgnorable.updateIgnorable(secondIgnorable);
            assertThat(firstIgnorable).as("updated object different").isEqualToIgnoringGivenFields(secondIgnorable, "ignored", "ignoredExpiration", "resetIgnoredStamp", "setIgnoredStamp");

            // resetIgnoredStamp test need to happen before ignored test, as isIgnored() can modify the stamp
            assertTrue(firstIgnorable.getResetIgnoredStamp() >= secondIgnorable.getResetIgnoredStamp(),
                       "resetIgnoredStamp " + firstIgnorable.getResetIgnoredStamp() + " " + secondIgnorable.getResetIgnoredStamp() + " " + firstIgnorable.isIgnored() + " " + secondIgnorable.isIgnored());
            if (secondIgnorable.isIgnored() && secondIgnorable.getIgnoredExpiration() >= firstIgnorable.getIgnoredExpiration()) {
                assertTrue(firstIgnorable.isIgnored(), "ignored");
            }
            assertTrue(firstIgnorable.getIgnoredExpiration() >= secondIgnorable.getIgnoredExpiration(), "ignoredExpiration");
            assertTrue(firstIgnorable.getSetIgnoredStamp() >= secondIgnorable.getSetIgnoredStamp(), "setIgnoredStamp");
        } catch (IllegalAccessException e) {
            fail("IllegalAccessException");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            fail("InvocationTargetException");
            e.printStackTrace();
        } catch (InstantiationException e) {
            fail("InstantiationException");
            e.printStackTrace();
        }
    }
}
