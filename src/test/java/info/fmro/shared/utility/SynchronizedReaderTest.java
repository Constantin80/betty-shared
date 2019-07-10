package info.fmro.shared.utility;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SynchronizedReaderTest {
    @Test
    void readLine_0args()
            throws IOException {
        FileOutputStream fileOutputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        File testFile = null;
        SynchronizedReader instance = null;
        try {
            final String testFileName = Generic.tempFileName("test");
            final String expResult = "testString";
            testFile = new File(testFileName);
            fileOutputStream = new FileOutputStream(testFile, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            outputStreamWriter.write(expResult);
            Generic.closeObjects(outputStreamWriter, fileOutputStream);

            instance = new SynchronizedReader(testFile);
            final String result = instance.readLine();

            assertEquals(expResult, result);
        } finally {
            Generic.closeObjects(outputStreamWriter, fileOutputStream, instance);
            if (testFile != null) {
                testFile.delete();
            }
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
            final String testFileName = Generic.tempFileName("test");
            final String expResult = "testString";
            testFile = new File(testFileName);
            fileOutputStream = new FileOutputStream(testFile, false);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            outputStreamWriter.write(Generic.encryptString(expResult, 2));
            Generic.closeObjects(outputStreamWriter, fileOutputStream);

            instance = new SynchronizedReader(testFile);
            final String result = instance.readLine(2);

            assertEquals(expResult, result);
        } finally {
            Generic.closeObjects(outputStreamWriter, fileOutputStream, instance);
            if (testFile != null) {
                testFile.delete();
            }
        }
    }

    @Test
    void close() {
        assertThrows(IOException.class, () -> {
            FileOutputStream fileOutputStream = null;
            OutputStreamWriter outputStreamWriter = null;
            File testFile = null;
            SynchronizedReader instance = null;
            try {
                final String testFileName = Generic.tempFileName("test");
                final String expResult = "testString";
                testFile = new File(testFileName);
                fileOutputStream = new FileOutputStream(testFile, false);
                outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                outputStreamWriter.write(expResult);
                Generic.closeObjects(outputStreamWriter, fileOutputStream);

                instance = new SynchronizedReader(testFile);
                instance.close();
                instance.readLine();
            } finally {
                Generic.closeObjects(outputStreamWriter, fileOutputStream, instance);
                if (testFile != null) {
                    testFile.delete();
                }
            }
        });
    }
}
