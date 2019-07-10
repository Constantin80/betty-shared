package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DebuggerTest {
    @Test
    void setDebugLevel() {
        Debugger instance = null;
        try {
            final int newValue = 5;
            instance = new Debugger();
            instance.setDebugLevel(newValue);
            assertEquals(newValue, instance.getDebugLevel());
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    void setEncryption() {
        Debugger instance = null;
        try {
            final int newValue = 5;
            instance = new Debugger();
            instance.setEncryption(newValue);
            assertEquals(newValue, instance.getEncryption());
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    void addWriter_6args()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final boolean append = false;
            final String charsetName = "UTF-8";
            final int bufferSize = 8192;
            final String id = "testId";
            final int minDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, charsetName, bufferSize, id, minDebugLevel);

            assertTrue(instance.write("testString", id));
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    void addWriter_4args()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final boolean append = false;
            final String id = "testId";
            final int minDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, id, minDebugLevel);

            assertTrue(instance.write("testString", id));
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    void addWriter_3args()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final boolean append = false;
            final int minDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, minDebugLevel);

            assertTrue(instance.write("testString", new File(fileName).getPath()));
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    void checkDiskSpace()
            throws Exception {
        //  "Less than 50Mb remain on disk. Reducing debugLevel." message is normal
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        final String changedFileName = Generic.tempFileName("test");
        try {
            final int debugLevel = 5;
            instance = new Debugger();
            instance.setDebugLevel(debugLevel);
            instance.addWriter(fileName, false, 0);
            instance.checkDiskSpace();
            assertEquals(debugLevel, instance.getDebugLevel(), "before");

            final Field mapField = Debugger.class.getDeclaredField("writersMap");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked") final Map<SynchronizedWriter, Integer> writersMap = (Map<SynchronizedWriter, Integer>) mapField.get(instance);
            final SynchronizedWriter writer = writersMap.keySet().iterator().next();

            final Field writerField = SynchronizedWriter.class.getDeclaredField("file");
            writerField.setAccessible(true);
            writerField.set(writer, new File(changedFileName));

            instance.checkDiskSpace();

            assertEquals(0, instance.getDebugLevel(), "after " + new File(changedFileName).getUsableSpace());
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
            new File(changedFileName).delete();
        }
    }

    @Test
    void enoughAvailableSpace()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final File file = new File(fileName);
            instance = new Debugger();
            instance.addWriter(fileName, false, 0);
            final long availableSpace = file.getUsableSpace();
//            logger.info("availableSpace = {}", availableSpace);

            boolean expResult = true;
            boolean result = instance.enoughAvailableSpace(availableSpace - Generic.MEGABYTE);
            assertEquals(expResult, result, "true");

            expResult = false;
            result = instance.enoughAvailableSpace(availableSpace + Generic.MEGABYTE);
            assertEquals(expResult, result, "false");
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    void getDebugLevel() {
        Debugger instance = null;
        try {
            instance = new Debugger();
            final int expResult = 0;
            final int result = instance.getDebugLevel();
            assertEquals(expResult, result);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    void getEncryption() {
        Debugger instance = null;
        try {
            instance = new Debugger();
            final int expResult = 0;
            final int result = instance.getEncryption();
            assertEquals(expResult, result);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    void getTimeLastCheckDiskSpace() {
        Debugger instance = null;
        try {
            instance = new Debugger();
            final long expResult = 0;
            final long result = instance.getTimeLastCheckDiskSpace();
            assertEquals(expResult, result);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    void timeLastCheckDiskSpaceStamp() {
        Debugger instance = null;
        try {
            instance = new Debugger();
            final long timeBefore = System.currentTimeMillis();
            instance.timeLastCheckDiskSpaceStamp();
            final long timeAfter = instance.getTimeLastCheckDiskSpace();
            assertTrue(timeAfter >= timeBefore);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    void write_3args()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final File file = new File(fileName);
            final boolean append = false;
            final int writerDebugLevel = 0;
            instance = new Debugger();

            instance.addWriter(fileName, append, writerDebugLevel);
            assertEquals(0, file.length(), "before");

            instance.write("testString", file.getPath(), 1);
            instance.flush();
            assertEquals(0, file.length(), "after first");

            instance.write("testString", file.getPath(), 0);
            instance.flush();
            assertEquals(10, file.length(), "after second");
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    void write_2args()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final File file = new File(fileName);
            final boolean append = false;
            final int writerDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, writerDebugLevel);

            assertEquals(0, file.length(), "before");

            instance.write("testString", file.getPath());
            instance.close();

            assertEquals(10, file.length(), "after");
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    void flush_int_int()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final File file = new File(fileName);
            final boolean append = false;
            final int writerDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, writerDebugLevel);

            int minDebugLevel = -10;
            int maxDebugLevel = -1;
            instance.write("testString", file.getPath());
            assertEquals(0, file.length(), "before");

            instance.flush(minDebugLevel, maxDebugLevel);
            assertEquals(0, file.length(), "after too low");

            minDebugLevel = 1;
            maxDebugLevel = 11;
            instance.flush(minDebugLevel, maxDebugLevel);
            assertEquals(0, file.length(), "after too high");

            minDebugLevel = 0;
            maxDebugLevel = 0;
            instance.flush(minDebugLevel, maxDebugLevel);
            assertEquals(10, file.length(), "after flush");
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    void flush_0args()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final File file = new File(fileName);
            final boolean append = false;
            final int writerDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, writerDebugLevel);

            instance.write("testString", file.getPath());
            assertEquals(0, file.length(), "before");

            instance.flush();
            assertEquals(10, file.length(), "after");

        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
        // (expected = IOException.class)
    void close()
            throws IOException {
        Debugger instance = null;
        final String fileName = Generic.tempFileName("test");
        try {
            final File file = new File(fileName);
            final boolean append = false;
            final int writerDebugLevel = 0;
            instance = new Debugger();
            instance.addWriter(fileName, append, writerDebugLevel);

            instance.close();
            final long timeBefore = System.currentTimeMillis();
            instance.write("testString", file.getPath());
            final long timeLastCheckDiskSpace = instance.getTimeLastCheckDiskSpace();

            assertTrue(timeLastCheckDiskSpace >= timeBefore);
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }
}
