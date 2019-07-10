package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AlreadyPrintedMapTest {
    private static final Logger logger = LoggerFactory.getLogger(AlreadyPrintedMapTest.class);

    @Test
    void logOnce() { // due to the inexact nature of system timer, this test could, rarely, fail
        String result = Generic.alreadyPrintedMap.logOnce(false, 500L, logger, LogLevel.ERROR, "test");
        final String expectedResult = "test";
        assertEquals(expectedResult, result, "1");
        result = Generic.alreadyPrintedMap.logOnce(false, 500L, logger, LogLevel.ERROR, "test");
        assertNull(result, "2");
        Generic.threadSleep(1_000L);
        result = Generic.alreadyPrintedMap.logOnce(false, 500L, logger, LogLevel.ERROR, "test");
        assertEquals(expectedResult, result, "3");

        Generic.threadSleep(200L);
        result = Generic.alreadyPrintedMap.logOnce(false, 500L, logger, LogLevel.ERROR, "test");
        assertNull(result, "counter:" + 1);
        Generic.threadSleep(200L);
        result = Generic.alreadyPrintedMap.logOnce(false, 500L, logger, LogLevel.ERROR, "test");
        assertNull(result, "counter:" + 2);
        Generic.threadSleep(200L);
        result = Generic.alreadyPrintedMap.logOnce(false, 500L, logger, LogLevel.ERROR, "test");
        assertEquals(expectedResult, result, "counter:" + 3);
    }
}
