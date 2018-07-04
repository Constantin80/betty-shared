package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SynchronizedReaderTest {
    public SynchronizedReaderTest() {
    }

  @Test
    void readLine_0args()
            throws IOException {
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        File testFile = null;
        SynchronizedReader instance = null;
        try {
            String testFileName = Generic.tempFileName("test");
            String expResult = "testString";
            testFile = new File(testFileName);
            fileOutputStream = new FileOutputStream(testFile, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write(expResult);
            Generic.closeObjects(outputStreamWriter, fileOutputStream);

            instance = new SynchronizedReader(testFile);
            String result = instance.readLine();

            assertEquals(expResult, result);
        } finally {
            Generic.closeObjects(outputStreamWriter, fileOutputStream, instance);
            testFile.delete();
        }
    }

  @Test
    void readLine_int()
            throws IOException {
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        File testFile = null;
        SynchronizedReader instance = null;
        try {
            String testFileName = Generic.tempFileName("test");
            String expResult = "testString";
            testFile = new File(testFileName);
            fileOutputStream = new FileOutputStream(testFile, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.write(Generic.encryptString(expResult, 2));
            Generic.closeObjects(outputStreamWriter, fileOutputStream);

            instance = new SynchronizedReader(testFile);
            String result = instance.readLine(2);

            assertEquals(expResult, result);
        } finally {
            Generic.closeObjects(outputStreamWriter, fileOutputStream, instance);
            testFile.delete();
        }
    }

  @Test
    void close()
            throws IOException {
        assertThrows(IOException.class, () -> {
            FileOutputStream fileOutputStream = null;
            OutputStreamWriter outputStreamWriter = null;
            File testFile = null;
            SynchronizedReader instance = null;
            try {
                String testFileName = Generic.tempFileName("test");
                String expResult = "testString";
                testFile = new File(testFileName);
                fileOutputStream = new FileOutputStream(testFile, false);
                outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                outputStreamWriter.write(expResult);
                Generic.closeObjects(outputStreamWriter, fileOutputStream);

                instance = new SynchronizedReader(testFile);
                instance.close();
                instance.readLine();
            } finally {
                Generic.closeObjects(outputStreamWriter, fileOutputStream, instance);
                testFile.delete();
            }
        });
    }
}
