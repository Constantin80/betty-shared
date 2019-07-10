package info.fmro.shared.utility;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

@SuppressWarnings({"ClassWithTooManyConstructors", "CyclicClassDependency"})
public class SynchronizedWriter {
    private static final Logger logger = LoggerFactory.getLogger(SynchronizedWriter.class);
    private static final String DEFAULT_CHARSET = Generic.UTF8_CHARSET;
    private static final int DEFAULT_BUFFER_SIZE = 32 << 10; // 32 * 1024
    private int encryptionKey;
    private String charsetName, id;
    private File file;
    private BufferedWriter bufferedWriter;
    private OutputStreamWriter outputStreamWriter;
    private BufferedOutputStream bufferedOutputStream;
    private FileOutputStream fileOutputStream;

    @SuppressWarnings("WeakerAccess")
    public SynchronizedWriter(final String fileName, final boolean append, final String charsetName, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, append, Charset.forName(charsetName), bufferSize, id);
    }

    @SuppressWarnings("unused")
    public SynchronizedWriter(final String fileName, final boolean append, final String charsetName, final int bufferSize)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, append, Charset.forName(charsetName), bufferSize, fileName);
    }

    public SynchronizedWriter(final String fileName, final boolean append, final String id)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, append, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE, id);
    }

    public SynchronizedWriter(final String fileName, final boolean append)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, append, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE, fileName);
    }

    public SynchronizedWriter(final File file, final boolean append)
            throws java.io.FileNotFoundException {
        this.initialize(file, append, DEFAULT_CHARSET, DEFAULT_BUFFER_SIZE, file.getPath());
    }

    @Contract(pure = true)
    public SynchronizedWriter(final int encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @SuppressWarnings({"SameParameterValue", "ParameterHidesMemberVariable"})
    private synchronized void initialize(final String fileName, final boolean append, final String charsetName, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        // this.close();

        this.file = new File(fileName);
        this.id = id;
        this.charsetName = charsetName;

        try {
            this.fileOutputStream = new FileOutputStream(this.file, append);
            this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, bufferSize);
            this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, this.charsetName);
            this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedWriter.initialize: {}", this.charsetName, unsupportedEncodingException);
        }
    }

    @SuppressWarnings("ParameterHidesMemberVariable")
    private synchronized void initialize(final String fileName, final boolean append, @NotNull final Charset charset, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        // this.close();

        this.file = new File(fileName);
        this.id = id;
        this.charsetName = charset.name();

        this.fileOutputStream = new FileOutputStream(this.file, append);
        this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, bufferSize);
        this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, charset);
        this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, bufferSize);
    }

    @SuppressWarnings({"ParameterHidesMemberVariable", "SameParameterValue"})
    private synchronized void initialize(final File file, final boolean append, final String charsetName, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        // this.close();

        this.file = new File(file, "");
        this.id = id;
        this.charsetName = charsetName;

        try {
            this.fileOutputStream = new FileOutputStream(this.file, append);
            this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, bufferSize);
            this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, this.charsetName);
            this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedWriter.initialize (file): {}", this.charsetName, unsupportedEncodingException);
        }
    }

    @SuppressWarnings({"unused", "ParameterHidesMemberVariable"})
    public synchronized void initialize(final File file, final boolean append, @NotNull final Charset charset, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        this.close();

        this.file = new File(file, "");
        this.id = id;
        this.charsetName = charset.name();
        this.fileOutputStream = new FileOutputStream(this.file, append);
        this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, bufferSize);
        this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, charset);
        this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, bufferSize);
    }

    public synchronized void initialize(final String fileName, final boolean append)
            throws java.io.FileNotFoundException {
        this.close();

        this.file = new File(fileName);
        this.id = fileName;
        this.charsetName = DEFAULT_CHARSET;

        try {
            this.fileOutputStream = new FileOutputStream(this.file, append);
            this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, DEFAULT_BUFFER_SIZE);
            this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, this.charsetName);
            this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, DEFAULT_BUFFER_SIZE);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedWriter.initialize: {}", this.charsetName, unsupportedEncodingException);
        }
    }

    @SuppressWarnings({"unused", "ParameterHidesMemberVariable"})
    public synchronized void initialize(final String fileName, final boolean append, final String charsetName)
            throws java.io.FileNotFoundException {
        this.close();

        this.file = new File(fileName);
        this.id = fileName;
        this.charsetName = charsetName;

        try {
            this.fileOutputStream = new FileOutputStream(this.file, append);
            this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, DEFAULT_BUFFER_SIZE);
            this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, this.charsetName);
            this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, DEFAULT_BUFFER_SIZE);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedWriter.initialize: {}", this.charsetName, unsupportedEncodingException);
        }
    }

    public synchronized String getCharsetName() {
        return this.charsetName;
    }

    public synchronized void setCharsetName(final String charsetName) {
        this.charsetName = charsetName;
    }

    public synchronized String getId() {
        return this.id;
    }

    public synchronized long getUsableSpace() {
        return this.file != null ? this.file.getUsableSpace() : 0L;
    }

    public synchronized boolean writeAndFlush(final String writeString) {
        boolean success;

//        if (this.encryptionKey != 0) {
//            writeString = Generic.encryptString(writeString, this.encryptionKey);
//        }
        success = this.write(writeString);
        if (success) {
            success = this.flush();
        }
        return success;
    }

    public synchronized boolean write(final String writeString, final int encryption) {
        return this.write(Generic.encryptString(writeString, encryption));
    }

    public synchronized boolean write(final String writeString) {
        boolean success;
        final String toBeWritten = this.encryptionKey == 0 ? writeString : Generic.encryptString(writeString, this.encryptionKey);

        try {
            this.bufferedWriter.write(toBeWritten);
            success = true;
        } catch (IOException iOException) {
            logger.error("iOException in SynchronizedWriter.write", iOException);
            success = false;
        }
        return success;
    }

    public synchronized boolean flush() {
        boolean success;

        try {
            if (this.bufferedWriter != null) {
                this.bufferedWriter.flush();
                success = true;
            } else {
                success = false;
            }
        } catch (IOException iOException) {
            logger.error("iOException in SynchronizedWriter.flush", iOException);
            success = false;
        }
        return success;
    }

    public synchronized boolean[] close() {
        return Generic.closeObjects(this.bufferedWriter, this.outputStreamWriter, this.bufferedOutputStream, this.fileOutputStream);
    }
}
