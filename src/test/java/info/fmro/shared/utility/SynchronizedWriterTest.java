package info.fmro.shared.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import junitx.framework.FileAssert;
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

public class SynchronizedWriterTest {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedWriterTest.class);

    public SynchronizedWriterTest() {
    }

    @Rule
    @SuppressWarnings("PublicField")
    public TestRule watchman = new TestWatcher() {
        @Override
        public void starting(Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };

    // @Test
    // public void testInitialize ()
    //     throws java.io.FileNotFoundException
    // {
    //     File file = null;
    //     boolean append = false;
    //     Charset charset = null;
    //     int bufferSize = 0;
    //     String id = "";
    //     SynchronizedWriter instance = null;
    //     instance.initialize (file, append, charset, bufferSize, id);
    //     fail ("The test case is a prototype.");
    // }
    @Test
    public void testGetId()
            throws FileNotFoundException {
        String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        try {
            String expResult = "testID";
            instance = new SynchronizedWriter(fileName, true, expResult);
            String result = instance.getId();
            assertEquals(expResult, result);
        } finally {
            Generic.closeObject(instance);
            new File(fileName).delete();
        }
    }

    @Test
    public void testGetUsableSpace()
            throws FileNotFoundException {
        String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        File file = null;
        try {
            file = new File(fileName);
            instance = new SynchronizedWriter(file, true);
            long expResult = file.getUsableSpace();
            long result = instance.getUsableSpace();
            long difference = Math.abs(expResult - result);
            logger.info("expResult = {}  result = {}", expResult, result);

            assertTrue(difference < 1024 * 1024);
        } finally {
            Generic.closeObject(instance);
            file.delete();
        }
    }

    @Test
    public void testWriteAndFlush()
            throws IOException {
        String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        File file = null;
        try {
            file = new File(fileName);
            instance = new SynchronizedWriter(file, false);
            instance.writeAndFlush("testString");

            assertEquals(10, file.length());
        } finally {
            Generic.closeObject(instance);
            file.delete();
        }
    }

    @Test
    public void testWrite_String()
            throws IOException {
        SynchronizedWriter instance = null;
        File file = null, testFile = null;
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fileOutputStream = null;
        try {
            String fileName = Generic.tempFileName("test");
            file = new File(fileName);
            instance = new SynchronizedWriter(file, false);
            instance.write("testString");
            Generic.closeObject(instance);

            String testFileName = Generic.tempFileName("test");
            testFile = new File(testFileName);
            fileOutputStream = new FileOutputStream(testFile, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write("testString");
            Generic.closeObjects(outputStreamWriter, fileOutputStream);

            FileAssert.assertBinaryEquals(testFile, file);
        } finally {
            Generic.closeObjects(instance, outputStreamWriter, fileOutputStream);
            file.delete();
            testFile.delete();
        }
    }

//    @Test
//    public void testWrite_String2()
//            throws IOException {
//        SynchronizedWriter instance = null;
//        File file = null, testFile = null;
//        OutputStreamWriter outputStreamWriter = null;
//        FileOutputStream fileOutputStream = null;
//        try {
//            String fileName = Generic.tempFileName("test");
//            file = new File(fileName);
//            instance = new SynchronizedWriter(file, false);
////            instance.setCharset(Generic.UTF8_CHARSET);
//            instance.write("üá");
//            Generic.closeObject(instance);
//
//            String testFileName = Generic.tempFileName("test");
//            testFile = new File(testFileName);
//            fileOutputStream = new FileOutputStream(testFile, false);
//            outputStreamWriter = new OutputStreamWriter(fileOutputStream);
//            outputStreamWriter.write("Ã¼Ã¡");
//            Generic.closeObjects(outputStreamWriter, fileOutputStream);
//
//            FileAssert.assertBinaryEquals(testFile, file);
//        } finally {
//            Generic.closeObjects(instance, outputStreamWriter, fileOutputStream);
//            file.delete();
//            testFile.delete();
//        }
//    }
    @Test
    public void testWrite_String_int()
            throws IOException {
        SynchronizedWriter instance = null;
        File file = null, testFile = null;
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fileOutputStream = null;
        try {
            String fileName = Generic.tempFileName("test");
            file = new File(fileName);
            instance = new SynchronizedWriter(file, false);
            instance.write("testString", 2);
            Generic.closeObject(instance);

            String testFileName = Generic.tempFileName("test");
            testFile = new File(testFileName);
            fileOutputStream = new FileOutputStream(testFile, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write(Generic.encryptString("testString", 2));
            Generic.closeObjects(outputStreamWriter, fileOutputStream);

            FileAssert.assertBinaryEquals(testFile, file);
        } finally {
            Generic.closeObjects(instance, outputStreamWriter, fileOutputStream);
            file.delete();
            testFile.delete();
        }
    }

    @Test
    public void testFlush()
            throws IOException {
        String fileName = Generic.tempFileName("test");
        SynchronizedWriter instance = null;
        File file = null;
        try {
            file = new File(fileName);
            instance = new SynchronizedWriter(file, false);
            instance.write("testString");

            assertEquals("before", 0, file.length());
            instance.flush();
            assertEquals("after", 10, file.length());
        } finally {
            Generic.closeObject(instance);
            file.delete();
        }
    }

    // @Test(expected = IOException.class)
    @Test
    public void testClose()
            throws IOException {
        String fileName = Generic.tempFileName("test");
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
