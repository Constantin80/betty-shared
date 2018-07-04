package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DebuggerTest {
    @Test
    void setDebugLevel() {
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
    void setEncryption() {
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
    void addWriter_6args()
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
    void addWriter_4args()
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
    void addWriter_3args()
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
    void checkDiskSpace()
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
            assertEquals(debugLevel, instance.getDebugLevel(), "before");

            Field mapField = Debugger.class.getDeclaredField("writersMap");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            LinkedHashMap<SynchronizedWriter, Integer> writersMap = (LinkedHashMap<SynchronizedWriter, Integer>) mapField.get(instance);
            SynchronizedWriter writer = writersMap.keySet().iterator().next();

            Field writerField = SynchronizedWriter.class.getDeclaredField("file");
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
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            instance = new Debugger();
            instance.addWriter(fileName, false, 0);
            long availableSpace = file.getUsableSpace();
//            logger.info("availableSpace = {}", availableSpace);

            boolean expResult = true;
            boolean result = instance.enoughAvailableSpace(availableSpace - 1024 * 1024);
            assertEquals(expResult, result, "true");

            expResult = false;
            result = instance.enoughAvailableSpace(availableSpace + 1024 * 1024);
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
            int expResult = 0;
            int result = instance.getDebugLevel();
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
            int expResult = 0;
            int result = instance.getEncryption();
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
            long expResult = 0;
            long result = instance.getTimeLastCheckDiskSpace();
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
            long timeBefore = System.currentTimeMillis();
            instance.timeLastCheckDiskSpaceStamp();
            long timeAfter = instance.getTimeLastCheckDiskSpace();
            assertTrue(timeAfter >= timeBefore);
        } finally {
            Generic.closeObject(instance);
        }
    }

    @Test
    void write_3args()
            throws IOException {
        Debugger instance = null;
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            boolean append = false;
            int writerDebugLevel = 0;
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
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            boolean append = false;
            int writerDebugLevel = 0;
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
        String fileName = Generic.tempFileName("test");
        try {
            File file = new File(fileName);
            boolean append = false;
            int writerDebugLevel = 0;
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
