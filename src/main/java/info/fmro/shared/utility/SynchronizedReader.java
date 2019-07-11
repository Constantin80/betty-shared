package info.fmro.shared.utility;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class SynchronizedReader {
    private static final Logger logger = LoggerFactory.getLogger(SynchronizedReader.class);
    private static final String DEFAULT_CHARSET = Generic.UTF8_CHARSET;
    private static final int DEFAULT_BUFFER_SIZE = 32 << 10; // 32 * 1_024
    private int decryptionKey; // careful, "-decryptionKey" is used to decrypt the string, not "+decryptionKey"
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized") // I couldn't find where it is accessed in unsynchronized context; it might be an IntelliJ inspection bug
    private String charsetName;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized") // I couldn't find where it is accessed in unsynchronized context; it might be an IntelliJ inspection bug
    private BufferedReader bufferedReader;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized") // I couldn't find where it is accessed in unsynchronized context; it might be an IntelliJ inspection bug
    private InputStreamReader inputStreamReader;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized") // I couldn't find where it is accessed in unsynchronized context; it might be an IntelliJ inspection bug
    private BufferedInputStream bufferedInputStream;
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized") // I couldn't find where it is accessed in unsynchronized context; it might be an IntelliJ inspection bug
    private FileInputStream fileInputStream;

    @SuppressWarnings("unused")
    private SynchronizedReader(final String fileName, final String charsetName, final int bufferSize) {
        this.initialize(fileName, Charset.forName(charsetName), bufferSize);
    }

    public SynchronizedReader(final String fileName) {
        this.initialize(fileName, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
    }

    public SynchronizedReader(final String fileName, final int decryptionKey) {
        this.initialize(fileName, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
        this.decryptionKey = decryptionKey;
    }

    public SynchronizedReader(final File file) {
        this.initialize(file, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
    }

    private synchronized void initialize(final String fileName, final String charset, final int bufferSize) {
        this.charsetName = charset;
        try {
            this.fileInputStream = new FileInputStream(fileName);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize : {}", charset, unsupportedEncodingException);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in SynchronizedReader.initialize1 : {} {} {}", fileName, charset, bufferSize, e);
        }
    }

    private synchronized void initialize(final String fileName, @NotNull final Charset charset, final int bufferSize) {
        this.charsetName = charset.name();
        try {
            this.fileInputStream = new FileInputStream(fileName);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in SynchronizedReader.initialize2 : {} {} {}", fileName, charset, bufferSize, e);
        }
    }

    private synchronized void initialize(final File file, final String charset, final int bufferSize) {
        this.charsetName = charset;
        try {
            this.fileInputStream = new FileInputStream(file);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize (file): {}", charset, unsupportedEncodingException);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in SynchronizedReader.initialize3 : {} {} {}", Generic.objectToString(file), charset, bufferSize, e);
        }
    }

    public synchronized void initialize(final File file, @NotNull final Charset charset, final int bufferSize) {
        this.close();
        this.charsetName = charset.name();
        try {
            this.fileInputStream = new FileInputStream(file);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in SynchronizedReader.initialize4 : {} {} {}", Generic.objectToString(file), charset, bufferSize, e);
        }
    }

    public synchronized void initialize(final String fileName) {
        this.close();
        this.charsetName = DEFAULT_CHARSET;
        try {
            this.fileInputStream = new FileInputStream(fileName);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, DEFAULT_BUFFER_SIZE);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, this.charsetName);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, DEFAULT_BUFFER_SIZE);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize : {}", this.charsetName, unsupportedEncodingException);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in SynchronizedReader.initialize5 : {}", fileName, e);
        }
    }

    public synchronized String readLine()
            throws java.io.IOException {
        String returnString = this.bufferedReader == null ? null : this.bufferedReader.readLine();
        if (this.decryptionKey != 0) {
            returnString = Generic.encryptString(returnString, -this.decryptionKey);
        }

        return returnString;
    }

    public synchronized String readLine(final int encryption)
            throws java.io.IOException {
        // careful, "-encryption" is used to decrypt the string, not "+encryption"
        return Generic.encryptString(this.readLine(), -encryption);
    }

    public synchronized boolean[] close() {
        return Generic.closeObjects(this.bufferedReader, this.inputStreamReader, this.bufferedInputStream, this.fileInputStream);
    }
}
