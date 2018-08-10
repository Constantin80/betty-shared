package info.fmro.shared.utility;

import junitx.framework.FileAssert;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericTest {
    private class LocalDebugger
            extends Debugger {
    }

    public static class AdditionalLocalTestObject
            extends LocalTestObject {
        private String id = "objectId";

        public AdditionalLocalTestObject() {
        }

        public String getId() {
            return this.id;
        }
    }

    public static class LocalTestObject {
        private static final long serialVersionUID = 1221L;
        private String name = "";
        private int number;

        public LocalTestObject() {
        }

        public LocalTestObject(String name, int number) {
            this.name = name;
            this.number = number;
        }

        public String getName() {
            return this.name;
        }

        public int getNumber() {
            return this.number;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof LocalTestObject)) {
                return false;
            }

            LocalTestObject localTestObject = (LocalTestObject) object;

            return this.name.equals(localTestObject.getName()) && this.number == localTestObject.getNumber();
        }

        @Override
        public int hashCode() {
            final int HASH_PRIME = 31;
            int result = 0;

            result = HASH_PRIME * result + this.name.hashCode();
            result = HASH_PRIME * result + this.number;

            return result;
        }
    }

    GenericTest() {
    }

    @Test
    void getMiddleIndex() {
        int expected = 7;
        int result = Generic.getMiddleIndex("SGI/SSK/SGI/SSK", "/");
        assertEquals(expected, result, "1");

        expected = 3;
        result = Generic.getMiddleIndex("SGI/SGI", "/");
        assertEquals(expected, result, "2");
    }

    @Test
    void createAndFill()
            throws InvocationTargetException, InstantiationException, IllegalAccessException {
        LocalTestObject result = new LocalTestObject();
        assertEquals("", result.getName(), "default");
        result = Generic.createAndFill(LocalTestObject.class);
        assertNotEquals("", result.getName(), "random");
        result = Generic.createAndFill(LocalTestObject.class, 10);
        assertEquals(null, result.getName(), "recursionCounter==10");
    }

    @Test
    void fillRandom()
            throws InvocationTargetException, InstantiationException, IllegalAccessException {
        LocalTestObject result = new LocalTestObject();
        assertEquals("", result.getName(), "default");
        Generic.fillRandom(result);
        assertNotEquals("", result.getName(), "random");
        Generic.fillRandom(result, 10);
        assertEquals(null, result.getName(), "recursionCounter==10");
    }

    @Test
    void getRandomValueForField()
            throws IllegalAccessException, NoSuchFieldException, InstantiationException, InvocationTargetException {
        LocalTestObject testObject = new LocalTestObject();
        Field field = LocalTestObject.class.getDeclaredField("name");
        field.setAccessible(true);
        assertEquals("", field.get(testObject), "default");

        Object result = Generic.getRandomValueForField(field, 0);
        assertNotEquals("", result, "random");
        assertEquals(String.class, field.getType(), "type");
        result = Generic.getRandomValueForField(field, 10);
        assertEquals(null, result, "recursionCounter==10");
    }

    @Test
    void createStringFromCodes() {
        String result = Generic.createStringFromCodes(32, 32, 32);
        assertEquals(result, "   ", "1");

        result = Generic.createStringFromCodes(32, 32, 0);
        assertEquals(result, "  \u0000", "2");
    }

    @Test
    void getStringCodePointValues() {
        String symbol_194_160 = (new StringBuilder(2)).append('\u00c2').append('\u00a0').toString();
        String result = Generic.getStringCodePointValues(symbol_194_160);
        assertEquals(result, "194 160");
    }

    @Test
    void properTimeStamp() {
        String value = Generic.properTimeStamp();

        assertEquals(value.length(), 23);
    }

    @Test
    void properTimeStamp_long() {
        String value = Generic.properTimeStamp(500L);

        assertEquals(value.length(), 23);

        value = Generic.properTimeStamp(0L);

        assertEquals(value.length(), 23);
    }

    @Test
    void getSubclasses() {
        Set<Class<? extends Debugger>> set = Generic.getSubclasses("info.fmro", Debugger.class);
        Set<Class<? extends Debugger>> expected = new HashSet<>(0);
        expected.add(LocalDebugger.class);

        assertEquals(expected, set);
    }

    @Test
    void collectionKeepMultiples() {
        ArrayList<Integer> list = new ArrayList<>(5);
        list.add(1);
        list.add(2);
        list.add(2);
        list.add(3);
        list.add(3);
        list.add(3);
        list.add(4);
        ArrayList<Integer> expected = new ArrayList<>(3);
        expected.add(2);
        expected.add(3);
        expected.add(3);
        Generic.collectionKeepMultiples(list, 2);
        assertEquals(expected, list);
    }

    @Test
    void isPowerOfTwo() {
        boolean result = Generic.isPowerOfTwo(0L);
        assertFalse(result);

        result = Generic.isPowerOfTwo(2L);
        assertTrue(result);

        result = Generic.isPowerOfTwo(8589934592L);
        assertTrue(result);

        result = Generic.isPowerOfTwo(8589934591L);
        assertFalse(result);
    }

    @Test
    void stringBuilderReplace() {
        StringBuilder stringBuilder = new StringBuilder("abcd");
        String result = "xyz";
        Generic.stringBuilderReplace(stringBuilder, result);
        String expResult = result;
        assertEquals(expResult, stringBuilder.toString());
    }

    @Test
    void middleValue_double() {
        double a = 3.62378947368421d, b = 1.3231d, c = 3.623789473684211d, result, expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertTrue(expResult == result);
    }

    @Test
    void middleValue_float() {
        float a = 3.62378947368421f, b = 3.3231f, c = 3.623789473684211f, result, expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertTrue(expResult == result);
    }

    @Test
    void middleValue_int() {
        int a = 3, b = 1, c = 1, result, expResult;

        result = Generic.middleValue(a, b, c);
        expResult = c;
        assertTrue(expResult == result);
    }

    @Test
    void middleValue_long() {
        long a = 3L, b = 2L, c = 3L, result, expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertTrue(expResult == result);
    }

    @Test
    void truncateDouble() {
        Double size = 3.62378947368421d;
        double result = Generic.truncateDouble(size, 2);
        double expResult = 3.62d;
        assertTrue(expResult == result, "1");

        size = 3.62978947368421d;
        result = Generic.truncateDouble(size, 2);
        expResult = 3.62d;
        assertTrue(expResult == result, "2");
    }

    @Test
    void quotedReplaceAll() {
        String string = "abc'-&  u. ", pattern = ". ", replacement = " ", expResult = "abc'-&  u ";
        String result = Generic.quotedReplaceAll(string, pattern, replacement);
        assertTrue(result.equals(expResult), "first");

        string = " ";
        pattern = " ";
        replacement = "";
        expResult = ""; // it's a special character, non breaking space 194+160
        result = Generic.quotedReplaceAll(string, pattern, replacement);
        assertTrue(result.equals(expResult), "second");
    }

    @Test
    void stringMatchChance_String_String() {
        String first = "", second = "";
        double expResult = 0;
        double result = Generic.stringMatchChance(first, second);
        assertTrue(expResult == result, "first");

        first = null;
        second = null;
        expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(expResult == result, "second");

        first = "Ab ";
        second = "   ab";
        expResult = 1;
        result = Generic.stringMatchChance(first, second);
        assertTrue(expResult == result, "third");

        first = "a";
        second = "b";
        expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(expResult == result, "fourth");

        first = "abcdefghijklmnopqrstuvwxyz0123456789";
        second = "hdfkladhlaeueuqrlahfasddshladshsjanj";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .2, "fifth");

        first = "Turin";
        second = "Torino";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result > .8, "sixth");

        first = "Al Khor";
        second = "Al Khoor";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result > .92, "seventh");

        first = "Al Jahra";
        second = "Al Tadamon";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .7, "eighth");

        first = "AL Shamal";
        second = "Al Kharaityat";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .7, "ninth");

        first = "Club Social DYC Espanol";
        second = "Grasshopper Club";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .7, "10th");

        first = "Real Cartagena";
        second = "Real Sociedad";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .7, "11th");

        first = "AC Sparta Praha U21";
        second = "Sparta Prague";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .8, "12th");

        first = "MAS Taborsko U21";
        second = "AEL Limassol";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .5, "13th");

        first = "AC Sparta Praha U21";
        second = "Spartaks";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .8, "14th");

        first = "Platense";
        second = "CA River Plate";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .75, "15th");

        first = "Real Cartagena";
        second = "Real Espana";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .8, "16th");

        first = "America de Cali";
        second = "Uni de Costa Rica";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .6, "17th");

        first = "Ajaccio GFCO";
        second = "GFCO Ajaccio";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result > .9, "18th");

        first = "Los Angeles Galaxy II";
        second = "Los Angeles Galaxy";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result > .9, "19th");

        first = "Hapoel Haifa";
        second = "Hapoel Tel Aviv";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result < .8, "20th");

        first = "ASO Chlef";
        second = "ASO CHIEF";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue(result > .9, "21st");
    }

    @Test
    void stringMatchChance_String_String_boolean() {
        String first = "a", second = "A";
        double expResult = 0;
        double result = Generic.stringMatchChance(first, second, false);
        assertTrue(expResult == result, "first");

        first = "A";
        second = "A ";
        expResult = 1;
        result = Generic.stringMatchChance(first, second, false);
        assertTrue(expResult == result, "second");
    }

    @Test
    void stringMatchChance_String_String_boolean_boolean() {
        String first = " ", second = " ";
        double expResult = 1;
        double result = Generic.stringMatchChance(first, second, true, false);
        assertTrue(expResult == result, "first");

        first = "a";
        second = "a ";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second, true, false);
        assertTrue(result < 1 && result > .5, "second");
    }

    @Test
    void getCollectionCapacity() {
        float loadFactor = 0.75f;
        int size = 43;
        int result = Generic.getCollectionCapacity(size, loadFactor);
        int expResult = 64;

        assertEquals(expResult, result, "first");

        loadFactor = 0.75f;
        size = 1;
        result = Generic.getCollectionCapacity(size, loadFactor);
        expResult = 2;

        assertEquals(expResult, result, "second");

        loadFactor = 0.5f;
        size = 88;
        result = Generic.getCollectionCapacity(size, loadFactor);
        expResult = 256;

        assertEquals(expResult, result, "third");

        loadFactor = 0.75f;
        size = 3;
        result = Generic.getCollectionCapacity(size, loadFactor);
        expResult = 4;

        assertEquals(expResult, result, "fourth");
    }

    // Test of printStackTraces method, of class Generic.
    @Test
    void printStackTraces_String() {
        assertTimeout(Duration.ofMillis(10000), () -> {
            String fileName = Generic.tempFileName("test");
            File file = new File(fileName);
            try {
                Generic.printStackTraces(fileName);
                assertTrue(file.length() > 0);
            } finally {
                file.delete();
            }
        });
    }

    // Test of printStackTraces method, of class Generic.
    @Test
    void printStackTraces_PrintStream() {
        ByteArrayOutputStream byteArrayOutputStream = null;
        PrintStream printStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            printStream = new PrintStream(byteArrayOutputStream);
            Generic.printStackTraces(printStream);
        } finally {
            Generic.closeObjects(byteArrayOutputStream, printStream);
        }

        assertTrue(byteArrayOutputStream.size() > 0);
    }

    // Test of printStackTrace method, of class Generic.
    @Test
    void printStackTrace_StackTraceElementArr_String() {
        Map<Thread, StackTraceElement[]> stacksMap = Thread.getAllStackTraces();
        StackTraceElement[] stackTraceElementArray = stacksMap.values().iterator().next();
        String fileName = Generic.tempFileName("test");
        File file = new File(fileName);

        try {
            Generic.printStackTrace(stackTraceElementArray, fileName);

            assertTrue(file.length() > 0);
        } finally {
            file.delete();
        }
    }

    // Test of printStackTrace method, of class Generic.
    @Test
    @SuppressWarnings("null")
    public void printStackTrace_StackTraceElementArr_PrintStream() {
        Map<Thread, StackTraceElement[]> stacksMap = Thread.getAllStackTraces();
        StackTraceElement[] stackTraceElementArray = stacksMap.values().iterator().next();
        ByteArrayOutputStream byteArrayOutputStream = null;
        PrintStream printStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            printStream = new PrintStream(byteArrayOutputStream);
            Generic.printStackTrace(stackTraceElementArray, printStream);
        } finally {
            Generic.closeObjects(byteArrayOutputStream, printStream);
        }

        assertTrue(byteArrayOutputStream.size() > 0);
    }

    // Test of closeObjects method, of class Generic.
    @Test
    void closeObjects() {
        boolean[] expResult = new boolean[]{true, true, true, true};
        boolean[] result = Generic.closeObjects(new Socket(), new Socket(), new Socket(), new Socket());
        assertTrue(Arrays.equals(expResult, result));
    }

    // Test of setSoLinger method, of class Generic.
    @Test
    void setSoLinger()
            throws SocketException {
        Socket socket = new Socket();
        try {
            boolean expResult = true;
            boolean result = Generic.setSoLinger(socket);

            assertEquals(expResult, result, "testSetSoLinger boolean result");
            assertEquals(0, socket.getSoLinger(), "testSetSoLinger setSoLinger");
        } finally {
            Generic.closeObject(socket);
        }
    }

    // Test of closeObject method, of class Generic.
    @Test
    void closeObject() {
        Socket socket = new Socket();
        try {
            boolean expResult = true;
            boolean result = Generic.closeObject(socket);

            assertEquals(expResult, result, "testCloseObject boolean result");
            assertTrue(socket.isClosed(), "testCloseObject close");
        } finally {
            Generic.closeObject(socket);
        }
    }

    // Test of concatByte method, of class Generic.
    @Test
    void concatByte() {
        // endIndexA & endIndexB are excluded
        byte[] a = new byte[55];
        int startIndexA = -1;
        int endIndexA = 53;
        byte[] b = new byte[50];
        int startIndexB = 11;
        int endIndexB = 47;
        a[1] = 2;
        b[45] = 1;
        byte[] expResult = new byte[53 - 0 + 47 - 11];
        expResult[1] = 2;
        expResult[expResult.length - 2] = 1;
        byte[] result = Generic.concatByte(a, startIndexA, endIndexA, b, startIndexB, endIndexB);

        assertArrayEquals(expResult, result);
    }

    // Test of encryptString method, of class Generic.
    @Test
    void encryptString() {
        String string = "abcE";
        int encryptKey = 3;
        String expResult = "defH";
        String result = Generic.encryptString(string, encryptKey);

        assertEquals(expResult, result);
    }

    // Test of encryptFile method, of class Generic.
    @Test
    void encryptFile()
            throws IOException {
        String initialString = "abc\r\nfgh";
        int encryptKey = 3;
        String expResult = "def\r\nijk\r\n", result = "";
        SynchronizedWriter synchronizedWriter = null;
        SynchronizedReader synchronizedReader = null;
        String fileName = Generic.tempFileName("testEncryptFile");
        boolean success = false;

        try {
            synchronizedWriter = new SynchronizedWriter(fileName, false);
            synchronizedWriter.write(initialString);
            synchronizedWriter.flush();
            Generic.closeObject(synchronizedWriter);

            success = Generic.encryptFile(fileName, encryptKey);

            synchronizedReader = new SynchronizedReader(fileName);
            String fileLine = synchronizedReader.readLine();
            while (fileLine != null) {
                result += fileLine + "\r\n";
                fileLine = synchronizedReader.readLine();
            } // end while
        } finally {
            Generic.closeObjects(synchronizedWriter, synchronizedReader);
            new File(fileName).delete();
        }

        assertTrue(success);
        assertEquals(expResult, result);
    }

    // Test of tempFileName method, of class Generic.
    @Test
    void tempFileName() {
        String fileName = "test";
        String result = Generic.tempFileName(fileName);
        String result2 = Generic.tempFileName(fileName);

        assertFalse(new File(result).exists(), "exists");
        assertFalse(result.equals(result2), "equals");
    }

    // Test of readObjectFromFile, writeObjectToFile, synchronizedWriteObjectToFile methods, of class Generic.
    @Test
    void readWriteObjectFromFile() {
        String fileName = Generic.tempFileName("test");
        int[] intArray = new int[]{1, 2, 5, 1, 3213, 6554211, 132312312};
        try {
            Generic.writeObjectToFile(intArray, fileName);
            int[] resultIntArray = (int[]) Generic.readObjectFromFile(fileName);

            assertArrayEquals(intArray, resultIntArray, "unsynchronized");

            Generic.synchronizedWriteObjectToFile(intArray, fileName, false);
            resultIntArray = (int[]) Generic.readObjectFromFile(fileName);

            assertArrayEquals(intArray, resultIntArray, "synchronized");
        } finally {
            new File(fileName).delete();
        }
    }

    // Test of getHexString method, of class Generic.
    @Test
    void getHexString() {
        byte[] byteArray = new byte[]{12, 22, 111, 2, 0, -12};
        String expResult = "0c166f0200f4";
        String result = Generic.getHexString(byteArray);

        assertEquals(expResult, result);
    }

    @Test
    void backwardWordsString() {
        String string = "Ajaccio GFCO";
        String expResult = "GFCO Ajaccio";
        String result = Generic.backwardWordsString(string);

        assertEquals(expResult, result, "1");

        string = " first  second third";
        expResult = "third second  first ";
        result = Generic.backwardWordsString(string);

        assertEquals(expResult, result, "2");
    }

    // Test of backwardString method, of class Generic.
    @Test
    void backwardString() {
        String string = "test123";
        String expResult = "321tset";
        String result = Generic.backwardString(string);

        assertEquals(expResult, result);
    }

    // Test of trimIP method, of class Generic.
    @Test
    void trimIP() {
        String IP = "012.001.000.123";
        String expResult = "12.1.0.123";
        String result = Generic.trimIP(IP);

        assertEquals(expResult, result, "real");

        IP = "dasdsadasd";
        expResult = "dasdsadasd";
        result = Generic.trimIP(IP);

        assertEquals(expResult, result, "bogus");
    }

    // Test of goodPort method, of class Generic.
    @Test
    void goodPort() {
        String tempPort = "ujklljkh";
        boolean expResult = false;
        boolean result = Generic.goodPort(tempPort);

        assertEquals(expResult, result, "bogus");

        tempPort = "1234";
        expResult = true;
        result = Generic.goodPort(tempPort);

        assertEquals(expResult, result, "good");

        tempPort = "65536";
        expResult = false;
        result = Generic.goodPort(tempPort);

        assertEquals(expResult, result, "bad, zero");

        tempPort = "43894344";
        expResult = true;
        result = Generic.goodPort(tempPort);

        assertEquals(expResult, result, "good, large number");
    }

    // Test of goodDomain method, of class Generic.
    @Test
    void goodDomain() {
        String host = "google.com";
        boolean expResult = true;
        boolean result = Generic.goodDomain(host);

        assertEquals(expResult, result, "good");

        host = "de-dsd_dasd-dsLd.ro";
        expResult = true;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "good, bogus name");

        host = null;
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "null");

        host = "";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "empty string");

        host = "de-dsdasdasd-dsad.ro" + (char) 244;
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, not pure ASCII");

        host = "de-dsdasdasd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, no period");

        host = "de-dsdasdasd-dsadro.";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, ends with period");

        host = "de-dsdasd/asd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, contains slash");

        host = "de-dsdasd?asd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, contains question mark");

        host = " de-dsdasdasd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, contains space");

        host = "de-dsd:sdasd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, disallowed character");

        host = "1234567890abcdefghjk1234.567890abcdefghjk1234567890a.bcdefghjk1234567890abcdefghjk12345.67890abcdefghjk1234567890abcde.fghjk1234567890abcdefghjk12.34567890abcdef" +
               "ghjk123.4567890abcdefghjk.1234567890abcdefghjk123.4567890abcdefghjk123456789.hjk123456789.com";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, size 254");

        host = "234567890abcdefghjk1234.567890abcdefghjk1234567890a.bcdefghjk1234567890abcdefghjk12345.67890abcdefghjk1234567890abcde.fghjk1234567890abcdefghjk12.34567890abcdefg" +
               "hjk123.4567890abcdefghjk.1234567890abcdefghjk123.4567890abcdefghjk123456789.hjk123456789.com";
        expResult = true;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "good, size 253");

        host = "1234567890abcdefghjk1234567890abcdefghjk1234567890abcdefghjk1234.567890abcdefghjk12345.67890abcdefghjk1234567890abcde.fghjk1234567890abcdefghjk12.34567890abcdefg" +
               "hjk123.4567890abcdefghjk.1234567890abcdefghjk123.4567890abcdefghjk123456789.hjk123456789.com";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, label size 64");

        host = "1234567890abcdefghjk1234567890abcdefghjk1234567890abcdefghjk123.4567890abcdefghjk12345.67890abcdefghjk1234567890abcde.fghjk1234567890abcdefghjk12.34567890abcdefg" +
               "hjk123.4567890abcdefghjk.1234567890abcdefghjk123.4567890abcdefghjk123456789.hjk123456789.com";
        expResult = true;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "good, label size 63");

        host = "xxx.tom";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad TLD");

        host = "209.191.122.70";
        expResult = true;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "good IP");

        host = "123.1.1.1.1";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, bogus IP");

        host = "225.191.122.70";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, reserved range IP");

        // a NumberFormatException is expected here
        host = "222718927192871289715.191.122.70";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals(expResult, result, "bad, IP parseInt exception");
    }

    // Test of getUserAgent method, of class Generic.
    @Test
    void getUserAgent() {
        String result = Generic.getUserAgent();

        assertTrue(result.startsWith("Mozilla/"), "startsWith");
        assertTrue(result.endsWith(")"), "endsWith");
    }

    // Test of getSocksType method, of class Generic.
    @Test
    void getSocksType() {
        String proxyType = "soCks4";
        byte expResult = 4;
        byte result = Generic.getSocksType(proxyType);

        assertEquals(expResult, result, "socks4");

        proxyType = "soCKs5";
        expResult = 5;
        result = Generic.getSocksType(proxyType);

        assertEquals(expResult, result, "socks5");

        // logger.warn is expected
        proxyType = "dhlskadh89sadnjm,n908l,.,";
        expResult = 5;
        result = Generic.getSocksType(proxyType);

        assertEquals(expResult, result, "bogus");
    }

    // Test of linkRemoveProtocol method, of class Generic.
    @Test
    void linkRemoveProtocol() {
        String link = "http://dsdsa.com:80/?query=1";
        String expResult = "dsdsa.com:80/?query=1";
        String result = Generic.linkRemoveProtocol(link);

        assertEquals(expResult, result);
    }

    // Test of linkRemovePort method, of class Generic.
    @Test
    void linkRemovePort() {
        String link = "http://dsdsa.com:80/?query=1";
        String expResult = "http://dsdsa.com/?query=1";
        String result = Generic.linkRemovePort(link);

        assertEquals(expResult, result);
    }

    // Test of linkRemoveQuery method, of class Generic.
    @Test
    void linkRemoveQuery() {
        String link = "http://dsdsa.com:80/?query=1";
        String expResult = "http://dsdsa.com:80/";
        String result = Generic.linkRemoveQuery(link);

        assertEquals(expResult, result);
    }

    // Test of getLinkHost method, of class Generic.
    @Test
    void getLinkHost() {
        String link = "http://dsDsa.com:80/?query=1";
        String expResult = "dsdsa.com";
        String result = Generic.getLinkHost(link);

        assertEquals(expResult, result, "good");

        link = "http://dsDsa.tom:80/?query=1";
        result = Generic.getLinkHost(link);

        assertNull(result, "bad TLD");

        link = null;
        result = Generic.getLinkHost(link);

        assertNull(result, "null");

        link = "/?query=1";
        result = Generic.getLinkHost(link);

        assertNull(result, "no host");

        link = "http://dsDsa.com:80";
        expResult = "dsdsa.com";
        result = Generic.getLinkHost(link);

        assertEquals(expResult, result, "domain only");
    }

    // Test of linkMatches method, of class Generic.
    @Test
    void linkMatches() {
        String path = "http://goOgle.com:80";
        String checkedLink = "https://gooGle.com/?query=1";
        boolean expResult = true;
        boolean result = Generic.linkMatches(path, checkedLink);
        assertEquals(expResult, result, "good, identical, protocol/port/query/domain_case ignored");

        // logger.warn is expected
        path = "google.como/";
        checkedLink = "google.como/";
        expResult = false;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals(expResult, result, "bad host");

        path = "google.com";
        checkedLink = "https://www.gooGle.com:8080/abcd/x.html?query=1";
        expResult = true;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals(expResult, result, "good, same domain or a subdomain");

        path = "google.com/";
        checkedLink = "https://gooGle.com:8080/abcd/x.html?query=1";
        expResult = true;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals(expResult, result, "good, subfolder");

        path = "google.com/";
        checkedLink = "https://www.gooGle.com:8080/abcd/x.html?query=1";
        expResult = false;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals(expResult, result, "bad, subfolder but on subdomain");

        path = "google.com/index.html";
        checkedLink = "https://gooGle.com:8080/index.html?query=1";
        expResult = true;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals(expResult, result, "good, identical link");

        path = "google.com/index.html";
        checkedLink = "https://gooGle.com:8080/inDex.html?query=1";
        expResult = false;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals(expResult, result, "bad, different letter case in the path");
    }

    // Test of addSpaces method, of class Generic.
    @Test
    void addSpaces() {
        Object object = null;
        int finalSize = 2;
        boolean inFront = false;
        String expResult = "  ";
        String result = Generic.addSpaces(object, finalSize, inFront);
        assertEquals(expResult, result, "null");

        object = "abc";
        finalSize = 5;
        inFront = false;
        expResult = "abc  ";
        result = Generic.addSpaces(object, finalSize, inFront);
        assertEquals(expResult, result, "spaces trailing");

        object = " abc";
        finalSize = 6;
        inFront = true;
        expResult = "   abc";
        result = Generic.addSpaces(object, finalSize, inFront);
        assertEquals(expResult, result, "spaces in front");

        object = " abc";
        finalSize = -1;
        inFront = true;
        expResult = " abc";
        result = Generic.addSpaces(object, finalSize, inFront);
        assertEquals(expResult, result, "no modification");
    }

    // Test of containsSubstring method, of class Generic.
    @Test
    void containsSubstring() {
        String string = "";
        String[] substrings = null;
        String expResult = null;
        String result = Generic.containsSubstring(string, substrings);
        assertEquals(expResult, result, "null");

        string = "xxx";
        substrings = new String[]{"", "x"};
        expResult = "";
        result = Generic.containsSubstring(string, substrings);
        assertEquals(expResult, result, "empty string");

        string = "xxx";
        substrings = new String[]{"y", "xxx", "vvv", ""};
        expResult = "xxx";
        result = Generic.containsSubstring(string, substrings);
        assertEquals(expResult, result, "normal string");

        string = "zzzxx";
        substrings = new String[]{"y", "xxx", "vvv"};
        expResult = null;
        result = Generic.containsSubstring(string, substrings);
        assertEquals(expResult, result, "substring not found");
    }

    // Test of convertMillisToDate method, of class Generic.
    @Test
    void convertMillisToDate() {
        // 12.08.2010 23:45:19.342
        long millis = 1074528964342L;
        String expResult = "19.01.2004 16:16:04.342";
        String result = Generic.convertMillisToDate(millis);
        assertEquals(expResult, result, "default GMT timeZone");

        expResult = "19.01.2004 08:16:04.342";
        result = Generic.convertMillisToDate(millis, "America/Los_Angeles");
        assertEquals(expResult, result, "Pacific Day Time timeZone");
    }

    // Test of getFormattedDate method, of class Generic.
    @Test
    void getFormattedDate_0args()
            throws InterruptedException {
        // this test can potentially fail in an extremely rare and unlikely case of high processor load and the test being run at the exact moment when the hour changes
        // GregorianCalendar gregorianCalendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        String expResult = Generic.convertMillisToDate(System.currentTimeMillis());
        expResult = expResult.substring(0, expResult.indexOf(':', expResult.indexOf(' ')) + ":".length());
        String result = Generic.getFormattedDate();

        assertTrue(result.startsWith(expResult));
    }

    // Test of getFormattedDate method, of class Generic.
    @Test
    void getFormattedDate_String() {
        // this test can potentially fail in an extremely rare and unlikely case of high processor load and the test being run at the exact moment when the hour changes
        String timeZoneName = "CET";
        String expResult = Generic.convertMillisToDate(System.currentTimeMillis(), "CET");
        expResult = expResult.substring(0, expResult.indexOf(':', expResult.indexOf(' ')) + ":".length());
        String result = Generic.getFormattedDate(timeZoneName);

        assertTrue(result.startsWith(expResult));
    }

    // Test of addCommas method, of class Generic.
    @Test
    void addCommas_Object() {
        Object value = "1203123.48987";
        String expResult = "1,203,123.48987";
        String result = Generic.addCommas(value);
        assertEquals(expResult, result);
    }

    // Test of addCommas method, of class Generic.
    @Test
    void addCommas_double_int() {
        double value = 1203123.48987;
        int nDecimals = 0;
        String expResult = "1,203,123";
        String result = Generic.addCommas(value, nDecimals);
        assertEquals(expResult, result, "zero decimals");

        value = 1203123.48987;
        nDecimals = 6;
        expResult = "1,203,123.489870";
        result = Generic.addCommas(value, nDecimals);
        assertEquals(expResult, result, "six decimals");
    }

    // Test of addCommas method, of class Generic.
    @Test
    void addCommas_4args() {
        String string = "012345678901234567890-1234567890";
        byte groupSize = 5;
        String commaDelimiter = ":";
        String periodDelimiter = "-";
        String expResult = "0:12345:67890:12345:67890-1234567890";
        String result = Generic.addCommas(string, groupSize, commaDelimiter, periodDelimiter);
        assertEquals(expResult, result);
    }

    // Test of isPureAscii method, of class Generic.
    @Test
    void isPureAscii() {
        String string = "faklfjhafl" + (char) 244;
        boolean expResult = false;
        boolean result = Generic.isPureAscii(string);
        assertEquals(expResult, result, "non-ASCII");

        string = "faklfjhafl";
        expResult = true;
        result = Generic.isPureAscii(string);
        assertEquals(expResult, result, "pure ASCII");
    }

    // Test of byteArrayIndexOf method, of class Generic.
    @Test
    void byteArrayIndexOf_byteArr_byteArr() {
        byte[] data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        byte[] pattern = new byte[]{3, 4, 5, 6, 7};
        int expResult = 2;
        int result = Generic.byteArrayIndexOf(data, pattern);
        assertEquals(expResult, result, "found");

        data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        pattern = new byte[]{3, -2, 5, 6, 7};
        expResult = -1;
        result = Generic.byteArrayIndexOf(data, pattern);
        assertEquals(expResult, result, "not found");
    }

    // Test of byteArrayIndexOf method, of class Generic.
    @Test
    void byteArrayIndexOf_3args() {
        byte[] data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        byte[] pattern = new byte[]{3, 4, 5, 6, 7};
        int beginIndex = 0;
        int expResult = 2;
        int result = Generic.byteArrayIndexOf(data, pattern, beginIndex);
        assertEquals(expResult, result, "found");

        data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        pattern = new byte[]{3, 4, 5, 6, 7};
        beginIndex = 2;
        expResult = 2;
        result = Generic.byteArrayIndexOf(data, pattern, beginIndex);
        assertEquals(expResult, result, "found, from index 2");

        data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        pattern = new byte[]{3, 4, 5, 6, 7};
        beginIndex = 3;
        expResult = -1;
        result = Generic.byteArrayIndexOf(data, pattern, beginIndex);
        assertEquals(expResult, result, "not found, from index 3");
    }

    // Test of byteArrayComputeFailure method, of class Generic.
    @Test
    void byteArrayComputeFailure() {
        byte[] pattern = new byte[]{3, 4, 5, 6, 7};
        int[] expResult = new int[]{0, 0, 0, 0, 0};
        int[] result = Generic.byteArrayComputeFailure(pattern);
        assertArrayEquals(expResult, result, "result with zeroes");

        pattern = new byte[]{3, 4, 3, 6, 8, 3, 4, 9};
        expResult = new int[]{0, 0, 1, 0, 0, 1, 2, 0};
        result = Generic.byteArrayComputeFailure(pattern);
        assertArrayEquals(expResult, result, "result with non-zeroes");
    }

    // Test of logOfBase method, of class Generic.
    @Test
    void logOfBase() {
        double base = 2;
        double num = 8;
        double expResult = 3;
        double result = Generic.logOfBase(base, num);
        assertEquals(expResult, result);
    }

    // Test of ceilingPowerOf method, of class Generic.
    @Test
    void ceilingPowerOf() {
        double base = 2;
        double num = 5;
        double expResult = 8;
        double result = Generic.ceilingPowerOf(base, num);
        assertEquals(expResult, result);
    }

    // Test of compareLinkedHashMap method, of class Generic.
    @Test
    void compareLinkedHashMap() {
        LinkedHashMap<String, Long> firstMap = new LinkedHashMap<String, Long>() {
            private static final long serialVersionUID = 1211L;

            {
                put("Standard1", (long) 110 * 60 * 1000);
                put("Sockslist", (long) 210 * 60 * 1000);
                put("Proxyhttp", (long) 410 * 60 * 1000);
                put("Freeproxylist", (long) 910 * 60 * 1000);
                put("Standard2", (long) 820 * 60 * 1000);
                put("Nntime", (long) 220 * 60 * 1000);
                put("Proxyspeedtest", (long) 320 * 60 * 1000);
                put("Proxybridge", (long) 120 * 60 * 1000);
            }
        };
        LinkedHashMap<String, Long> firstMapDuplicate = new LinkedHashMap<String, Long>() {
            private static final long serialVersionUID = 1211L;

            {
                put("Standard1", (long) 110 * 60 * 1000);
                put("Sockslist", (long) 210 * 60 * 1000);
                put("Proxyhttp", (long) 410 * 60 * 1000);
                put("Freeproxylist", (long) 910 * 60 * 1000);
                put("Standard2", (long) 820 * 60 * 1000);
                put("Nntime", (long) 220 * 60 * 1000);
                put("Proxyspeedtest", (long) 320 * 60 * 1000);
                put("Proxybridge", (long) 120 * 60 * 1000);
            }
        };
        LinkedHashMap<String, Long> secondMap = new LinkedHashMap<String, Long>() {
            private static final long serialVersionUID = 1212L;

            {
                put("Standard1", (long) 110 * 60 * 1000);
                put("Proxybridge", (long) 120 * 60 * 1000);
                put("Sockslist", (long) 210 * 60 * 1000);
                put("Nntime", (long) 220 * 60 * 1000);
                put("Proxyspeedtest", (long) 320 * 60 * 1000);
                put("Proxyhttp", (long) 410 * 60 * 1000);
                put("Standard2", (long) 820 * 60 * 1000);
                put("Freeproxylist", (long) 910 * 60 * 1000);
            }
        };
        LinkedHashMap<String, Long> thirdMap = new LinkedHashMap<String, Long>() {
            private static final long serialVersionUID = 1213L;

            {
                put("Standard1", (long) 110 * 60 * 1000);
                put("Sockslist", (long) 210 * 60 * 1000);
                put("Proxyhttp", (long) 410 * 60 * 1000);

                put("Standard2", (long) 820 * 60 * 1000);
                put("Nntime", (long) 220 * 60 * 1000);
                put("Proxyspeedtest", (long) 320 * 60 * 1000);
                put("Proxybridge", (long) 120 * 60 * 1000);
            }
        };

        boolean result = Generic.compareLinkedHashMap(firstMap, firstMapDuplicate);
        assertTrue(result, "duplicate");

        result = Generic.compareLinkedHashMap(firstMap, secondMap);
        assertFalse(result, "same elements, different order");

        result = Generic.compareLinkedHashMap(firstMap, thirdMap);
        assertFalse(result, "different nElements");
    }

    // Test of sortByValue method, of class Generic.
    @Test
    void sortByValue() {
        LinkedHashMap<String, Long> map = new LinkedHashMap<String, Long>() {
            private static final long serialVersionUID = 1201L;

            {
                put("Standard1", (long) 110 * 60 * 1000);
                put("Sockslist", (long) 210 * 60 * 1000);
                put("Proxyhttp", (long) 410 * 60 * 1000);
                put("Freeproxylist", (long) 910 * 60 * 1000);
                put("Standard2", (long) 820 * 60 * 1000);
                put("Nntime", (long) 220 * 60 * 1000);
                put("Proxyspeedtest", (long) 320 * 60 * 1000);
                put("Proxybridge", (long) 120 * 60 * 1000);
            }
        };
        LinkedHashMap<String, Long> expResultAscending = new LinkedHashMap<String, Long>() {
            private static final long serialVersionUID = 1202L;

            {
                put("Standard1", (long) 110 * 60 * 1000);
                put("Proxybridge", (long) 120 * 60 * 1000);
                put("Sockslist", (long) 210 * 60 * 1000);
                put("Nntime", (long) 220 * 60 * 1000);
                put("Proxyspeedtest", (long) 320 * 60 * 1000);
                put("Proxyhttp", (long) 410 * 60 * 1000);
                put("Standard2", (long) 820 * 60 * 1000);
                put("Freeproxylist", (long) 910 * 60 * 1000);
            }
        };
        LinkedHashMap<String, Long> expResultDescending = new LinkedHashMap<String, Long>() {
            private static final long serialVersionUID = 1203L;

            {
                put("Freeproxylist", (long) 910 * 60 * 1000);
                put("Standard2", (long) 820 * 60 * 1000);
                put("Proxyhttp", (long) 410 * 60 * 1000);
                put("Proxyspeedtest", (long) 320 * 60 * 1000);
                put("Nntime", (long) 220 * 60 * 1000);
                put("Sockslist", (long) 210 * 60 * 1000);
                put("Proxybridge", (long) 120 * 60 * 1000);
                put("Standard1", (long) 110 * 60 * 1000);
            }
        };

        boolean ascendingOrder = true;
        LinkedHashMap<String, Long> result = Generic.sortByValue(map, ascendingOrder);
        assertTrue(Generic.compareLinkedHashMap(expResultAscending, result), "ascending");

        ascendingOrder = false;
        result = Generic.sortByValue(map, ascendingOrder);
        assertTrue(Generic.compareLinkedHashMap(expResultDescending, result), "descending");
    }

    // Test of getRandomElementFromSet method, of class Generic.
    @Test
    void getRandomElementFromSet() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList(new String[]{"AC", "AD", "AE", "AERO", "AF", "AG", "AI", "AL", "AM", "AN"}));
        String result = Generic.getRandomElementFromSet(set);

        assertTrue(set.contains(result));
    }

    // Test of copyFile method, of class Generic.
    @Test
    void copyFile()
            throws IOException {
        String sourceFileName = Generic.tempFileName("sourceTest"), destFileName = Generic.tempFileName("destTest");
        File sourceFile = new File(sourceFileName), destFile = new File(destFileName);
        try {
            Generic.printStackTraces(sourceFileName);
            Generic.copyFile(sourceFile, destFile);

            FileAssert.assertBinaryEquals(sourceFile, destFile);
        } finally {
            sourceFile.delete();
            destFile.delete();
        }
    }
    // Test of concatArrays method, of class Generic.

    @Test
    void concatArrays() {
        Byte[] firstArray = new Byte[]{1, 2, 3, 4};
        Byte[][] restArrays = new Byte[][]{{5, 6}, {}, {7}, {8, 8, 8, 8, 9, 1}};
        Byte[] expResult = new Byte[]{1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 8, 9, 1};
        Byte[] result = Generic.concatArrays(firstArray, restArrays);

        assertArrayEquals(expResult, result);
    }

    // Test of compressByteArray method, of class Generic.
    @Test
    void compressByteArray()
            throws IOException {
        byte[] bytes = new byte[]{};
        String compressionFormat = "bogus_format";
        byte[] expResult = new byte[]{};
        byte[] result = Generic.compressByteArray(bytes, compressionFormat);
        assertArrayEquals(expResult, result, "bogus_format");

        bytes = new byte[]{};
        compressionFormat = "gzip";
        result = Generic.compressByteArray(Generic.compressByteArray(bytes, compressionFormat), compressionFormat);
        assertTrue(result.length > 0, "gzip");

        bytes = new byte[]{};
        compressionFormat = "deflate";
        result = Generic.compressByteArray(Generic.compressByteArray(bytes, compressionFormat), compressionFormat);
        assertTrue(result.length > 0, "deflate");
    }

    // Test of decompressByteArray method, of class Generic.
    @Test
    void decompressByteArray()
            throws IOException {
        byte[] bytes = new byte[]{1, 1, 1};
        String compressionFormat = "bogus_format";
        byte[] expResult = new byte[]{1, 1, 1};
        byte[] result = Generic.decompressByteArray(bytes, compressionFormat);
        assertArrayEquals(expResult, result, "bogus_format");

        bytes = new byte[]{1, 2, 3, 4};
        compressionFormat = "gzip";
        expResult = new byte[]{1, 2, 3, 4};
        result = Generic.decompressByteArray(Generic.compressByteArray(bytes, compressionFormat), compressionFormat);
        assertArrayEquals(expResult, result, "gzip");

        bytes = new byte[]{1, 2, 3, 4};
        compressionFormat = "deflate";
        expResult = new byte[]{1, 2, 3, 4};
        result = Generic.decompressByteArray(Generic.compressByteArray(bytes, compressionFormat), compressionFormat);
        assertArrayEquals(expResult, result, "deflate");
    }

    // Test of getSubstrings method, of class Generic.
    @Test
    void getSubstrings_3args() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        LinkedList<String> expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "NOSCRIPT", "A", "b", "", "", "", "A"}));
        LinkedList<String> result = Generic.getSubstrings(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    // Test of getSubstrings method, of class Generic.
    @Test
    void getSubstrings_4args() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        boolean getInterSubstrings = false;
        LinkedList<String> expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "NOSCRIPT", "A", "b", "", "", "", "A"}));
        LinkedList<String> result = Generic.getSubstrings(harvestInputString, firstDelimiter, secondDelimiter, getInterSubstrings);
        assertEquals(expResult, result);
    }

    // Test of getSubstrings method, of class Generic.
    @Test
    void getSubstrings_6args() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        boolean getInterSubstrings = false;
        int nSubstrings = -1;
        LinkedList<String> expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "NOSCRIPT", "A", "b", "", "", "", "A"}));
        LinkedList<String> result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters different");

        harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        firstDelimiter = "<";
        secondDelimiter = ">";
        getInterSubstrings = false;
        nSubstrings = 0;
        expResult = new LinkedList<>(Arrays.asList(new String[]{}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings zero, getInterSubstrings false, delimiters different");

        harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        firstDelimiter = "<";
        secondDelimiter = ">";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "", "NOSCRIPT", "", "A", " "}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters different");

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "NOSCRIPT", "A", " ", "b", "das", "sss", "", "", "A|1"}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings true, delimiters identical");

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = false;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "A", "b", "sss", ""}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters identical");

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "NOSCRIPT", "A", " ", "b", "das"}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters identical");

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "-|NOSCRIPT", "A", "-| ", "b", "-|das"}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters different but with similar parts");

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "-|NOSCRIPT", "A", "-| ", "b", "-|das", "sss", "", "", "A|1"}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings true, delimiters different but with similar parts");

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = false;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "A", "b", "sss", ""}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters different but with similar parts");
    }

    // Test of getSubstringsIgnoreCase method, of class Generic.
    @Test
    void getSubstringsIgnoreCase() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "o";
        String secondDelimiter = "S";
        LinkedList<String> expResult = new LinkedList<>(Arrays.asList(new String[]{"", ""}));
        LinkedList<String> result = Generic.getSubstringsIgnoreCase(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    // Test of getSubstring method, of class Generic.
    @Test
    void getSubstring() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        String expResult = "NOSCRIPT";
        String result = Generic.getSubstring(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    @Test
    void removeSubstring() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        String expResult = "<><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String result = Generic.removeSubstring(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);

        final String replacement = "replace";
        expResult = "<" + replacement + "><NOSCRIPT><A> <b>das<>sss<><><A>1";
        result = Generic.removeSubstring(harvestInputString, firstDelimiter, secondDelimiter, replacement);
        assertEquals(expResult, result);
    }

    // Test of serializedDeepCopy method, of class Generic.
    @Test
    void serializedDeepCopy() {
        String sourceObject = "test";
        String expResult = "test";
        String result = Generic.serializedDeepCopy(sourceObject);
        assertEquals(expResult, result);
    }

    // Test of synchronizedCopyObjectFields method, of class Generic.
    @Test
    void synchronizedCopyObjectFields()
            throws UnknownHostException {
        LocalTestObject sourceObject = new LocalTestObject("test", 7);
        LocalTestObject destinationObject = new LocalTestObject();

        Generic.synchronizedCopyObjectFields(sourceObject, destinationObject);
        assertEquals(sourceObject, destinationObject);
    }

    // Test of copyObjectFields method, of class Generic.
    @Test
    void copyObjectFields() {
        LocalTestObject sourceObject = new LocalTestObject("test", 7);
        LocalTestObject destinationObject = new LocalTestObject();

        Generic.copyObjectFields(sourceObject, destinationObject);
        assertEquals(sourceObject, destinationObject);
    }

    // Test of objectToString method, of class Generic.
    @Test
    void objectToString_1args() {
        LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test number=0)";
        String result = Generic.objectToString(object);
        assertEquals(expResult, result, "first");

        LocalTestObject[] arrayObject = new LocalTestObject[]{object};
        expResult = "[(name=test number=0)]";
        result = Generic.objectToString(arrayObject);
        assertEquals(expResult, result, "second");

        ArrayList<LocalTestObject> arrayList = new ArrayList<>(1);
        arrayList.add(object);
        expResult = "[(name=test number=0)]";
        result = Generic.objectToString(arrayList);
        assertEquals(expResult, result, "third");

        HashMap<LocalTestObject, LocalTestObject> hashMap = new HashMap<>(2);
        hashMap.put(object, object);
        expResult = "[(key=(name=test number=0) value=(name=test number=0))]";
        result = Generic.objectToString(hashMap);
        assertEquals(expResult, result, "fourth");

        expResult = "Thu Jan 01 00:00:00 UTC 1970";
        result = Generic.objectToString(new Date(0L));
        assertEquals(expResult, result, "fifth");
    }

    // Test of objectToString method, of class Generic.
    @Test
    void objectToString_2args() {
        LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test)";
        String result = Generic.objectToString(object, false);
        assertEquals(expResult, result);
    }

    // Test of objectToString method, of class Generic.
    @Test
    void objectToString_3args() {
        LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test number=0)";
        String result = Generic.objectToString(object, true, true);
        assertEquals(expResult, result);
    }

    @Test
    void objectToString_6args()
            throws UnknownHostException {
        LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test)";
        String result = Generic.objectToString(object, false, true, false, 10);
        assertEquals(expResult, result);

        expResult = "()";
        result = Generic.objectToString(object, false, true, false, 10, "name");
        assertEquals(expResult, result);
    }

    // Test of disableHTTPSValidation method, of class Generic.
    @Test
    void disableHTTPSValidation() {
        // the all trusting manager is not tested here as I couldn't find a method to do it, but there's no rush, as it seems to work fine
        assertEquals(HttpsURLConnection.getDefaultHostnameVerifier().verify("", null), false, "before");

        Generic.disableHTTPSValidation();

        assertEquals(HttpsURLConnection.getDefaultHostnameVerifier().verify("", null), true, "after");
    }

    // Test of specialCharParser method, of class Generic.
    @Test
    void specialCharParser() {
        String line = "abcdefghijklmnopqrstuvwxyz";
        String expResult = "abcdefghijklmnopqrstuvwxyz";
        String result = Generic.specialCharParser(line);
        assertEquals(expResult, result, "no change");

        line = "abcde&nbsp;fghijk&amp;&amp;lm&#x38;n&#56;op&quot;qrstu&lt;&gt;vwxyz&amp;";
        expResult = "abcde fghijk&&lm8n8op\"qrstu<>vwxyz&";
        result = Generic.specialCharParser(line);
        assertEquals(expResult, result, "change");
    }

    // Test of closeStandardStreams method, of class Generic.
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    @Test
    void closeStandardStreams()
            throws IOException {
        ByteArrayOutputStream outContent = null, errContent = null;
        PipedInputStream pipedInputStream = null;
        PipedOutputStream pipedOutputStream = null;
        PrintStream outPrintStream = null, errPrintStream = null;

        PrintStream originalOut = System.out, originalErr = System.err;
        InputStream originalIn = System.in;

        try {
            outContent = new ByteArrayOutputStream();
            outPrintStream = new PrintStream(outContent);
            errContent = new ByteArrayOutputStream();
            errPrintStream = new PrintStream(errContent);
            pipedInputStream = new PipedInputStream();
            pipedOutputStream = new PipedOutputStream(pipedInputStream);

            System.setIn(pipedInputStream);
            System.setOut(outPrintStream);
            System.setErr(errPrintStream);

            System.out.print("hello out");
            System.err.print("hello err");
            pipedOutputStream.write(new byte[6]);

            assertTrue(outContent.toString().contains("hello out"), "before out");
            assertTrue(errContent.toString().contains("hello err"), "before err");
            assertTrue(pipedInputStream.available() == 6, "before in");

            Generic.closeStandardStreams();

            System.out.print("xxx");
            System.err.print("xxx");
            try {
                pipedOutputStream.write(new byte[3]);
            } catch (IOException ioException) {
//                logger.debug("Expected pipe closed IOException", ioException);
            }

            assertTrue(outContent.toString().contains("hello out"), "after out");
            assertTrue(errContent.toString().contains("hello err"), "after err");
            assertTrue(pipedInputStream.available() == 0, "after in");
        } finally {
            Generic.closeObjects(outPrintStream, outContent, errPrintStream, errContent, pipedOutputStream, pipedInputStream);
            System.setOut(originalOut);
            System.setErr(originalErr);
            System.setIn(originalIn);
        }
    }

    @Test
    void checkAtomicBooleans_varargs() {
        AtomicBoolean first = new AtomicBoolean(), second = new AtomicBoolean();

        boolean result = Generic.checkAtomicBooleans(first, second);
        assertFalse(result, "false");

        second.set(true);
        result = Generic.checkAtomicBooleans(first, second);
        assertTrue(result, "true");
    }

    @Test
    void checkAtomicBooleans_boolean_varargs() {
        AtomicBoolean first = new AtomicBoolean(), second = new AtomicBoolean();

        boolean result = Generic.checkAtomicBooleans(false, first, second);
        assertTrue(result, "true");

        first.set(true);
        second.set(true);
        result = Generic.checkAtomicBooleans(false, first, second);
        assertFalse(result, "false");
    }

    @Test
    void checkObjects() {
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        AtomicReference<String> atomicReference = new AtomicReference<>();

        boolean result = Generic.checkObjects(atomicBoolean, atomicReference);
        assertFalse(result, "false");

        atomicBoolean.set(true);
        result = Generic.checkObjects(atomicBoolean, atomicReference);
        assertTrue(result, "true 1");

        atomicBoolean.set(false);
        atomicReference.set("test");
        result = Generic.checkObjects(atomicBoolean, atomicReference);
        assertTrue(result, "true 2");
    }

    // Test of threadSleepSegmented_long_long_AtomicBoolean method, of class Generic.
    @Test
    void threadSleepSegmented_long_long_AtomicBoolean() {
        long millis = 1000L;
        long timeBefore = System.currentTimeMillis();

        Generic.threadSleepSegmented(millis, 1L, new AtomicBoolean());

        long timeAfter = System.currentTimeMillis();

//        logger.info("threadSleepSegmented_long_long_AtomicBoolean() millis slept: {}    millis passed: {}", millis, timeAfter - timeBefore);
        assertTrue(timeAfter - timeBefore >= 1000);
    }

    // Test of threadSleep method, of class Generic.
    @Test
    void threadSleep() {
        long millis = 1000L;
        long timeBefore = System.currentTimeMillis();

        Generic.threadSleep(millis);

        long timeAfter = System.currentTimeMillis();

//        logger.info("testThreadSleep() millis slept: {}    millis passed: {}", millis, timeAfter - timeBefore);
        assertTrue(timeAfter - timeBefore >= 1000);
    }

    // Test of setFinalStatic method, of class Generic.
    @Test
    void setFinalStatic()
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = LocalTestObject.class.getDeclaredField("serialVersionUID");
        long newValue = 33L;

        Generic.setFinalStatic(field, newValue);
        boolean fieldAccessible = field.isAccessible();
        if (!fieldAccessible) {
            field.setAccessible(true);
        }
        long result = field.getLong(LocalTestObject.class);

        if (fieldAccessible != field.isAccessible()) {
            field.setAccessible(fieldAccessible);
        }

        assertTrue(result == newValue);
    }

    @Test
    void getField() {
        String expName = "testName";
        int expNumber = 123;

        LocalTestObject localTestObject = new LocalTestObject(expName, expNumber);

        String resultName = (String) Generic.getField(localTestObject, "name");
        assertEquals(expName, resultName, "name");

        int resultNumber = (Integer) Generic.getField(localTestObject, "number");
        assertEquals(expNumber, resultNumber, "number");

        assertNull(Generic.getField(localTestObject, "bogus"), "null");
    }

    @Test
    void setField() {
        String expName = "testName";
        int expNumber = 123;

        LocalTestObject localTestObject = new LocalTestObject();

        boolean expResult = true;
        boolean result = Generic.setField(localTestObject, "name", expName);
        assertEquals(expResult, result, "result name");
        assertEquals(expName, localTestObject.getName(), "name");

        expResult = true;
        result = Generic.setField(localTestObject, "number", expNumber);
        assertEquals(expResult, result, "result number");
        assertEquals(expNumber, localTestObject.getNumber(), "number");

        // NoSuchFieldException is expected
        expResult = false;
        result = Generic.setField(localTestObject, "bogus", expNumber);
        assertEquals(expResult, result, "result bogus");
    }

    @Test
    void turnOffHtmlUnitLogger() {
        // I won't be testing this method; it works and it will be obvious if it doesn't
    }
}
