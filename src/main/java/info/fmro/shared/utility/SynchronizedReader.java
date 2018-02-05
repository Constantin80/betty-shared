package info.fmro.shared.utility;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizedReader {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedReader.class);
    private static final String DEFAULT_CHARSET = Generic.UTF8_CHARSET;
    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    private int decryptionKey; // careful, "-decryptionKey" is used to decrypt the string, not "+decryptionKey"
    private String charset;
    private BufferedReader bufferedReader;
    private InputStreamReader inputStreamReader;
    private BufferedInputStream bufferedInputStream;
    private FileInputStream fileInputStream;

    public SynchronizedReader(String fileName, String charsetName, int bufferSize)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, Charset.forName(charsetName), bufferSize);
    }

    public SynchronizedReader(String fileName)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
    }

    public SynchronizedReader(String fileName, int decryptionKey)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
        this.decryptionKey = decryptionKey;
    }

    public SynchronizedReader(File file)
            throws java.io.FileNotFoundException {
        this.initialize(file, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE);
    }

    public SynchronizedReader(int decryptionKey) {
        this.decryptionKey = decryptionKey;
    }

    public SynchronizedReader() {
    }

    private void initialize(String fileName, String charset, int bufferSize)
            throws java.io.FileNotFoundException {
        // this.close();

        this.charset = charset;

        try {
            this.fileInputStream = new FileInputStream(fileName);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize : {}", charset, unsupportedEncodingException);
        }
    }

    private void initialize(String fileName, Charset charset, int bufferSize)
            throws java.io.FileNotFoundException {
        // this.close();

        this.charset = charset.name();

        this.fileInputStream = new FileInputStream(fileName);
        this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
        this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
        this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
    }

    private void initialize(File file, String charset, int bufferSize)
            throws java.io.FileNotFoundException {
        // this.close();

        this.charset = charset;

        try {
            this.fileInputStream = new FileInputStream(file);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize (file): {}", charset, unsupportedEncodingException);
        }
    }

    public synchronized void initialize(File file, Charset charset, int bufferSize)
            throws java.io.FileNotFoundException {
        this.close();

        this.charset = charset.name();

        this.fileInputStream = new FileInputStream(file);
        this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, bufferSize);
        this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
        this.bufferedReader = new BufferedReader(this.inputStreamReader, bufferSize);
    }

    public synchronized void initialize(String fileName)
            throws java.io.FileNotFoundException {
        this.close();

        this.charset = DEFAULT_CHARSET;

        try {
            this.fileInputStream = new FileInputStream(fileName);
            this.bufferedInputStream = new BufferedInputStream(this.fileInputStream, DEFAULT_BUFFER_SIZE);
            this.inputStreamReader = new InputStreamReader(this.bufferedInputStream, charset);
            this.bufferedReader = new BufferedReader(this.inputStreamReader, DEFAULT_BUFFER_SIZE);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedReader.initialize : {}", charset, unsupportedEncodingException);
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

    public synchronized String readLine(int encryption)
            throws java.io.IOException {
        // careful, "-encryption" is used to decrypt the string, not "+encryption"
        return Generic.encryptString(this.readLine(), -encryption);
    }

    public synchronized boolean[] close() {
        return Generic.closeObjects(this.bufferedReader, this.inputStreamReader, this.bufferedInputStream, this.fileInputStream);
    }
}
