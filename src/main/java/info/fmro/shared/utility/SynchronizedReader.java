package info.fmro.shared.utility;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

@SuppressWarnings("CyclicClassDependency")
public class SynchronizedReader {
    private static final Logger logger = LoggerFactory.getLogger(SynchronizedReader.class);
    private static final String DEFAULT_CHARSET = Generic.UTF8_CHARSET;
    private static final int DEFAULT_BUFFER_SIZE = 32 << 10; // 32 * 1_024
    private int decryptionKey; // careful, "-decryptionKey" is used to decrypt the string, not "+decryptionKey"
    private String charsetName;
    private BufferedReader bufferedReader;
    private InputStreamReader inputStreamReader;
    private BufferedInputStream bufferedInputStream;
    private FileInputStream fileInputStream;

    @SuppressWarnings("unused")
    public SynchronizedReader(final String fileName, final String charsetName, final int bufferSize)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, Charset.forName(charsetName), bufferSize);
    }

    public SynchronizedReader(final String fileName)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
    }

    public SynchronizedReader(final String fileName, final int decryptionKey)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
        this.decryptionKey = decryptionKey;
    }

    public SynchronizedReader(final File file)
            throws java.io.FileNotFoundException {
        this.initialize(file, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
    }

    @SuppressWarnings("SameParameterValue")
    private synchronized void initialize(final String fileName, final String charset, final int bufferSize)
            throws java.io.FileNotFoundException {
        // this.close();

        this.charsetName = charset;

        try {
            this.fileInputStream = new FileInputStream(fileName);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize : {}", charset, unsupportedEncodingException);
        }
    }

    private synchronized void initialize(final String fileName, @NotNull final Charset charset, final int bufferSize)
            throws java.io.FileNotFoundException {
        // this.close();

        this.charsetName = charset.name();

        this.fileInputStream = new FileInputStream(fileName);
        this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
        this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
        this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
    }

    @SuppressWarnings("SameParameterValue")
    private synchronized void initialize(final File file, final String charset, final int bufferSize)
            throws java.io.FileNotFoundException {
        // this.close();

        this.charsetName = charset;

        try {
            this.fileInputStream = new FileInputStream(file);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize (file): {}", charset, unsupportedEncodingException);
        }
    }

    @SuppressWarnings("unused")
    public synchronized void initialize(final File file, @NotNull final Charset charset, final int bufferSize)
            throws java.io.FileNotFoundException {
        this.close();

        this.charsetName = charset.name();

        this.fileInputStream = new FileInputStream(file);
        this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
        this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
        this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
    }

    @SuppressWarnings("unused")
    public synchronized void initialize(final String fileName)
            throws java.io.FileNotFoundException {
        this.close();

        this.charsetName = DEFAULT_CHARSET;

        try {
            this.fileInputStream = new FileInputStream(fileName);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, DEFAULT_BUFFER_SIZE);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, this.charsetName);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, DEFAULT_BUFFER_SIZE);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize : {}", this.charsetName, unsupportedEncodingException);
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
