package info.fmro.shared.utility;

import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.stream.cache.market.PriceSize;
import info.fmro.shared.stream.objects.RunnerId;
import junitx.framework.FileAssert;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ConstantConditions")
class GenericTest {
    static final String BOGUS = "bogus";

    private static class LocalDebugger
            extends Debugger { // dummy testing class
    }

    @SuppressWarnings({"NonFinalFieldReferenceInEquals", "NonFinalFieldReferencedInHashCode"})
    static class LocalTestObject {
        private String name = "";
        private int number;

        @SuppressWarnings("WeakerAccess")
        @Contract(pure = true)
        public LocalTestObject() {
        }

        @Contract(pure = true)
        LocalTestObject(final String name, final int number) {
            this.name = name;
            this.number = number;
        }

        String getName() {
            return this.name;
        }

        int getNumber() {
            return this.number;
        }

        @Contract(value = "null -> false", pure = true)
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final LocalTestObject that = (LocalTestObject) obj;
            return this.number == that.number &&
                   Objects.equals(this.name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.number);
        }
    }

    @Test
    void millisecondsToSecondsString() {
        String result = Generic.millisecondsToSecondsString(0L);
        assertEquals("0.000", result, "0");
        result = Generic.millisecondsToSecondsString(1L);
        assertEquals("0.001", result, "1");
        result = Generic.millisecondsToSecondsString(100L);
        assertEquals("0.100", result, "2");
        result = Generic.millisecondsToSecondsString(1_200L);
        assertEquals("1.20", result, "3");
        result = Generic.millisecondsToSecondsString(60_001L);
        assertEquals("60.0", result, "4");
        result = Generic.millisecondsToSecondsString(400_000L);
        assertEquals("400", result, "5");
        result = Generic.millisecondsToSecondsString(3_600_000L);
        assertEquals("3,600", result, "6");
        result = Generic.millisecondsToSecondsString(2_111_003_600_000L);
        assertEquals("2,111,003,600", result, "7");
    }

    @Test
    void getClosestNumber() {
        int result = Generic.getClosestNumber(5);
        assertEquals(5, result, "1");

        result = Generic.getClosestNumber(5, 1);
        assertEquals(1, result, "2");

        result = Generic.getClosestNumber(5, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
        assertEquals(5, result, "3");

        result = Generic.getClosestNumber(5, 1, 2, 3, 4, 6, 7, 8, 9, 0);
        assertEquals(4, result, "4");

        result = Generic.getClosestNumber(5, 1, 2, 3, 6, 7, 8, 9, 0);
        assertEquals(6, result, "5");
    }

    @Test
    void getConstructor()
            throws Exception {
        final Constructor<LocalTestObject> constructor = Generic.getConstructor(LocalTestObject.class, String.class, int.class);
        final LocalTestObject obj = constructor.newInstance("John", 11);
        final String name = obj.getName();
        assertEquals("John", name, "name");
        final Constructor<LocalTestObject> goodConstructor = Generic.getConstructor(LocalTestObject.class);
        assertNotNull(goodConstructor, "not null");
        final Constructor<LocalTestObject> badConstructor = Generic.getConstructor(LocalTestObject.class, double.class);
        assertNull(badConstructor, " null");
    }

    @Test
    void getMethod()
            throws Exception {
        final Method method = Generic.getMethod(LocalTestObject.class, "getName");
        final LocalTestObject obj = new LocalTestObject("John", 11);
        final String name = (String) method.invoke(obj);
        assertEquals("John", name, "name");

        final Method badMethod = Generic.getMethod(LocalTestObject.class, "getname");
        assertNull(badMethod, "null");
    }

    @Test
    void objectInstanceOf() {
        final Object managedRunner = new ManagedRunner("id", new RunnerId(1234L, 9876.123d), new AtomicBoolean(), new AtomicBoolean());
        final Object exposureObjectClass = Exposure.class;
        final Class<?> exposureClass = (Class<?>) exposureObjectClass;
        assertTrue(exposureClass.isAssignableFrom(managedRunner.getClass()), "1");
        assertTrue(exposureClass.isInstance(managedRunner), "2");
        assertTrue(Generic.objectInstanceOf(exposureClass, managedRunner), "3");
    }

    @Test
    void updateObject() {
        final LocalTestObject mainObject = new LocalTestObject("Alex", 88), updateObject = new LocalTestObject("blah", 32);
        Generic.updateObject(mainObject, updateObject);
        assertEquals(mainObject, updateObject);
    }

    @Test
    void parseDouble() {
        String initialString = null;
        double result = Generic.parseDouble(initialString);
        assertTrue(Double.isNaN(result), "1");

        initialString = "";
        result = Generic.parseDouble(initialString);
        assertTrue(Double.isNaN(result), "2");

        initialString = " ";
        result = Generic.parseDouble(initialString);
        assertTrue(Double.isNaN(result), "3");

        initialString = "-";
        result = Generic.parseDouble(initialString);
        assertTrue(Double.isNaN(result), "4");

        initialString = "1.00";
        result = Generic.parseDouble(initialString);
        assertEquals(1d, result, "5");

        //noinspection RedundantCast
        assertTrue(Double.isNaN((Double) Double.NaN), "6");
    }

    @Test
    void splitStringAroundSpaces() {
        String initialString = "";
        String[] result = Generic.splitStringAroundSpaces(initialString);
        assertEquals(0, result.length, "1: " + Generic.objectToString(result));

        initialString = " ";
        result = Generic.splitStringAroundSpaces(initialString);
        assertEquals(0, result.length, "2: " + Generic.objectToString(result));

        initialString = "      ";
        result = Generic.splitStringAroundSpaces(initialString);
        assertEquals(0, result.length, "3: " + Generic.objectToString(result));

        initialString = "     xxx    sasasa uuu     ";
        result = Generic.splitStringAroundSpaces(initialString);
        assertArrayEquals(new String[]{"xxx", "sasasa", "uuu"}, result, "4: " + Generic.objectToString(result));
    }

    @Test
    void booleanToInt() {
        int expected = 1;
        int result = Generic.booleanToInt(true);
        assertEquals(expected, result, "true");

        expected = 0;
        result = Generic.booleanToInt(false);
        assertEquals(expected, result, "false");
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
            throws Exception {
        LocalTestObject result = new LocalTestObject();
        assertEquals("", result.getName(), "default");
        result = Generic.createAndFill(LocalTestObject.class);
        assertNotEquals("", result.getName(), "random");
        result = Generic.createAndFill(LocalTestObject.class, 10);
        assertNull(result.getName(), "recursionCounter==10");
    }

    @Test
    void fillRandom()
            throws Exception {
        final LocalTestObject result = new LocalTestObject();
        assertEquals("", result.getName(), "default");
        Generic.fillRandom(result);
        assertNotEquals("", result.getName(), "random");
        Generic.fillRandom(result, 10);
        assertNull(result.getName(), "recursionCounter==10");
    }

    @Test
    void getRandomValueForField()
            throws Exception {
        final LocalTestObject testObject = new LocalTestObject();
        final Field field = LocalTestObject.class.getDeclaredField("name");
        field.setAccessible(true);
        assertEquals("", field.get(testObject), "default");

        Object result = Generic.getRandomValueForField(field, 0);
        assertNotEquals("", result, "random");
        assertSame(String.class, field.getType(), "type");
        result = Generic.getRandomValueForField(field, 10);
        assertNull(result, "recursionCounter==10");
    }

    @Test
    void createStringFromCodes() {
        String result = Generic.createStringFromCodes(32, 32, 32);
        assertEquals("   ", result, "1");

        result = Generic.createStringFromCodes(32, 32, 0);
        assertEquals("  \u0000", result, "2");
    }

    @Test
    void getStringCodePointValues() {
        final String symbol_194_160 = "Â" + '\u00a0'; // "Â" == 194
        final String result = Generic.getStringCodePointValues(symbol_194_160);
        assertEquals("194 160", result);
    }

    @Test
    void properTimeStamp() {
        final String value = Generic.properTimeStamp();
        assertEquals(23, value.length());
    }

    @Test
    void properTimeStamp_long() {
        String value = Generic.properTimeStamp(500L);
        assertEquals(23, value.length());
        value = Generic.properTimeStamp(0L);
        assertEquals(23, value.length());
    }

    @Test
    void getSubclasses() {
        final Set<Class<? extends Debugger>> set = Generic.getSubclasses("info.fmro", Debugger.class);
        final Collection<Class<? extends Debugger>> expected = new HashSet<>(0);
        expected.add(LocalDebugger.class);

        assertEquals(expected, set);
    }

    @Test
    void collectionKeepMultiples() {
        final Collection<Integer> list = new ArrayList<>(7);
        list.add(1);
        list.add(2);
        list.add(2);
        list.add(3);
        list.add(3);
        list.add(3);
        list.add(4);
        final Collection<Integer> expected = List.of(2, 3, 3);
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
        final StringBuilder stringBuilder = new StringBuilder("abcd");
        final String result = "xyz";
        Generic.stringBuilderReplace(stringBuilder, result);
        assertEquals(result, stringBuilder.toString());
    }

    @Test
    void middleValue_double() {
        final double a = 3.62378947368421d;
        final double b = 1.3231d;
        final double c = 3.623789473684211d;
        final double result;
        final double expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertEquals(expResult, result);
    }

    @Test
    void middleValue_float() {
        final float a = 3.62378947368421f;
        final float b = 3.3231f;
        final float c = 3.623789473684211f;
        final float result;
        final float expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertEquals(expResult, result);
    }

    @Test
    void middleValue_int() {
        final int a = 3;
        final int b = 1;
        final int c = 1;
        final int result;
        final int expResult;

        result = Generic.middleValue(a, b, c);
        expResult = c;
        assertEquals(expResult, result);
    }

    @Test
    void middleValue_long() {
        final long a = 3L;
        final long b = 2L;
        final long c = 3L;
        final long result;
        final long expResult;

        result = Generic.middleValue(a, b, c);
        expResult = a;
        assertEquals(expResult, result);
    }

    @Test
    void truncateDouble() {
        double size = 3.62378947368421d;
        double result = Generic.truncateDouble(size, 2);
        double expResult = 3.62d;
        assertEquals(expResult, result, "1");

        size = 3.62978947368421d;
        result = Generic.truncateDouble(size, 2);
        expResult = 3.62d;
        assertEquals(expResult, result, "2");
    }

    @Test
    void quotedReplaceAll() {
        String initialString = "abc'-&  u. ", pattern = ". ", replacement = " ", expResult = "abc'-&  u ";
        String result = Generic.quotedReplaceAll(initialString, pattern, replacement);
        assertEquals(expResult, result, "first");

        initialString = " ";
        pattern = " ";
        replacement = "";
        expResult = ""; // it's a special character, non breaking space 194+160
        result = Generic.quotedReplaceAll(initialString, pattern, replacement);
        assertEquals(expResult, result, "second");
    }

    @SuppressWarnings("OverlyLongMethod")
    @Test
    void stringMatchChance_String_String() {
        @Nullable String first = "", second = "";
        double expResult = 0;
        double result = Generic.stringMatchChance(first, second);
        assertEquals(expResult, result, "first");

        first = null;
        second = null;
        expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertEquals(expResult, result, "second");

        first = "Ab ";
        second = "   ab";
        expResult = 1;
        result = Generic.stringMatchChance(first, second);
        assertEquals(expResult, result, "third");

        first = "a";
        second = "b";
        expResult = 0;
        result = Generic.stringMatchChance(first, second);
        assertEquals(expResult, result, "fourth");

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
        assertEquals(expResult, result, "first");

        first = "A";
        second = "A ";
        expResult = 1;
        result = Generic.stringMatchChance(first, second, false);
        assertEquals(expResult, result, "second");
    }

    @Test
    void stringMatchChance_String_String_boolean_boolean() {
        String first = " ", second = " ";
        final double expResult = 1;
        double result = Generic.stringMatchChance(first, second, true, false);
        assertEquals(expResult, result, "first");

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
            final String fileName = Generic.tempFileName("test");
            final File file = new File(fileName);
            try {
                Generic.printStackTraces(fileName);
                assertTrue(file.length() > 0);
            } finally {
                file.delete();
            }
        });
    }

    // Test of printStackTraces method, of class Generic.
    @SuppressWarnings("ImplicitDefaultCharsetUsage")
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
        final Map<Thread, StackTraceElement[]> stacksMap = Thread.getAllStackTraces();
        final StackTraceElement[] stackTraceElementArray = stacksMap.values().iterator().next();
        final String fileName = Generic.tempFileName("test");
        final File file = new File(fileName);

        try {
            Generic.printStackTrace(stackTraceElementArray, fileName);

            assertTrue(file.length() > 0);
        } finally {
            file.delete();
        }
    }

    // Test of printStackTrace method, of class Generic.
    @Test
    @SuppressWarnings({"null", "ImplicitDefaultCharsetUsage"})
    public void printStackTrace_StackTraceElementArr_PrintStream() {
        final Map<Thread, StackTraceElement[]> stacksMap = Thread.getAllStackTraces();
        final StackTraceElement[] stackTraceElementArray = stacksMap.values().iterator().next();
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
        final boolean[] expResult = {true, true, true, true};
        final boolean[] result = Generic.closeObjects(new Socket(), new Socket(), new Socket(), new Socket());
        assertArrayEquals(expResult, result);
    }

    // Test of setSoLinger method, of class Generic.
    @Test
    void setSoLinger()
            throws SocketException {
        final Socket socket = new Socket();
        try {
            final boolean expResult = true;
            final boolean result = Generic.setSoLinger(socket);

            assertEquals(expResult, result, "testSetSoLinger boolean result");
            assertEquals(0, socket.getSoLinger(), "testSetSoLinger setSoLinger");
        } finally {
            Generic.closeObject(socket);
        }
    }

    // Test of closeObject method, of class Generic.
    @Test
    void closeObject() {
        final Socket socket = new Socket();
        try {
            final boolean expResult = true;
            final boolean result = Generic.closeObject(socket);

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
        final byte[] a = new byte[55];
        final int startIndexA = -1;
        final int endIndexA = 53;
        final byte[] b = new byte[50];
        final int startIndexB = 11;
        final int endIndexB = 47;
        a[1] = 2;
        b[45] = 1;
        @SuppressWarnings("PointlessArithmeticExpression") final byte[] expResult = new byte[53 - 0 + 47 - 11];
        expResult[1] = 2;
        expResult[expResult.length - 2] = 1;
        final byte[] result = Generic.concatByte(a, startIndexA, endIndexA, b, startIndexB, endIndexB);

        assertArrayEquals(expResult, result);
    }

    // Test of encryptString method, of class Generic.
    @Test
    void encryptString() {
        final String s = "abcE";
        final int encryptKey = 3;
        final String expResult = "defH";
        final String result = Generic.encryptString(s, encryptKey);

        assertEquals(expResult, result);
    }

    // Test of encryptFile method, of class Generic.
    @Test
    void encryptFile()
            throws IOException {
        final String initialString = "abc\r\nfgh";
        final int encryptKey = 3;
        final String expResult = "def\r\nijk\r\n";
        final StringBuilder result = new StringBuilder(initialString.length() + 2);
        SynchronizedWriter synchronizedWriter = null;
        SynchronizedReader synchronizedReader = null;
        final String fileName = Generic.tempFileName("testEncryptFile");
        boolean success;

        try {
            synchronizedWriter = new SynchronizedWriter(fileName, false);
            synchronizedWriter.write(initialString);
            synchronizedWriter.flush();
            Generic.closeObject(synchronizedWriter);

            success = Generic.encryptFile(fileName, encryptKey);

            synchronizedReader = new SynchronizedReader(fileName);
            String fileLine = synchronizedReader.readLine();
            while (fileLine != null) {
                result.append(fileLine).append("\r\n");
                fileLine = synchronizedReader.readLine();
            } // end while
        } finally {
            Generic.closeObjects(synchronizedWriter, synchronizedReader);
            new File(fileName).delete();
        }

        assertTrue(success);
        assertEquals(expResult, result.toString());
    }

    // Test of tempFileName method, of class Generic.
    @Test
    void tempFileName() {
        final String fileName = "test";
        final String result = Generic.tempFileName(fileName);
        final String result2 = Generic.tempFileName(fileName);

        assertFalse(new File(result).exists(), "exists");
        assertNotEquals(result, result2, "equals");
    }

    // Test of readObjectFromFile, writeObjectToFile, synchronizedWriteObjectToFile methods, of class Generic.
    @Test
    void readWriteObjectFromFile() {
        final String fileName = Generic.tempFileName("test");
        final int[] intArray = {1, 2, 5, 1, 3213, 6554211, 132312312};
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
        final byte[] byteArray = {12, 22, 111, 2, 0, -12};
        final String expResult = "0c166f0200f4";
        final String result = Generic.getHexString(byteArray);

        assertEquals(expResult, result);
    }

    @Test
    void backwardWordsString() {
        String s = "Ajaccio GFCO";
        String expResult = "GFCO Ajaccio";
        String result = Generic.backwardWordsString(s);

        assertEquals(expResult, result, "1");

        s = " first  second third";
        expResult = "third second  first ";
        result = Generic.backwardWordsString(s);

        assertEquals(expResult, result, "2");
    }

    // Test of backwardString method, of class Generic.
    @Test
    void backwardString() {
        final String s = "test123";
        final String expResult = "321tset";
        final String result = Generic.backwardString(s);

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

        assertEquals(expResult, result, BOGUS);
    }

    // Test of goodPort method, of class Generic.
    @Test
    void goodPort() {
        String tempPort = "ujklljkh";
        boolean expResult = false;
        boolean result = Generic.goodPort(tempPort);

        assertEquals(expResult, result, BOGUS);

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
    @SuppressWarnings("OverlyLongMethod")
    @Test
    void goodDomain() {
        @Nullable String host = "google.com";
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
        final String result = Generic.getUserAgent();

        //noinspection DuplicateStringLiteralInspection
        assertTrue(result.startsWith("Mozilla/"), "startsWith");
        assertEquals(')', result.charAt(result.length() - 1), "endsWith");
    }

    // Test of getSocksType method, of class Generic.
    @Test
    void getSocksType() {
        String proxyType = "soCks4";
        byte expResult = 4;
        byte result = Generic.getSocksType(proxyType);

        //noinspection DuplicateStringLiteralInspection
        assertEquals(expResult, result, "socks4");

        proxyType = "soCKs5";
        expResult = 5;
        result = Generic.getSocksType(proxyType);

        //noinspection DuplicateStringLiteralInspection
        assertEquals(expResult, result, "socks5");

        // logger.warn is expected
        proxyType = "dhlskadh89sadnjm,n908l,.,";
        expResult = 5;
        result = Generic.getSocksType(proxyType);

        assertEquals(expResult, result, BOGUS);
    }

    // Test of linkRemoveProtocol method, of class Generic.
    @Test
    void linkRemoveProtocol() {
        final String link = "http://dsdsa.com:80/?query=1";
        final String expResult = "dsdsa.com:80/?query=1";
        final String result = Generic.linkRemoveProtocol(link);

        assertEquals(expResult, result);
    }

    // Test of linkRemovePort method, of class Generic.
    @Test
    void linkRemovePort() {
        final String link = "http://dsdsa.com:80/?query=1";
        final String expResult = "http://dsdsa.com/?query=1";
        final String result = Generic.linkRemovePort(link);

        assertEquals(expResult, result);
    }

    // Test of linkRemoveQuery method, of class Generic.
    @Test
    void linkRemoveQuery() {
        final String link = "http://dsdsa.com:80/?query=1";
        final String expResult = "http://dsdsa.com:80/";
        final String result = Generic.linkRemoveQuery(link);

        assertEquals(expResult, result);
    }

    // Test of getLinkHost method, of class Generic.
    @Test
    void getLinkHost() {
        @Nullable String link = "http://dsDsa.com:80/?query=1";
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
        String s = "";
        String[] substrings = null;
        @Nullable String expResult = null;
        String result = Generic.containsSubstring(s, substrings);
        assertEquals(expResult, result, "null");

        s = "xxx";
        substrings = new String[]{"", "x"};
        expResult = "";
        result = Generic.containsSubstring(s, substrings);
        assertEquals(expResult, result, "empty string");

        s = "xxx";
        substrings = new String[]{"y", "xxx", "vvv", ""};
        expResult = "xxx";
        result = Generic.containsSubstring(s, substrings);
        assertEquals(expResult, result, "normal string");

        s = "zzzxx";
        substrings = new String[]{"y", "xxx", "vvv"};
        expResult = null;
        result = Generic.containsSubstring(s, substrings);
        assertEquals(expResult, result, "substring not found");
    }

    // Test of convertMillisToDate method, of class Generic.
    @Test
    void convertMillisToDate() {
        // 12.08.2010 23:45:19.342
        final long millis = 1074528964342L;
        String expResult = "19.01.2004 16:16:04.342";
        String result = Generic.convertMillisToDate(millis);
        assertEquals(expResult, result, "default GMT timeZone");

        expResult = "19.01.2004 08:16:04.342";
        result = Generic.convertMillisToDate(millis, "America/Los_Angeles");
        assertEquals(expResult, result, "Pacific Day Time timeZone");
    }

    // Test of getFormattedDate method, of class Generic.
    @Test
    void getFormattedDate_0args() {
        // this test can potentially fail in an extremely rare and unlikely case of high processor load and the test being run at the exact moment when the hour changes
        // GregorianCalendar gregorianCalendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        String expResult = Generic.convertMillisToDate(System.currentTimeMillis());
        expResult = expResult.substring(0, expResult.indexOf(':', expResult.indexOf(' ')) + ":".length());
        final String result = Generic.getFormattedDate();

        assertTrue(result.startsWith(expResult));
    }

    // Test of getFormattedDate method, of class Generic.
    @Test
    void getFormattedDate_String() {
        // this test can potentially fail in an extremely rare and unlikely case of high processor load and the test being run at the exact moment when the hour changes
        final String timeZoneName = "CET";
        String expResult = Generic.convertMillisToDate(System.currentTimeMillis(), "CET");
        expResult = expResult.substring(0, expResult.indexOf(':', expResult.indexOf(' ')) + ":".length());
        final String result = Generic.getFormattedDate(timeZoneName);

        assertTrue(result.startsWith(expResult));
    }

    // Test of addCommas method, of class Generic.
    @Test
    void addCommas_Object() {
        final Object value = "1203123.48987";
        final String expResult = "1,203,123.48987";
        final String result = Generic.addCommas(value);
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
        final String s = "012345678901234567890-1234567890";
        final byte groupSize = 5;
        final String commaDelimiter = ":";
        final String periodDelimiter = "-";
        final String expResult = "0:12345:67890:12345:67890-1234567890";
        final String result = Generic.addCommas(s, groupSize, commaDelimiter, periodDelimiter);
        assertEquals(expResult, result);
    }

    // Test of isPureAscii method, of class Generic.
    @Test
    void isPureAscii() {
        String s = "faklfjhafl" + (char) 244;
        boolean expResult = false;
        boolean result = Generic.isPureAscii(s);
        assertEquals(expResult, result, "non-ASCII");

        s = "faklfjhafl";
        expResult = true;
        result = Generic.isPureAscii(s);
        assertEquals(expResult, result, "pure ASCII");
    }

    // Test of byteArrayIndexOf method, of class Generic.
    @Test
    void byteArrayIndexOf_byteArr_byteArr() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        byte[] pattern = {3, 4, 5, 6, 7};
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
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5};
        byte[] pattern = {3, 4, 5, 6, 7};
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
        byte[] pattern = {3, 4, 5, 6, 7};
        int[] expResult = {0, 0, 0, 0, 0};
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
        final double base = 2;
        final double num = 8;
        final double expResult = 3;
        final double result = Generic.logOfBase(base, num);
        assertEquals(expResult, result);
    }

    // Test of ceilingPowerOf method, of class Generic.
    @Test
    void ceilingPowerOf() {
        final double base = 2;
        final double num = 5;
        final double expResult = 8;
        final double result = Generic.ceilingPowerOf(base, num);
        assertEquals(expResult, result);
    }

    // Test of compareSortedLinkedHashMap method, of class Generic.
    @Test
    void compareLinkedHashMap() {
        final LinkedHashMap<String, Long> firstMap = new LinkedHashMap<>(16), firstMapDuplicate = new LinkedHashMap<>(16), secondMap = new LinkedHashMap<>(16), thirdMap = new LinkedHashMap<>(16);
        firstMap.put("Standard1", 110L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMap.put("Sockslist", 210L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMap.put("Proxyhttp", 410L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMap.put("Freeproxylist", 910L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMap.put("Standard2", 820L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMap.put("Nntime", 220L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMap.put("Proxyspeedtest", 320L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMap.put("Proxybridge", 120L * Generic.MINUTE_LENGTH_MILLISECONDS);

        firstMapDuplicate.put("Standard1", 110L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMapDuplicate.put("Sockslist", 210L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMapDuplicate.put("Proxyhttp", 410L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMapDuplicate.put("Freeproxylist", 910L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMapDuplicate.put("Standard2", 820L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMapDuplicate.put("Nntime", 220L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMapDuplicate.put("Proxyspeedtest", 320L * Generic.MINUTE_LENGTH_MILLISECONDS);
        firstMapDuplicate.put("Proxybridge", 120L * Generic.MINUTE_LENGTH_MILLISECONDS);

        secondMap.put("Standard1", 110L * Generic.MINUTE_LENGTH_MILLISECONDS);
        secondMap.put("Proxybridge", 120L * Generic.MINUTE_LENGTH_MILLISECONDS);
        secondMap.put("Sockslist", 210L * Generic.MINUTE_LENGTH_MILLISECONDS);
        secondMap.put("Nntime", 220L * Generic.MINUTE_LENGTH_MILLISECONDS);
        secondMap.put("Proxyspeedtest", 320L * Generic.MINUTE_LENGTH_MILLISECONDS);
        secondMap.put("Proxyhttp", 410L * Generic.MINUTE_LENGTH_MILLISECONDS);
        secondMap.put("Standard2", 820L * Generic.MINUTE_LENGTH_MILLISECONDS);
        secondMap.put("Freeproxylist", 910L * Generic.MINUTE_LENGTH_MILLISECONDS);

        thirdMap.put("Standard1", 110L * Generic.MINUTE_LENGTH_MILLISECONDS);
        thirdMap.put("Sockslist", 210L * Generic.MINUTE_LENGTH_MILLISECONDS);
        thirdMap.put("Proxyhttp", 410L * Generic.MINUTE_LENGTH_MILLISECONDS);
        thirdMap.put("Standard2", 820L * Generic.MINUTE_LENGTH_MILLISECONDS);
        thirdMap.put("Nntime", 220L * Generic.MINUTE_LENGTH_MILLISECONDS);
        thirdMap.put("Proxyspeedtest", 320L * Generic.MINUTE_LENGTH_MILLISECONDS);
        thirdMap.put("Proxybridge", 120L * Generic.MINUTE_LENGTH_MILLISECONDS);

        boolean result = Generic.compareSortedLinkedHashMap(firstMap, firstMapDuplicate);
        assertTrue(result, "duplicate");

        result = Generic.compareSortedLinkedHashMap(firstMap, secondMap);
        assertFalse(result, "same elements, different order");

        result = Generic.compareSortedLinkedHashMap(firstMap, thirdMap);
        assertFalse(result, "different nElements");
    }

    // Test of sortByValue method, of class Generic.
    @Test
    void sortByValue() {
        final Map<String, Long> map = new LinkedHashMap<>(16);
        final LinkedHashMap<String, Long> expResultAscending = new LinkedHashMap<>(16), expResultDescending = new LinkedHashMap<>(16);

        map.put("Standard1", 110L * Generic.MINUTE_LENGTH_MILLISECONDS);
        map.put("Sockslist", 210L * Generic.MINUTE_LENGTH_MILLISECONDS);
        map.put("Proxyhttp", 410L * Generic.MINUTE_LENGTH_MILLISECONDS);
        map.put("Freeproxylist", 910L * Generic.MINUTE_LENGTH_MILLISECONDS);
        map.put("Standard2", 820L * Generic.MINUTE_LENGTH_MILLISECONDS);
        map.put("Nntime", 220L * Generic.MINUTE_LENGTH_MILLISECONDS);
        map.put("Proxyspeedtest", 320L * Generic.MINUTE_LENGTH_MILLISECONDS);
        map.put("Proxybridge", 120L * Generic.MINUTE_LENGTH_MILLISECONDS);

        expResultAscending.put("Standard1", 110L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultAscending.put("Proxybridge", 120L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultAscending.put("Sockslist", 210L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultAscending.put("Nntime", 220L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultAscending.put("Proxyspeedtest", 320L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultAscending.put("Proxyhttp", 410L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultAscending.put("Standard2", 820L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultAscending.put("Freeproxylist", 910L * Generic.MINUTE_LENGTH_MILLISECONDS);

        expResultDescending.put("Freeproxylist", 910L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultDescending.put("Standard2", 820L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultDescending.put("Proxyhttp", 410L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultDescending.put("Proxyspeedtest", 320L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultDescending.put("Nntime", 220L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultDescending.put("Sockslist", 210L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultDescending.put("Proxybridge", 120L * Generic.MINUTE_LENGTH_MILLISECONDS);
        expResultDescending.put("Standard1", 110L * Generic.MINUTE_LENGTH_MILLISECONDS);

        boolean ascendingOrder = true;
        LinkedHashMap<String, Long> result = Generic.sortByValue(map, ascendingOrder);
        assertTrue(Generic.compareSortedLinkedHashMap(expResultAscending, result), "ascending");

        ascendingOrder = false;
        result = Generic.sortByValue(map, ascendingOrder);
        assertTrue(Generic.compareSortedLinkedHashMap(expResultDescending, result), "descending");
    }

    // Test of getRandomElementFromSet method, of class Generic.
    @Test
    void getRandomElementFromSet() {
        final Collection<String> set = new LinkedHashSet<>(Arrays.asList("AC", "AD", "AE", "AERO", "AF", "AG", "AI", "AL", "AM", "AN"));
        final String result = Generic.getRandomElementFromSet(set);

        assertTrue(set.contains(result));
    }

    // Test of copyFile method, of class Generic.
    @Test
    void copyFile()
            throws IOException {
        final String sourceFileName = Generic.tempFileName("sourceTest");
        final String destFileName = Generic.tempFileName("destTest");
        final File sourceFile = new File(sourceFileName);
        final File destFile = new File(destFileName);
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
        final Byte[] firstArray = {1, 2, 3, 4};
        final Byte[][] restArrays = {{5, 6}, {}, {7}, {8, 8, 8, 8, 9, 1}};
        final Byte[] expResult = {1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 8, 9, 1};
        final Byte[] result = Generic.concatArrays(firstArray, restArrays);

        assertArrayEquals(expResult, result);
    }

    // Test of compressByteArray method, of class Generic.
    @Test
    void compressByteArray()
            throws IOException {
        byte[] bytes = {};
        String compressionFormat = "bogus_format";
        final byte[] expResult = {};
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
        byte[] bytes = {1, 1, 1};
        String compressionFormat = "bogus_format";
        byte[] expResult = {1, 1, 1};
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
        final String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        final String firstDelimiter = "<";
        final String secondDelimiter = ">";
        final LinkedList<String> expResult = new LinkedList<>(Arrays.asList("NOSCRIPT", "NOSCRIPT", "A", "b", "", "", "", "A"));
        final LinkedList<String> result = Generic.getSubstrings(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    // Test of getSubstrings method, of class Generic.
    @Test
    void getSubstrings_4args() {
        final String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        final String firstDelimiter = "<";
        final String secondDelimiter = ">";
        final boolean getInterSubstrings = false;
        final LinkedList<String> expResult = new LinkedList<>(Arrays.asList("NOSCRIPT", "NOSCRIPT", "A", "b", "", "", "", "A"));
        final LinkedList<String> result = Generic.getSubstrings(harvestInputString, firstDelimiter, secondDelimiter, getInterSubstrings);
        assertEquals(expResult, result);
    }

    // Test of getSubstrings method, of class Generic.
    @SuppressWarnings("OverlyLongMethod")
    @Test
    void getSubstrings_6args() {
        String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        String firstDelimiter = "<";
        String secondDelimiter = ">";
        boolean getInterSubstrings = false;
        int nSubstrings = -1;
        LinkedList<String> expResult = new LinkedList<>(Arrays.asList("NOSCRIPT", "NOSCRIPT", "A", "b", "", "", "", "A"));
        LinkedList<String> result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters different");

        harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        firstDelimiter = "<";
        secondDelimiter = ">";
        getInterSubstrings = false;
        nSubstrings = 0;
        expResult = new LinkedList<>(Collections.emptyList());
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings zero, getInterSubstrings false, delimiters different");

        harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        searchInputString = "<NOsCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        firstDelimiter = "<";
        secondDelimiter = ">";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList("", "NOSCRIPT", "", "NOSCRIPT", "", "A", " "));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters different");

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList("", "NOSCRIPT", "NOSCRIPT", "A", " ", "b", "das", "sss", "", "", "A|1"));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings true, delimiters identical");

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = false;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList("NOSCRIPT", "A", "b", "sss", ""));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters identical");

        harvestInputString = "|NOSCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        searchInputString = "|NosCRIPT|NOSCRIPT|A| |b|das|sss|||A|1";
        firstDelimiter = "|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList("", "NOSCRIPT", "NOSCRIPT", "A", " ", "b", "das"));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters identical");

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = 7;
        expResult = new LinkedList<>(Arrays.asList("", "NOSCRIPT", "-|NOSCRIPT", "A", "-| ", "b", "-|das"));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings limited, getInterSubstrings true, delimiters different but with similar parts");

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = true;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList("", "NOSCRIPT", "-|NOSCRIPT", "A", "-| ", "b", "-|das", "sss", "", "", "A|1"));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings true, delimiters different but with similar parts");

        harvestInputString = "|-|NOSCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        searchInputString = "|-|NosCRIPT|-|NOSCRIPT|-|A|-| |-|b|-|das|-|sss||-||A|1";
        firstDelimiter = "|-|";
        secondDelimiter = "|";
        getInterSubstrings = false;
        nSubstrings = -1;
        expResult = new LinkedList<>(Arrays.asList("NOSCRIPT", "A", "b", "sss", ""));
        result = Generic.getSubstrings(harvestInputString, searchInputString, firstDelimiter, secondDelimiter, getInterSubstrings, nSubstrings);
        assertEquals(expResult, result, "different harvestInputString/searchInputString, nSubstrings negative, getInterSubstrings false, delimiters different but with similar parts");
    }

    // Test of getSubstringsIgnoreCase method, of class Generic.
    @Test
    void getSubstringsIgnoreCase() {
        final String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        final String firstDelimiter = "o";
        final String secondDelimiter = "S";
        final LinkedList<String> expResult = new LinkedList<>(Arrays.asList("", ""));
        final LinkedList<String> result = Generic.getSubstringsIgnoreCase(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    // Test of getSubstring method, of class Generic.
    @Test
    void getSubstring() {
        final String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        final String firstDelimiter = "<";
        final String secondDelimiter = ">";
        final String expResult = "NOSCRIPT";
        final String result = Generic.getSubstring(harvestInputString, firstDelimiter, secondDelimiter);
        assertEquals(expResult, result);
    }

    @Test
    void removeSubstring() {
        final String harvestInputString = "<NOSCRIPT><NOSCRIPT><A> <b>das<>sss<><><A>1";
        final String firstDelimiter = "<";
        final String secondDelimiter = ">";
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
        final String sourceObject = "test";
        final String expResult = "test";
        final String result = Generic.serializedDeepCopy(sourceObject);
        assertEquals(expResult, result);
    }

    // Test of synchronizedCopyObjectFields method, of class Generic.
    @Test
    void synchronizedCopyObjectFields() {
        final LocalTestObject sourceObject = new LocalTestObject("test", 7);
        final LocalTestObject destinationObject = new LocalTestObject();

        Generic.synchronizedCopyObjectFields(sourceObject, destinationObject);
        assertEquals(sourceObject, destinationObject);
    }

    // Test of copyObjectFields method, of class Generic.
    @Test
    void copyObjectFields() {
        final LocalTestObject sourceObject = new LocalTestObject("test", 7);
        final LocalTestObject destinationObject = new LocalTestObject();

        Generic.copyObjectFields(sourceObject, destinationObject);
        assertEquals(sourceObject, destinationObject);
    }

    // Test of objectToString method, of class Generic.
    @Test
    void objectToString_1args() {
        final LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test number=0)";
        String result = Generic.objectToString(object);
        assertEquals(expResult, result, "first");

        final LocalTestObject[] arrayObject = {object};
        expResult = "[(name=test number=0)]";
        result = Generic.objectToString(arrayObject);
        assertEquals(expResult, result, "second");

        final Collection<LocalTestObject> arrayList = new ArrayList<>(1);
        arrayList.add(object);
        expResult = "[(name=test number=0)]";
        result = Generic.objectToString(arrayList);
        assertEquals(expResult, result, "third");

        final Map<LocalTestObject, LocalTestObject> hashMap = new HashMap<>(2);
        hashMap.put(object, object);
        expResult = "[(key=(name=test number=0) value=(name=test number=0))]";
        result = Generic.objectToString(hashMap);
        assertEquals(expResult, result, "fourth");

        expResult = "Thu Jan 01 00:00:00 UTC 1970";
        result = Generic.objectToString(new Date(0L));
        assertEquals(expResult, result, "fifth");

        final Map<Double, PriceSize> priceToSize = new TreeMap<>();
        priceToSize.put(1.1d, new PriceSize(List.of(1.1d, 0.2d)));
        priceToSize.put(1.5d, new PriceSize(List.of(1.5d, 0.6d)));
        priceToSize.put(1.05d, new PriceSize(List.of(1.05d, 0.1d)));
        result = Generic.objectToString(priceToSize);
        expResult = "[(key=1.05 value=(price=1.05 size=0.1)), (key=1.1 value=(price=1.1 size=0.2)), (key=1.5 value=(price=1.5 size=0.6))]";
        assertEquals(expResult, result, "6");
    }

    // Test of objectToString method, of class Generic.
    @Test
    void objectToString_2args() {
        final LocalTestObject object = new LocalTestObject("test", 0);
        final String expResult = "(name=test)";
        final String result = Generic.objectToString(object, false);
        assertEquals(expResult, result);
    }

    // Test of objectToString method, of class Generic.
    @Test
    void objectToString_3args() {
        final LocalTestObject object = new LocalTestObject("test", 0);
        final String expResult = "(name=test number=0)";
        final String result = Generic.objectToString(object, true, true);
        assertEquals(expResult, result);
    }

    @Test
    void objectToString_6args() {
        final LocalTestObject object = new LocalTestObject("test", 0);
        String expResult = "(name=test)";
        String result = Generic.objectToString(object, false, true, false, 10);
        assertEquals(expResult, result);

        expResult = "()";
        result = Generic.objectToString(object, false, true, false, 10, "name");
        assertEquals(expResult, result);
    }

    @Test
    void removeNewLine() {
        final String expResult = "test\\n test\\n test\\n test\\n test";
        final String result = Generic.removeNewLine("test\r\ntest\n\rtest\ntest\rtest");
        assertEquals(expResult, result);
    }

    // Test of disableHTTPSValidation method, of class Generic.
    @Test
    void disableHTTPSValidation() {
        // the all trusting manager is not tested here as I couldn't find a method to do it, but there's no rush, as it seems to work fine
        assertFalse(HttpsURLConnection.getDefaultHostnameVerifier().verify("", null), "before");

        Generic.disableHTTPSValidation();

        assertTrue(HttpsURLConnection.getDefaultHostnameVerifier().verify("", null), "after");
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
    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "ImplicitDefaultCharsetUsage", "NestedTryStatement"})
    @Test
    void closeStandardStreams()
            throws IOException {
        ByteArrayOutputStream outContent = null, errContent = null;
        PipedInputStream pipedInputStream = null;
        PipedOutputStream pipedOutputStream = null;
        PrintStream outPrintStream = null, errPrintStream = null;

        final PrintStream originalOut = System.out;
        final PrintStream originalErr = System.err;
        final InputStream originalIn = System.in;

        try {
            outContent = new ByteArrayOutputStream();
            outPrintStream = new PrintStream(outContent);
            errContent = new ByteArrayOutputStream();
            errPrintStream = new PrintStream(errContent);
            pipedInputStream = new PipedInputStream();
            //noinspection resource,IOResourceOpenedButNotSafelyClosed
            pipedOutputStream = new PipedOutputStream(pipedInputStream);

            System.setIn(pipedInputStream);
            System.setOut(outPrintStream);
            System.setErr(errPrintStream);

            System.out.print("hello out");
            System.err.print("hello err");
            pipedOutputStream.write(new byte[6]);

            assertTrue(outContent.toString().contains("hello out"), "before out");
            assertTrue(errContent.toString().contains("hello err"), "before err");
            assertEquals(6, pipedInputStream.available(), "before in");

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
            assertEquals(0, pipedInputStream.available(), "after in");
        } finally {
            Generic.closeObjects(outPrintStream, outContent, errPrintStream, errContent, pipedOutputStream, pipedInputStream);
            System.setOut(originalOut);
            System.setErr(originalErr);
            System.setIn(originalIn);
        }
    }

    @Test
    void checkAtomicBooleans_varargs() {
        final AtomicBoolean first = new AtomicBoolean();
        final AtomicBoolean second = new AtomicBoolean();

        boolean result = Generic.checkAtomicBooleans(first, second);
        assertFalse(result, "false");

        second.set(true);
        result = Generic.checkAtomicBooleans(first, second);
        assertTrue(result, "true");
    }

    @Test
    void checkAtomicBooleans_boolean_varargs() {
        final AtomicBoolean first = new AtomicBoolean();
        final AtomicBoolean second = new AtomicBoolean();

        boolean result = Generic.checkAtomicBooleans(false, first, second);
        assertTrue(result, "true");

        first.set(true);
        second.set(true);
        result = Generic.checkAtomicBooleans(false, first, second);
        assertFalse(result, "false");
    }

    @Test
    void checkObjects() {
        final AtomicBoolean atomicBoolean = new AtomicBoolean();
        final AtomicReference<String> atomicReference = new AtomicReference<>();

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
        final long millis = 1000L;
        final long timeBefore = System.currentTimeMillis();

        Generic.threadSleepSegmented(millis, 1L, new AtomicBoolean());

        final long timeAfter = System.currentTimeMillis();

//        logger.info("threadSleepSegmented_long_long_AtomicBoolean() millis slept: {}    millis passed: {}", millis, timeAfter - timeBefore);
        assertTrue(timeAfter - timeBefore >= 1000);
    }

    // Test of threadSleep method, of class Generic.
    @Test
    void threadSleep() {
        final long millis = 1000L;
        final long timeBefore = System.currentTimeMillis();

        Generic.threadSleep(millis);

        final long timeAfter = System.currentTimeMillis();

//        logger.info("testThreadSleep() millis slept: {}    millis passed: {}", millis, timeAfter - timeBefore);
        assertTrue(timeAfter - timeBefore >= 1000);
    }

//    @Test
//    void setFinalStatic()
//            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
//        final Field field = LocalTestObject.class.getDeclaredField("serialVersionUID");
//        final long newValue = 33L;
//        final LocalTestObject instance = new LocalTestObject();
//
//        Generic.setFinalStatic(field, newValue);
//        boolean fieldAccessible = field.canAccess(instance);
//        if (!fieldAccessible) {
//            field.setAccessible(true);
//        }
//        final long result = field.getLong(LocalTestObject.class);
//
//        if (fieldAccessible != field.canAccess(instance)) {
//            field.setAccessible(fieldAccessible);
//        }
//
//        assertEquals(result, newValue);
//    }

    @Test
    void getField() {
        final String expName = "testName";
        final int expNumber = 123;

        final LocalTestObject localTestObject = new LocalTestObject(expName, expNumber);

        final String resultName = (String) Generic.getField(localTestObject, "name");
        assertEquals(expName, resultName, "name");

        final int resultNumber = (Integer) Generic.getField(localTestObject, "number");
        assertEquals(expNumber, resultNumber, "number");

        assertNull(Generic.getField(localTestObject, BOGUS), "null");
    }

    @Test
    void setField() {
        final String expName = "testName";
        final int expNumber = 123;

        final LocalTestObject localTestObject = new LocalTestObject();

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
        result = Generic.setField(localTestObject, BOGUS, expNumber);
        assertEquals(expResult, result, "result bogus");
    }

    @SuppressWarnings("JUnitTestMethodWithNoAssertions")
    @Test
    void turnOffHtmlUnitLogger() {
        // I won't be testing this method; it works and it will be obvious if it doesn't
    }
}
