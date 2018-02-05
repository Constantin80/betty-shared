package info.fmro.shared.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizedReaderTest
{
    private static final Logger logger = LoggerFactory.getLogger (SynchronizedReaderTest.class);

    public SynchronizedReaderTest ()
    {
    }
    @Rule
    @SuppressWarnings ("PublicField")
    public TestRule watchman = new TestWatcher ()
    {
        @Override
        public void starting (Description description)
        {
            logger.info ("{} being run...", description.getMethodName ());
        }
    };

    @Test
    public void testReadLine_0args ()
        throws IOException
    {
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        File testFile = null;
        SynchronizedReader instance = null;
        try {
            String testFileName = Generic.tempFileName ("test");
            String expResult = "testString";
            testFile = new File (testFileName);
            fileOutputStream = new FileOutputStream (testFile, false);
            outputStreamWriter = new OutputStreamWriter (fileOutputStream);
            outputStreamWriter.write (expResult);
            Generic.closeObjects (outputStreamWriter, fileOutputStream);

            instance = new SynchronizedReader (testFile);
            String result = instance.readLine ();

            assertEquals (expResult, result);
        } finally {
            Generic.closeObjects (outputStreamWriter, fileOutputStream, instance);
            testFile.delete ();
        }
    }

    @Test
    public void testReadLine_int ()
        throws IOException
    {
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        File testFile = null;
        SynchronizedReader instance = null;
        try {
            String testFileName = Generic.tempFileName ("test");
            String expResult = "testString";
            testFile = new File (testFileName);
            fileOutputStream = new FileOutputStream (testFile, false);
            outputStreamWriter = new OutputStreamWriter (fileOutputStream);
            outputStreamWriter.write (Generic.encryptString (expResult, 2));
            Generic.closeObjects (outputStreamWriter, fileOutputStream);

            instance = new SynchronizedReader (testFile);
            String result = instance.readLine (2);

            assertEquals (expResult, result);
        } finally {
            Generic.closeObjects (outputStreamWriter, fileOutputStream, instance);
            testFile.delete ();
        }
    }

    @Test (expected = IOException.class)
    public void testClose ()
        throws IOException
    {
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        File testFile = null;
        SynchronizedReader instance = null;
        try {
            String testFileName = Generic.tempFileName ("test");
            String expResult = "testString";
            testFile = new File (testFileName);
            fileOutputStream = new FileOutputStream (testFile, false);
            outputStreamWriter = new OutputStreamWriter (fileOutputStream);
            outputStreamWriter.write (expResult);
            Generic.closeObjects (outputStreamWriter, fileOutputStream);

            instance = new SynchronizedReader (testFile);
            instance.close ();
            instance.readLine ();
        } finally {
            Generic.closeObjects (outputStreamWriter, fileOutputStream, instance);
            testFile.delete ();
        }
    }
}
