package info.fmro.shared.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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
import javax.net.ssl.HttpsURLConnection;
import junitx.framework.FileAssert;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericTest {

    private static final Logger logger = LoggerFactory.getLogger(GenericTest.class);

    private class LocalDebugger
            extends Debugger {
    }

    private class LocalTestObject {

        private static final long serialVersionUID = 1221L;
        private String name = "";
        private int number;

        private LocalTestObject() {
        }

        private LocalTestObject(String name, int number) {
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

    public GenericTest() {
    }

    @Rule
    @SuppressWarnings("PublicField")
    public TestRule watchman = new TestWatcher() {
        @Override
        public void starting(Description description) {
            logger.info("{} being run...", description.getMethodName());
        }
    };

    @Test
    public void testCreateStringFromCodes() {
        String result = Generic.createStringFromCodes(32, 32, 32);
        assertEquals("1", result, "   ");

        result = Generic.createStringFromCodes(32, 32, 0);
        assertEquals("2", result, "  \u0000");
    }

    @Test
    public void testGetStringCodePointValues() {
        String symbol_194_160 = (new StringBuilder(2)).append('\u00c2').append('\u00a0').toString();
        String result = Generic.getStringCodePointValues(symbol_194_160);
        assertEquals(result, "194 160");
    }

//    @Test
//    public void testEscapeString() {
//        String initialString = "abc";
//        String result = Generic.escapeString(initialString);
//
//        assertEquals(result, "abc");
//
////        String symbol_194_160 = "\u00c2" + "-" + "\u0020" + "-" + "\u00a0";
//        String symbol_194_160 = (new StringBuilder(2)).append('\u00c2').append('\u00a0').toString();
//        result = Generic.escapeString(symbol_194_160);
//
//        assertEquals(result, "abc");
//    }
    @Test
    public void testProperTimeStamp() {
        String value = Generic.properTimeStamp();

        assertEquals(value.length(), 23);
    }

    @Test
    public void testProperTimeStamp_long() {
        String value = Generic.properTimeStamp(500L);

        assertEquals(value.length(), 23);

        value = Generic.properTimeStamp(0L);

        assertEquals(value.length(), 23);
    }

    @Test
    public void testGetSubclasses() {
        Set<Class<? extends Debugger>> set = Generic.getSubclasses("info.fmro", Debugger.class);
        Set<Class<? extends Debugger>> expected = new HashSet<>(0);
        expected.add(LocalDebugger.class);

        assertEquals(expected, set);
    }

    @Test
    public void testCollectionKeepMultiples() {
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
    public void testIsPowerOfTwo() {
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
    public void testStringBuilderReplace() {
        StringBuilder stringBuilder = new StringBuilder("abcd");
        String result = "xyz";
        Generic.stringBuilderReplace(stringBuilder, result);
        String expResult = result;
        assertEquals(expResult, stringBuilder.toString());
    }

    @Test
    public void testMiddleValue_double() {
        double a = 3.62378947368421d, b = 1.3231d, c = 3.623789473684211d, result, expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertTrue(expResult == result);
    }

    @Test
    public void testMiddleValue_float() {
        float a = 3.62378947368421f, b = 3.3231f, c = 3.623789473684211f, result, expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertTrue(expResult == result);
    }

    @Test
    public void testMiddleValue_int() {
        int a = 3, b = 1, c = 1, result, expResult;

        result = Generic.middleValue(a, b, c);
        expResult = c;
        assertTrue(expResult == result);
    }

    @Test
    public void testMiddleValue_long() {
        long a = 3L, b = 2L, c = 3L, result, expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertTrue(expResult == result);
    }

    @Test
    public void testTruncateDouble() {
        Double size = 3.62378947368421d;
        double result = Generic.truncateDouble(size, 2);
        double expResult = 3.62d;
        assertTrue("1", expResult == result);

        size = 3.62978947368421d;
        result = Generic.truncateDouble(size, 2);
        expResult = 3.62d;
        assertTrue("2", expResult == result);
    }

    @Test
    public void testQuotedReplaceAll() {
        String string = "abc'-&  u. ", pattern = ". ", replacement = " ", expResult = "abc'-&  u ";
        String result = Generic.quotedReplaceAll(string, pattern, replacement);
        assertTrue("first", result.equals(expResult));

        string = " ";
        pattern = " ";
        replacement = "";
        expResult = ""; // it's a special character, non breaking space 194+160
        result = Generic.quotedReplaceAll(string, pattern, replacement);
        assertTrue("second", result.equals(expResult));
    }

    @Test
    public void testStringMatchChance_String_String() {
        String first = "", second = "";
        double expResult = 0;
        double result = Generic.stringMatchChance(first, second);
        assertTrue("first", expResult == result);

        first = null;
        second = null;
        expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("second", expResult == result);

        first = "Ab ";
        second = "   ab";
        expResult = 1;
        result = Generic.stringMatchChance(first, second);
        assertTrue("third", expResult == result);

        first = "a";
        second = "b";
        expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("fourth", expResult == result);

        first = "abcdefghijklmnopqrstuvwxyz0123456789";
        second = "hdfkladhlaeueuqrlahfasddshladshsjanj";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("fifth", result < .2);

        first = "Turin";
        second = "Torino";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("sixth", result > .8);

        first = "Al Khor";
        second = "Al Khoor";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("seventh", result > .92);

        first = "Al Jahra";
        second = "Al Tadamon";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("eight", result < .7);

        first = "AL Shamal";
        second = "Al Kharaityat";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("ninth", result < .7);

        first = "Club Social DYC Espanol";
        second = "Grasshopper Club";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("10", result < .7);

        first = "Real Cartagena";
        second = "Real Sociedad";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("11", result < .7);

        first = "AC Sparta Praha U21";
        second = "Sparta Prague";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("12", result < .8);

        first = "MAS Taborsko U21";
        second = "AEL Limassol";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("13", result < .5);

        first = "AC Sparta Praha U21";
        second = "Spartaks";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("14", result < .8);

        first = "Platense";
        second = "CA River Plate";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("15", result < .75);

        first = "Real Cartagena";
        second = "Real Espana";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("16", result < .8);

        first = "America de Cali";
        second = "Uni de Costa Rica";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("17", result < .6);

        first = "Ajaccio GFCO";
        second = "GFCO Ajaccio";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("18", result > .9);

        first = "Los Angeles Galaxy II";
        second = "Los Angeles Galaxy";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("19", result > .9);

        first = "Hapoel Haifa";
        second = "Hapoel Tel Aviv";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("19", result < .8);

        first = "ASO Chlef";
        second = "ASO CHIEF";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertTrue("20", result > .9);
    }

    @Test
    public void testStringMatchChance_String_String_boolean() {
        String first = "a", second = "A";
        double expResult = 0;
        double result = Generic.stringMatchChance(first, second, false);
        assertTrue("first", expResult == result);

        first = "A";
        second = "A ";
        expResult = 1;
        result = Generic.stringMatchChance(first, second, false);
        assertTrue("second", expResult == result);
    }

    @Test
    public void testStringMatchChance_String_String_boolean_boolean() {
        String first = " ", second = " ";
        double expResult = 1;
        double result = Generic.stringMatchChance(first, second, true, false);
        assertTrue("first", expResult == result);

        first = "a";
        second = "a ";
        // expResult = 0;
        result = Generic.stringMatchChance(first, second, true, false);
        assertTrue("second", result < 1 && result > .5);
    }

    @Test
    public void testGetCollectionCapacity() {
        float loadFactor = 0.75f;
        int size = 43;
        int result = Generic.getCollectionCapacity(size, loadFactor);
        int expResult = 64;

        assertEquals("first", expResult, result);

        loadFactor = 0.75f;
        size = 1;
        result = Generic.getCollectionCapacity(size, loadFactor);
        expResult = 2;

        assertEquals("second", expResult, result);

        loadFactor = 0.5f;
        size = 88;
        result = Generic.getCollectionCapacity(size, loadFactor);
        expResult = 256;

        assertEquals("third", expResult, result);

        loadFactor = 0.75f;
        size = 3;
        result = Generic.getCollectionCapacity(size, loadFactor);
        expResult = 4;

        assertEquals("fourth", expResult, result);
    }

    // Test of printStackTraces method, of class Generic.
    @Test(timeout = 10000)
    public void testPrintStackTraces_String() {
        String fileName = Generic.tempFileName("test");
        File file = new File(fileName);
        try {
            Generic.printStackTraces(fileName);

            assertTrue(file.length() > 0);
        } finally {
            file.delete();
        }
    }

    // Test of printStackTraces method, of class Generic.
    @Test
    public void testPrintStackTraces_PrintStream() {
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
    public void testPrintStackTrace_StackTraceElementArr_String() {
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
    public void testPrintStackTrace_StackTraceElementArr_PrintStream() {
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
    public void testCloseObjects() {
        boolean[] expResult = new boolean[]{true, true, true, true};
        boolean[] result = Generic.closeObjects(new Socket(), new Socket(), new Socket(), new Socket());
        assertTrue(Arrays.equals(expResult, result));
    }

    // Test of setSoLinger method, of class Generic.
    @Test
    public void testSetSoLinger()
            throws SocketException {
        Socket socket = new Socket();
        try {
            boolean expResult = true;
            boolean result = Generic.setSoLinger(socket);

            assertEquals("testSetSoLinger boolean result", expResult, result);
            assertEquals("testSetSoLinger setSoLinger", 0, socket.getSoLinger());
        } finally {
            Generic.closeObject(socket);
        }
    }

    // Test of closeObject method, of class Generic.
    @Test
    public void testCloseObject() {
        Socket socket = new Socket();
        try {
            boolean expResult = true;
            boolean result = Generic.closeObject(socket);

            assertEquals("testCloseObject boolean result", expResult, result);
            assertTrue("testCloseObject close", socket.isClosed());
        } finally {
            Generic.closeObject(socket);
        }
    }

    // Test of concatByte method, of class Generic.
    @Test
    public void testConcatByte() {
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
    public void testEncryptString() {
        String string = "abcE";
        int encryptKey = 3;
        String expResult = "defH";
        String result = Generic.encryptString(string, encryptKey);

        assertEquals(expResult, result);
    }

    // Test of encryptFile method, of class Generic.
    @Test
    public void testEncryptFile()
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
    public void testTempFileName() {
        String fileName = "test";
        String result = Generic.tempFileName(fileName);
        String result2 = Generic.tempFileName(fileName);

        assertFalse("exists", new File(result).exists());
        assertFalse("equals", result.equals(result2));
    }

    // Test of readObjectFromFile, writeObjectToFile, synchronizedWriteObjectToFile methods, of class Generic.
    @Test
    public void testReadWriteObjectFromFile() {
        String fileName = Generic.tempFileName("test");
        int[] intArray = new int[]{1, 2, 5, 1, 3213, 6554211, 132312312};
        try {
            Generic.writeObjectToFile(intArray, fileName);
            int[] resultIntArray = (int[]) Generic.readObjectFromFile(fileName);

            assertArrayEquals("unsynchronized", intArray, resultIntArray);

            Generic.synchronizedWriteObjectToFile(intArray, fileName, false);
            resultIntArray = (int[]) Generic.readObjectFromFile(fileName);

            assertArrayEquals("synchronized", intArray, resultIntArray);
        } finally {
            new File(fileName).delete();
        }
    }

    // Test of getHexString method, of class Generic.
    @Test
    public void testGetHexString() {
        byte[] byteArray = new byte[]{12, 22, 111, 2, 0, -12};
        String expResult = "0c166f0200f4";
        String result = Generic.getHexString(byteArray);

        assertEquals(expResult, result);
    }

    @Test
    public void testBackwardWordsString() {
        String string = "Ajaccio GFCO";
        String expResult = "GFCO Ajaccio";
        String result = Generic.backwardWordsString(string);

        assertEquals("1", expResult, result);

        string = " first  second third";
        expResult = "third second  first ";
        result = Generic.backwardWordsString(string);

        assertEquals("1", expResult, result);
    }

    // Test of backwardString method, of class Generic.
    @Test
    public void testBackwardString() {
        String string = "test123";
        String expResult = "321tset";
        String result = Generic.backwardString(string);

        assertEquals(expResult, result);
    }

    // Test of trimIP method, of class Generic.
    @Test
    public void testTrimIP() {
        String IP = "012.001.000.123";
        String expResult = "12.1.0.123";
        String result = Generic.trimIP(IP);

        assertEquals("real", expResult, result);

        IP = "dasdsadasd";
        expResult = "dasdsadasd";
        result = Generic.trimIP(IP);

        assertEquals("bogus", expResult, result);
    }

    // Test of goodPort method, of class Generic.
    @Test
    public void testGoodPort() {
        String tempPort = "ujklljkh";
        boolean expResult = false;
        boolean result = Generic.goodPort(tempPort);

        assertEquals("bogus", expResult, result);

        tempPort = "1234";
        expResult = true;
        result = Generic.goodPort(tempPort);

        assertEquals("good", expResult, result);

        tempPort = "65536";
        expResult = false;
        result = Generic.goodPort(tempPort);

        assertEquals("bad, zero", expResult, result);

        tempPort = "43894344";
        expResult = true;
        result = Generic.goodPort(tempPort);

        assertEquals("good, large number", expResult, result);
    }

    // Test of goodDomain method, of class Generic.
    @Test
    public void testGoodDomain() {
        String host = "google.com";
        boolean expResult = true;
        boolean result = Generic.goodDomain(host);

        assertEquals("good", expResult, result);

        host = "de-dsd_dasd-dsLd.ro";
        expResult = true;
        result = Generic.goodDomain(host);

        assertEquals("good, bogus name", expResult, result);

        host = null;
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("null", expResult, result);

        host = "";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("empty string", expResult, result);

        host = "de-dsdasdasd-dsad.ro" + (char) 244;
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, not pure ASCII", expResult, result);

        host = "de-dsdasdasd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, no period", expResult, result);

        host = "de-dsdasdasd-dsadro.";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, ends with period", expResult, result);

        host = "de-dsdasd/asd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, contains slash", expResult, result);

        host = "de-dsdasd?asd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, contains question mark", expResult, result);

        host = " de-dsdasdasd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, contains space", expResult, result);

        host = "de-dsd:sdasd-dsadro";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, disallowed character", expResult, result);

        host = "1234567890abcdefghjk1234.567890abcdefghjk1234567890a.bcdefghjk1234567890abcdefghjk12345.67890abcdefghjk1234567890abcde.fghjk1234567890abcdefghjk12.34567890abcdef" +
                "ghjk123.4567890abcdefghjk.1234567890abcdefghjk123.4567890abcdefghjk123456789.hjk123456789.com";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, size 254", expResult, result);

        host = "234567890abcdefghjk1234.567890abcdefghjk1234567890a.bcdefghjk1234567890abcdefghjk12345.67890abcdefghjk1234567890abcde.fghjk1234567890abcdefghjk12.34567890abcdefg" +
                "hjk123.4567890abcdefghjk.1234567890abcdefghjk123.4567890abcdefghjk123456789.hjk123456789.com";
        expResult = true;
        result = Generic.goodDomain(host);

        assertEquals("good, size 253", expResult, result);

        host = "1234567890abcdefghjk1234567890abcdefghjk1234567890abcdefghjk1234.567890abcdefghjk12345.67890abcdefghjk1234567890abcde.fghjk1234567890abcdefghjk12.34567890abcdefg" +
                "hjk123.4567890abcdefghjk.1234567890abcdefghjk123.4567890abcdefghjk123456789.hjk123456789.com";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, label size 64", expResult, result);

        host = "1234567890abcdefghjk1234567890abcdefghjk1234567890abcdefghjk123.4567890abcdefghjk12345.67890abcdefghjk1234567890abcde.fghjk1234567890abcdefghjk12.34567890abcdefg" +
                "hjk123.4567890abcdefghjk.1234567890abcdefghjk123.4567890abcdefghjk123456789.hjk123456789.com";
        expResult = true;
        result = Generic.goodDomain(host);

        assertEquals("good, label size 63", expResult, result);

        host = "xxx.tom";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad TLD", expResult, result);

        host = "209.191.122.70";
        expResult = true;
        result = Generic.goodDomain(host);

        assertEquals("good IP", expResult, result);

        host = "123.1.1.1.1";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, bogus IP", expResult, result);

        host = "225.191.122.70";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, reserved range IP", expResult, result);

        // a NumberFormatException is expected here
        host = "222718927192871289715.191.122.70";
        expResult = false;
        result = Generic.goodDomain(host);

        assertEquals("bad, IP parseInt exception", expResult, result);
    }

    // Test of getUserAgent method, of class Generic.
    @Test
    public void testGetUserAgent() {
        String result = Generic.getUserAgent();

        assertTrue("startsWith", result.startsWith("Mozilla/"));
        assertTrue("endsWith", result.endsWith(")"));
    }

    // Test of getSocksType method, of class Generic.
    @Test
    public void testGetSocksType() {
        String proxyType = "soCks4";
        byte expResult = 4;
        byte result = Generic.getSocksType(proxyType);

        assertEquals("socks4", expResult, result);

        proxyType = "soCKs5";
        expResult = 5;
        result = Generic.getSocksType(proxyType);

        assertEquals("socks5", expResult, result);

        // logger.warn is expected
        proxyType = "dhlskadh89sadnjm,n908l,.,";
        expResult = 5;
        result = Generic.getSocksType(proxyType);

        assertEquals("bogus", expResult, result);
    }

    // Test of linkRemoveProtocol method, of class Generic.
    @Test
    public void testLinkRemoveProtocol() {
        String link = "http://dsdsa.com:80/?query=1";
        String expResult = "dsdsa.com:80/?query=1";
        String result = Generic.linkRemoveProtocol(link);

        assertEquals(expResult, result);
    }

    // Test of linkRemovePort method, of class Generic.
    @Test
    public void testLinkRemovePort() {
        String link = "http://dsdsa.com:80/?query=1";
        String expResult = "http://dsdsa.com/?query=1";
        String result = Generic.linkRemovePort(link);

        assertEquals(expResult, result);
    }

    // Test of linkRemoveQuery method, of class Generic.
    @Test
    public void testLinkRemoveQuery() {
        String link = "http://dsdsa.com:80/?query=1";
        String expResult = "http://dsdsa.com:80/";
        String result = Generic.linkRemoveQuery(link);

        assertEquals(expResult, result);
    }

    // Test of getLinkHost method, of class Generic.
    @Test
    public void testGetLinkHost() {
        String link = "http://dsDsa.com:80/?query=1";
        String expResult = "dsdsa.com";
        String result = Generic.getLinkHost(link);

        assertEquals("good", expResult, result);

        link = "http://dsDsa.tom:80/?query=1";
        result = Generic.getLinkHost(link);

        assertNull("bad TLD", result);

        link = null;
        result = Generic.getLinkHost(link);

        assertNull("null", result);

        link = "/?query=1";
        result = Generic.getLinkHost(link);

        assertNull("no host", result);

        link = "http://dsDsa.com:80";
        expResult = "dsdsa.com";
        result = Generic.getLinkHost(link);

        assertEquals("domain only", expResult, result);
    }

    // Test of linkMatches method, of class Generic.
    @Test
    public void testLinkMatches() {
        String path = "http://goOgle.com:80";
        String checkedLink = "https://gooGle.com/?query=1";
        boolean expResult = true;
        boolean result = Generic.linkMatches(path, checkedLink);
        assertEquals("good, identical, protocol/port/query/domain_case ignored", expResult, result);

        // logger.warn is expected
        path = "google.como/";
        checkedLink = "google.como/";
        expResult = false;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals("bad host", expResult, result);

        path = "google.com";
        checkedLink = "https://www.gooGle.com:8080/abcd/x.html?query=1";
        expResult = true;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals("good, same domain or a subdomain", expResult, result);

        path = "google.com/";
        checkedLink = "https://gooGle.com:8080/abcd/x.html?query=1";
        expResult = true;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals("good, subfolder", expResult, result);

        path = "google.com/";
        checkedLink = "https://www.gooGle.com:8080/abcd/x.html?query=1";
        expResult = false;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals("bad, subfolder but on subdomain", expResult, result);

        path = "google.com/index.html";
        checkedLink = "https://gooGle.com:8080/index.html?query=1";
        expResult = true;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals("good, identical link", expResult, result);

        path = "google.com/index.html";
        checkedLink = "https://gooGle.com:8080/inDex.html?query=1";
        expResult = false;
        result = Generic.linkMatches(path, checkedLink);
        assertEquals("bad, different letter case in the path", expResult, result);
    }

    // Test of addSpaces method, of class Generic.
    @Test
    public void testAddSpaces() {
        Object object = null;
        int finalSize = 2;
        boolean inFront = false;
        String expResult = "  ";
        String result = Generic.addSpaces(object, finalSize, inFront);
        assertEquals("null", expResult, result);

        object = "abc";
        finalSize = 5;
        inFront = false;
        expResult = "abc  ";
        result = Generic.addSpaces(object, finalSize, inFront);
        assertEquals("spaces trailing", expResult, result);

        object = " abc";
        finalSize = 6;
        inFront = true;
        expResult = "   abc";
        result = Generic.addSpaces(object, finalSize, inFront);
        assertEquals("spaces in front", expResult, result);

        object = " abc";
        finalSize = -1;
        inFront = true;
        expResult = " abc";
        result = Generic.addSpaces(object, finalSize, inFront);
        assertEquals("no modification", expResult, result);
    }

    // Test of containsSubstring method, of class Generic.
    @Test
    public void testContainsSubstring() {
        String string = "";
        String[] substrings = null;
        String expResult = null;
        String result = Generic.containsSubstring(string, substrings);
        assertEquals("null", expResult, result);

        string = "xxx";
        substrings = new String[]{"", "x"};
        expResult = "";
        result = Generic.containsSubstring(string, substrings);
        assertEquals("empty string", expResult, result);

        string = "xxx";
        substrings = new String[]{"y", "xxx", "vvv", ""};
        expResult = "xxx";
        result = Generic.containsSubstring(string, substrings);
        assertEquals("normal string", expResult, result);

        string = "zzzxx";
        substrings = new String[]{"y", "xxx", "vvv"};
        expResult = null;
        result = Generic.containsSubstring(string, substrings);
        assertEquals("substring not found", expResult, result);
    }

    // Test of convertMillisToDate method, of class Generic.
    @Test
    public void testConvertMillisToDate() {
        // 12.08.2010 23:45:19.342
        long millis = 1074528964342L;
        String expResult = "19.01.2004 16:16:04.342";
        String result = Generic.convertMillisToDate(millis);
        assertEquals("default GMT timeZone", expResult, result);

        expResult = "19.01.2004 08:16:04.342";
        result = Generic.convertMillisToDate(millis, "America/Los_Angeles");
        assertEquals("Pacific Day Time timeZone", expResult, result);
    }

    // Test of getFormattedDate method, of class Generic.
    @Test
    public void testGetFormattedDate_0args()
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
    public void testGetFormattedDate_String() {
        // this test can potentially fail in an extremely rare and unlikely case of high processor load and the test being run at the exact moment when the hour changes
        String timeZoneName = "CET";
        String expResult = Generic.convertMillisToDate(System.currentTimeMillis(), "CET");
        expResult = expResult.substring(0, expResult.indexOf(':', expResult.indexOf(' ')) + ":".length());
        String result = Generic.getFormattedDate(timeZoneName);

        assertTrue(result.startsWith(expResult));
    }

    // Test of addCommas method, of class Generic.
    @Test
    public void testAddCommas_Object() {
        Object value = "1203123.48987";
        String expResult = "1,203,123.48987";
        String result = Generic.addCommas(value);
        assertEquals(expResult, result);
    }

    // Test of addCommas method, of class Generic.
    @Test
    public void testAddCommas_double_int() {
        double value = 1203123.48987;
        int nDecimals = 0;
        String expResult = "1,203,123";
        String result = Generic.addCommas(value, nDecimals);
        assertEquals("zero decimals", expResult, result);

        value = 1203123.48987;
        nDecimals = 6;
        expResult = "1,203,123.489870";
        result = Generic.addCommas(value, nDecimals);
        assertEquals("six decimals", expResult, result);
    }

    // Test of addCommas method, of class Generic.
    @Test
    public void testAddCommas_4args() {
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
    public void testIsPureAscii() {
        String string = "faklfjhafl" + (char) 244;
        boolean expResult = false;
        boolean result = Generic.isPureAscii(string);
        assertEquals("non-ASCII", expResult, result);

        string = "faklfjhafl";
        expResult = true;
        result = Generic.isPureAscii(string);
        assertEquals("pure ASCII", expResult, result);
    }

    // Test of byteArrayIndexOf method, of class Generic.
    @Test
    public void testByteArrayIndexOf_byteArr_byteArr() {
        byte[] data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        byte[] pattern = new byte[]{3, 4, 5, 6, 7};
        int expResult = 2;
        int result = Generic.byteArrayIndexOf(data, pattern);
        assertEquals("found", expResult, result);

        data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        pattern = new byte[]{3, -2, 5, 6, 7};
        expResult = -1;
        result = Generic.byteArrayIndexOf(data, pattern);
        assertEquals("not found", expResult, result);
    }

    // Test of byteArrayIndexOf method, of class Generic.
    @Test
    public void testByteArrayIndexOf_3args() {
        byte[] data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        byte[] pattern = new byte[]{3, 4, 5, 6, 7};
        int beginIndex = 0;
        int expResult = 2;
        int result = Generic.byteArrayIndexOf(data, pattern, beginIndex);
        assertEquals("found", expResult, result);

        data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        pattern = new byte[]{3, 4, 5, 6, 7};
        beginIndex = 2;
        expResult = 2;
        result = Generic.byteArrayIndexOf(data, pattern, beginIndex);
        assertEquals("found, from index 2", expResult, result);

        data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        pattern = new byte[]{3, 4, 5, 6, 7};
        beginIndex = 3;
        expResult = -1;
        result = Generic.byteArrayIndexOf(data, pattern, beginIndex);
        assertEquals("not found, from index 3", expResult, result);
    }

    // Test of byteArrayComputeFailure method, of class Generic.
    @Test
    public void testByteArrayComputeFailure() {
        byte[] pattern = new byte[]{3, 4, 5, 6, 7};
        int[] expResult = new int[]{0, 0, 0, 0, 0};
        int[] result = Generic.byteArrayComputeFailure(pattern);
        assertArrayEquals("result with zeroes", expResult, result);

        pattern = new byte[]{3, 4, 3, 6, 8, 3, 4, 9};
        expResult = new int[]{0, 0, 1, 0, 0, 1, 2, 0};
        result = Generic.byteArrayComputeFailure(pattern);
        assertArrayEquals("result with non-zeroes", expResult, result);
    }

    // Test of logOfBase method, of class Generic.
    @Test
    public void testLogOfBase() {
        double base = 2;
        double num = 8;
        double expResult = 3;
        double result = Generic.logOfBase(base, num);
        assertEquals(expResult, result, 0.0);
    }

    // Test of ceilingPowerOf method, of class Generic.
    @Test
    public void testCeilingPowerOf() {
        double base = 2;
        double num = 5;
        double expResult = 8;
        double result = Generic.ceilingPowerOf(base, num);
        assertEquals(expResult, result, 0.0);
    }

    // Test of compareLinkedHashMap method, of class Generic.
    @Test
    public void testCompareLinkedHashMap() {
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
        assertTrue("duplicate", result);

        result = Generic.compareLinkedHashMap(firstMap, secondMap);
        assertFalse("same elements, different order", result);

        result = Generic.compareLinkedHashMap(firstMap, thirdMap);
        assertFalse("different nElements", result);
    }

    // Test of sortByValue method, of class Generic.
    @Test
    public void testSortByValue() {
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
        assertTrue("ascending", Generic.compareLinkedHashMap(expResultAscending, result));

        ascendingOrder = false;
        result = Generic.sortByValue(map, ascendingOrder);
        assertTrue("descending", Generic.compareLinkedHashMap(expResultDescending, result));
    }

    // Test of getRandomElementFromSet method, of class Generic.
    @Test
    public void testGetRandomElementFromSet() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList(new String[]{"AC", "AD", "AE", "AERO", "AF", "AG", "AI", "AL", "AM", "AN"}));
        String result = Generic.getRandomElementFromSet(set);

        assertTrue(set.contains(result));
    }

    // Test of copyFile method, of class Generic.
    @Test
    public void testCopyFile()
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
    public void testConcatArrays() {
        Byte[] firstArray = new Byte[]{1, 2, 3, 4};
        Byte[][] restArrays = new Byte[][]{{5, 6}, {}, {7}, {8, 8, 8, 8, 9, 1}};
        Byte[] expResult = new Byte[]{1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 8, 9, 1};
        Byte[] result = Generic.concatArrays(firstArray, restArrays);

        assertArrayEquals(expResult, result);
    }

    // Test of compressByteArray method, of class Generic.
    @Test
    public void testCompressByteArray()
            throws IOException {
        byte[] bytes = new byte[]{};
        String compressionFormat = "bogus_format";
        byte[] expResult = new byte[]{};
        byte[] result = Generic.compressByteArray(bytes, compressionFormat);
        assertArrayEquals("bogus_format", expResult, result);

        bytes = new byte[]{};
        compressionFormat = "gzip";
        result = Generic.compressByteArray(Generic.compressByteArray(bytes, compressionFormat), compressionFormat);
        assertTrue("gzip", result.length > 0);

        bytes = new byte[]{};
        compressionFormat = "deflate";
        result = Generic.compressByteArray(Generic.compressByteArray(bytes, compressionFormat), compressionFormat);
        assertTrue("deflate", result.length > 0);
    }

    // Test of decompressByteArray method, of class Generic.
    @Test
    public void testDecompressByteArray()
            throws IOException {
        byte[] bytes = new byte[]{1, 1, 1};
        String compressionFormat = "bogus_format";
        byte[] expResult = new byte[]{1, 1, 1};
        byte[] result = Generic.decompressByteArray(bytes, compressionFormat);
        assertArrayEquals("bogus_format", expResult, result);

        bytes = new byte[]{1, 2, 3, 4};
        compressionFormat = "gzip";
        expResult = new byte[]{1, 2, 3, 4};
        result = Generic.decompressByteArray(Generic.compressByteArray(bytes, compressionFormat), compressionFormat);
        assertArrayEquals("gzip", expResult, result);

        bytes = new byte[]{1, 2, 3, 4};
        compressionFormat = "deflate";
        expResult = new byte[]{1, 2, 3, 4};
        result = Generic.decompressByteArray(Generic.compressByteArray(bytes, compressionFormat), compressionFormat);
        assertArrayEquals("deflate", expResult, result);
    }

    // Test of getSubstrings method, of class Generic.
    @Test
    public void testGetSubstrings_3args() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        LinkedList<String> expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "NOSCRIPT", "A", "b", "", "", "", "A"}));
        LinkedList<String> result = Generic.getSubstrings(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    // Test of getSubstrings method, of class Generic.
    @Test
    public void testGetSubstrings_4args() {
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
    public void testGetSubstrings_6args() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        boolean getInterSubstrings = false;
        int nSubstrings = -1;
        LinkedList<String> expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "NOSCRIPT", "A", "b", "", "", "", "A"}));
        LinkedList<String> result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters different", expResult, result);

        harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        firstDelimiter = "<";
        secondDelimiter = ">";
        getInterSubstrings = false;
        nSubstrings = 0;
        expResult = new LinkedList<>(Arrays.asList(new String[]{}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings zero, getInterSubstrings false, delimiters different", expResult, result);

        harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        firstDelimiter = "<";
        secondDelimiter = ">";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "", "NOSCRIPT", "", "A", " "}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters different", expResult, result);

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "NOSCRIPT", "A", " ", "b", "das", "sss", "", "", "A|1"}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings true, delimiters identical", expResult, result);

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = false;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "A", "b", "sss", ""}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters identical", expResult, result);

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "NOSCRIPT", "A", " ", "b", "das"}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters identical", expResult, result);

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "-|NOSCRIPT", "A", "-| ", "b", "-|das"}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters different but with similar parts", expResult, result);

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"", "NOSCRIPT", "-|NOSCRIPT", "A", "-| ", "b", "-|das", "sss", "", "", "A|1"}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings true, delimiters different but with similar parts", expResult, result);

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = false;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList(new String[]{"NOSCRIPT", "A", "b", "sss", ""}));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals("different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters different but with similar parts",
                expResult, result);
    }

    // Test of getSubstringsIgnoreCase method, of class Generic.
    @Test
    public void testGetSubstringsIgnoreCase() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "o";
        String secondDelimiter = "S";
        LinkedList<String> expResult = new LinkedList<>(Arrays.asList(new String[]{"", ""}));
        LinkedList<String> result = Generic.getSubstringsIgnoreCase(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    // Test of getSubstring method, of class Generic.
    @Test
    public void testGetSubstring() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        String expResult = "NOSCRIPT";
        String result = Generic.getSubstring(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    // Test of serializedDeepCopy method, of class Generic.
    @Test
    public void testSerializedDeepCopy() {
        String sourceObject = "test";
        String expResult = "test";
        String result = Generic.serializedDeepCopy(sourceObject);
        assertEquals(expResult, result);
    }

    // Test of synchronizedCopyObjectFields method, of class Generic.
    @Test
    public void testSynchronizedCopyObjectFields()
            throws UnknownHostException {
        LocalTestObject sourceObject = new LocalTestObject("test", 7);
        LocalTestObject destinationObject = new LocalTestObject();

        Generic.synchronizedCopyObjectFields(sourceObject, destinationObject);
        assertEquals(sourceObject, destinationObject);
    }

    // Test of copyObjectFields method, of class Generic.
    @Test
    public void testCopyObjectFields() {
        LocalTestObject sourceObject = new LocalTestObject("test", 7);
        LocalTestObject destinationObject = new LocalTestObject();

        Generic.copyObjectFields(sourceObject, destinationObject);
        assertEquals(sourceObject, destinationObject);
    }

    // Test of objectToString method, of class Generic.
    @Test
    public void testObjectToString_1args() {
        LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test number=0)";
        String result = Generic.objectToString(object);
        assertEquals("first", expResult, result);

        LocalTestObject[] arrayObject = new LocalTestObject[]{object};
        expResult = "[(name=test number=0)]";
        result = Generic.objectToString(arrayObject);
        assertEquals("second", expResult, result);

        ArrayList<LocalTestObject> arrayList = new ArrayList<>(1);
        arrayList.add(object);
        expResult = "[(name=test number=0)]";
        result = Generic.objectToString(arrayList);
        assertEquals("third", expResult, result);

        HashMap<LocalTestObject, LocalTestObject> hashMap = new HashMap<>(2);
        hashMap.put(object, object);
        expResult = "[(key=(name=test number=0) value=(name=test number=0))]";
        result = Generic.objectToString(hashMap);
        assertEquals("fourth", expResult, result);

        expResult = "Thu Jan 01 00:00:00 UTC 1970";
        result = Generic.objectToString(new Date(0L));
        assertEquals("fifth", expResult, result);
    }

    // Test of objectToString method, of class Generic.
    @Test
    public void testObjectToString_2args() {
        LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test)";
        String result = Generic.objectToString(object, false);
        assertEquals(expResult, result);
    }

    // Test of objectToString method, of class Generic.
    @Test
    public void testObjectToString_3args() {
        LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test number=0)";
        String result = Generic.objectToString(object, true, true);
        assertEquals(expResult, result);
    }

    @Test
    public void testObjectToString_6args()
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
    public void testDisableHTTPSValidation() {
        // the all trusting manager is not tested here as I couldn't find a method to do it, but there's no rush, as it seems to work fine
        assertEquals(HttpsURLConnection.getDefaultHostnameVerifier().verify("", null), false);

        Generic.disableHTTPSValidation();

        assertEquals(HttpsURLConnection.getDefaultHostnameVerifier().verify("", null), true);
    }

    // Test of specialCharParser method, of class Generic.
    @Test
    public void testSpecialCharParser() {
        String line = "abcdefghijklmnopqrstuvwxyz";
        String expResult = "abcdefghijklmnopqrstuvwxyz";
        String result = Generic.specialCharParser(line);
        assertEquals("no change", expResult, result);

        line = "abcde&nbsp;fghijk&amp;&amp;lm&#x38;n&#56;op&quot;qrstu&lt;&gt;vwxyz&amp;";
        expResult = "abcde fghijk&&lm8n8op\"qrstu<>vwxyz&";
        result = Generic.specialCharParser(line);
        assertEquals("change", expResult, result);
    }

    // Test of closeStandardStreams method, of class Generic.
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    @Test
    public void testCloseStandardStreams()
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

            assertTrue("before out", outContent.toString().contains("hello out"));
            assertTrue("before err", errContent.toString().contains("hello err"));
            assertTrue("before in", pipedInputStream.available() == 6);

            Generic.closeStandardStreams();

            System.out.print("xxx");
            System.err.print("xxx");
            try {
                pipedOutputStream.write(new byte[3]);
            } catch (IOException ioException) {
                logger.debug("Expected pipe closed IOException", ioException);
            }

            assertTrue("after out", outContent.toString().contains("hello out"));
            assertTrue("after err", errContent.toString().contains("hello err"));
            assertTrue("after in", pipedInputStream.available() == 0);
        } finally {
            Generic.closeObjects(outPrintStream, outContent, errPrintStream, errContent, pipedOutputStream, pipedInputStream);
            System.setOut(originalOut);
            System.setErr(originalErr);
            System.setIn(originalIn);
        }
    }

    @Test
    public void testCheckAtomicBooleans_varargs() {
        AtomicBoolean first = new AtomicBoolean(), second = new AtomicBoolean();

        boolean result = Generic.checkAtomicBooleans(first, second);
        assertFalse("false", result);

        second.set(true);
        result = Generic.checkAtomicBooleans(first, second);
        assertTrue("true", result);
    }

    @Test
    public void testCheckAtomicBooleans_boolean_varargs() {
        AtomicBoolean first = new AtomicBoolean(), second = new AtomicBoolean();

        boolean result = Generic.checkAtomicBooleans(false, first, second);
        assertTrue("true", result);

        first.set(true);
        second.set(true);
        result = Generic.checkAtomicBooleans(false, first, second);
        assertFalse("false", result);
    }

    @Test
    public void testCheckObjects() {
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        AtomicReference<String> atomicReference = new AtomicReference<>();

        boolean result = Generic.checkObjects(atomicBoolean, atomicReference);
        assertFalse("false", result);

        atomicBoolean.set(true);
        result = Generic.checkObjects(atomicBoolean, atomicReference);
        assertTrue("true 1", result);

        atomicBoolean.set(false);
        atomicReference.set("test");
        result = Generic.checkObjects(atomicBoolean, atomicReference);
        assertTrue("true 2", result);
    }

    // Test of threadSleepSegmented_long_AtomicBoolean method, of class Generic.
    // @Test
    // public void testThreadSleepSegmented_long_AtomicBoolean() {
    //     long millis = 1000L;
    //     long timeBefore = System.currentTimeMillis();
    //     Generic.threadSleepSegmented(millis, new AtomicBoolean());
    //     long timeAfter = System.currentTimeMillis();
    //     logger.info("threadSleepSegmented_long_AtomicBoolean() millis slept: {}    millis passed: {}", millis, timeAfter - timeBefore);
    //     assertTrue(timeAfter - timeBefore >= 1000);
    // }
    // Test of threadSleepSegmented_long_long_AtomicBoolean method, of class Generic.
    @Test
    public void testThreadSleepSegmented_long_long_AtomicBoolean() {
        long millis = 1000L;
        long timeBefore = System.currentTimeMillis();

        Generic.threadSleepSegmented(millis, 1L, new AtomicBoolean());

        long timeAfter = System.currentTimeMillis();

        logger.info("threadSleepSegmented_long_long_AtomicBoolean() millis slept: {}    millis passed: {}", millis, timeAfter - timeBefore);
        assertTrue(timeAfter - timeBefore >= 1000);
    }

    // Test of threadSleep method, of class Generic.
    @Test
    public void testThreadSleep() {
        long millis = 1000L;
        long timeBefore = System.currentTimeMillis();

        Generic.threadSleep(millis);

        long timeAfter = System.currentTimeMillis();

        logger.info("testThreadSleep() millis slept: {}    millis passed: {}", millis, timeAfter - timeBefore);
        assertTrue(timeAfter - timeBefore >= 1000);
    }

    // Test of setFinalStatic method, of class Generic.
    @Test
    public void testSetFinalStatic()
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
    public void testGetField() {
        String expName = "testName";
        int expNumber = 123;

        LocalTestObject localTestObject = new LocalTestObject(expName, expNumber);

        String resultName = (String) Generic.getField(localTestObject, "name");
        assertEquals("name", expName, resultName);

        int resultNumber = (Integer) Generic.getField(localTestObject, "number");
        assertEquals("number", expNumber, resultNumber);

        assertNull("null", Generic.getField(localTestObject, "bogus"));
    }

    @Test
    public void testSetField() {
        String expName = "testName";
        int expNumber = 123;

        LocalTestObject localTestObject = new LocalTestObject();

        boolean expResult = true;
        boolean result = Generic.setField(localTestObject, "name", expName);
        assertEquals("result name", expResult, result);
        assertEquals("name", expName, localTestObject.getName());

        expResult = true;
        result = Generic.setField(localTestObject, "number", expNumber);
        assertEquals("result number", expResult, result);
        assertEquals("number", expNumber, localTestObject.getNumber());

        // NoSuchFieldException is expected
        expResult = false;
        result = Generic.setField(localTestObject, "bogus", expNumber);
        assertEquals("result bogus", expResult, result);
    }

    @Test
    public void testTurnOffHtmlUnitLogger() {
        // I won't be testing this method; it works and it will be obvious if it doesn't
    }
}
