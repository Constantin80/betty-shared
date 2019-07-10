package info.fmro.shared.utility;

import junitx.framework.FileAssert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SynchronizedWriterTest {
    @Test
    void getId()
            throws FileNotFoundException {
        final String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        try {
            final String expResult = "testID";
            instance = new SynchronizedWriter(fileName, true, expResult);
            final String result = instance.getId();
            assertEquals(expResult, result);
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    void getUsableSpace()
            throws FileNotFoundException {
        final String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        File file = null;
        try {
            file = new File(fileName);
            instance = new SynchronizedWriter(file, true);
            final long expResult = file.getUsableSpace();
            final long result = instance.getUsableSpace();
            final long difference = Math.abs(expResult - result);
//            logger.info("expResult = {}  result = {}", expResult, result);

            assertTrue(difference < Generic.MEGABYTE);
        } finally {
            Generic.closeObject(instance);
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    void writeAndFlush()
            throws IOException {
        final String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        File file = null;
        try {
            file = new File(fileName);
            instance = new SynchronizedWriter(file, false);
            instance.writeAndFlush("testString");

            assertEquals(10, file.length());
        } finally {
            Generic.closeObject(instance);
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    void write_String()
            throws IOException {
        SynchronizedWriter instance = null;
        File file = null, testFile = null;
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fileOutputStream = null;
        try {
            final String fileName = Generic.tempFileName("test");
            file = new File(fileName);
            instance = new SynchronizedWriter(file, false);
            instance.write("testString");
            Generic.closeObject(instance);

            final String testFileName = Generic.tempFileName("test");
            testFile = new File(testFileName);
            fileOutputStream = new FileOutputStream(testFile, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            outputStreamWriter.write("testString");
            Generic.closeObjects(outputStreamWriter, fileOutputStream);

            FileAssert.assertBinaryEquals(testFile, file);
        } finally {
            Generic.closeObjects(instance, outputStreamWriter, fileOutputStream);
            if (file != null) {
                file.delete();
            }
            if (testFile != null) {
                testFile.delete();
            }
        }
    }

    @Test
    void write_String_int()
            throws IOException {
        SynchronizedWriter instance = null;
        File file = null, testFile = null;
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fileOutputStream = null;
        try {
            final String fileName = Generic.tempFileName("test");
            file = new File(fileName);
            instance = new SynchronizedWriter(file, false);
            instance.write("testString", 2);
            Generic.closeObject(instance);

            final String testFileName = Generic.tempFileName("test");
            testFile = new File(testFileName);
            fileOutputStream = new FileOutputStream(testFile, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            outputStreamWriter.write(Generic.encryptString("testString", 2));
            Generic.closeObjects(outputStreamWriter, fileOutputStream);

            FileAssert.assertBinaryEquals(testFile, file);
        } finally {
            Generic.closeObjects(instance, outputStreamWriter, fileOutputStream);
            if (file != null) {
                file.delete();
            }
            if (testFile != null) {
                testFile.delete();
            }
        }
    }

    @Test
    void flush()
            throws IOException {
        final String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        File file = null;
        try {
            file = new File(fileName);
            instance = new SynchronizedWriter(file, false);
            instance.write("testString");

            assertEquals(0, file.length(), "before");
            instance.flush();
            assertEquals(10, file.length(), "after");
        } finally {
            Generic.closeObject(instance);
            if (file != null) {
                file.delete();
            }
        }
    }

    @Test
    void close()
            throws IOException {
        final String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        try {
            instance = new SynchronizedWriter(fileName, true);
            instance.close();
            assertFalse(instance.write("testString"));
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }
}
