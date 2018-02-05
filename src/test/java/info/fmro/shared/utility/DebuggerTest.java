package info.fmro.shared.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebuggerTest {

    private static final Logger logger = LoggerFactory.getLogger(DebuggerTest.class);

    @Rule
    @SuppressWarnings("PublicField")
    public TestRule watchman = new TestWatcher() {
        @Override
        public void starting(Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };

    @Test
    public void testSetDebugLevel() {
        Debugger instance = null;
        try {
            int newValue = 5;
            instance = new Debugger();
            instance.setDebugLevel(newValue);
            assertEquals(newValue, instance.getDebugLevel());
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    public void testSetEncryption() {
        Debugger instance = null;
        try {
            int newValue = 5;
            instance = new Debugger();
            instance.setEncryption(newValue);
            assertEquals(newValue, instance.getEncryption());
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    public void testAddWriter_6args()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            boolean append = false;
            String charsetName = "UTF-8";
            int bufferSize = 8192;
            String id = "testId";
            int minDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, charsetName, bufferSize, id, minDebugLevel);

            assertTrue(instance.write("testString", id));
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    public void testAddWriter_4args()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            boolean append = false;
            String id = "testId";
            int minDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, id, minDebugLevel);

            assertTrue(instance.write("testString", id));
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    public void testAddWriter_3args()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            boolean append = false;
            int minDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, minDebugLevel);

            assertTrue(instance.write("testString", new File(fileName).getPath()));
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    public void testCheckDiskSpace()
            throws FileNotFoundException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        //  "Less than 50Mb remain on disk. Reducing debugLevel." message is normal
        Debugger instance = null;
        String fileName = Generic.tempFileName("test"), changedFileName = Generic.tempFileName("test");
        try {
            int debugLevel = 5;
            instance = new Debugger();
            instance.setDebugLevel(debugLevel);
            instance.addWriter(fileName, false, 0);
            instance.checkDiskSpace();
            assertEquals("before", debugLevel, instance.getDebugLevel());

            Field mapField = Debugger.class.getDeclaredField("writersMap");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            LinkedHashMap<SynchronizedWriter, Integer> writersMap = (LinkedHashMap<SynchronizedWriter, Integer>) mapField.get(instance);
            SynchronizedWriter writer = writersMap.keySet().iterator().next();

            Field writerField = SynchronizedWriter.class.getDeclaredField("file");
            writerField.setAccessible(true);
            writerField.set(writer, new File(changedFileName));

            instance.checkDiskSpace();

            assertEquals("after " + new File(changedFileName).getUsableSpace(), 0, instance.getDebugLevel());
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
            new File(changedFileName).delete();
        }
    }

    @Test
    public void testEnoughAvailableSpace()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            instance = new Debugger();
            instance.addWriter(fileName, false, 0);
            long availableSpace = file.getUsableSpace();
            logger.info("availableSpace = {}", availableSpace);

            boolean expResult = true;
            boolean result = instance.enoughAvailableSpace(availableSpace - 1024 * 1024);
            assertEquals("true", expResult, result);

            expResult = false;
            result = instance.enoughAvailableSpace(availableSpace + 1024 * 1024);
            assertEquals("false", expResult, result);
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    public void testGetDebugLevel() {
        Debugger instance = null;
        try {
            instance = new Debugger();
            int expResult = 0;
            int result = instance.getDebugLevel();
            assertEquals(expResult, result);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    public void testGetEncryption() {
        Debugger instance = null;
        try {
            instance = new Debugger();
            int expResult = 0;
            int result = instance.getEncryption();
            assertEquals(expResult, result);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    public void testGetTimeLastCheckDiskSpace() {
        Debugger instance = null;
        try {
            instance = new Debugger();
            long expResult = 0;
            long result = instance.getTimeLastCheckDiskSpace();
            assertEquals(expResult, result);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    public void testTimeLastCheckDiskSpaceStamp() {
        Debugger instance = null;
        try {
            instance = new Debugger();
            long timeBefore = System.currentTimeMillis();
            instance.timeLastCheckDiskSpaceStamp();
            long timeAfter = instance.getTimeLastCheckDiskSpace();
            assertTrue(timeAfter >= timeBefore);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    public void testWrite_3args()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            boolean append = false;
            int writerDebugLevel = 0;
            instance = new Debugger();

            instance.addWriter(fileName, append, writerDebugLevel);
            assertEquals("before", 0, file.length());

            instance.write("testString", file.getPath(), 1);
            instance.flush();
            assertEquals("after first", 0, file.length());

            instance.write("testString", file.getPath(), 0);
            instance.flush();
            assertEquals("after second", 10, file.length());
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    public void testWrite_2args()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            boolean append = false;
            int writerDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, writerDebugLevel);

            assertEquals("before", 0, file.length());

            instance.write("testString", file.getPath());
            instance.close();

            assertEquals("after", 10, file.length());
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    public void testFlush_int_int()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            boolean append = false;
            int writerDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, writerDebugLevel);

            int minDebugLevel = -10;
            int maxDebugLevel = -1;
            instance.write("testString", file.getPath());
            assertEquals("before", 0, file.length());

            instance.flush(minDebugLevel, maxDebugLevel);
            assertEquals("after too low", 0, file.length());

            minDebugLevel = 1;
            maxDebugLevel = 11;
            instance.flush(minDebugLevel, maxDebugLevel);
            assertEquals("after too high", 0, file.length());

            minDebugLevel = 0;
            maxDebugLevel = 0;
            instance.flush(minDebugLevel, maxDebugLevel);
            assertEquals("after flush", 10, file.length());
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    public void testFlush_0args()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            boolean append = false;
            int writerDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, writerDebugLevel);

            instance.write("testString", file.getPath());
            assertEquals("before", 0, file.length());

            instance.flush();
            assertEquals("after", 10, file.length());

        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test // (expected = IOException.class)
    public void testClose()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            boolean append = false;
            int writerDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, writerDebugLevel);

            instance.close();
            long timeBefore = System.currentTimeMillis();
            instance.write("testString", file.getPath());
            long timeLastCheckDiskSpace = instance.getTimeLastCheckDiskSpace();

            assertTrue(timeLastCheckDiskSpace >= timeBefore);
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }
}
