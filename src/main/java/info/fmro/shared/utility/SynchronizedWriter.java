package info.fmro.shared.utility;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizedWriter {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedWriter.class);
    private static final String DEFAULT_CHARSET = Generic.UTF8_CHARSET;
    private static final int DEFAULT_BUFFER_SIZE = 32 * 1024;
    private int encryptionKey;
    private String charset, id;
    private File file;
    private BufferedWriter bufferedWriter;
    private OutputStreamWriter outputStreamWriter;
    private BufferedOutputStream bufferedOutputStream;
    private FileOutputStream fileOutputStream;

    public SynchronizedWriter(final String fileName, final boolean append, final String charsetName, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        this.initialize(fileName, append, Charset.forName(charsetName), bufferSize, id);
    }

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

    public SynchronizedWriter(final int encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public SynchronizedWriter() {
    }

    private void initialize(final String fileName, final boolean append, final String charset, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        // this.close();

        this.file = new File(fileName);
        this.id = id;
        this.charset = charset;

        try {
            this.fileOutputStream = new FileOutputStream(this.file, append);
            this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, bufferSize);
            this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, this.charset);
            this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedWriter.initialize: {}", this.charset, unsupportedEncodingException);
        }
    }

    private void initialize(final String fileName, final boolean append, final Charset charset, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        // this.close();

        this.file = new File(fileName);
        this.id = id;
        this.charset = charset.name();

        this.fileOutputStream = new FileOutputStream(this.file, append);
        this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, bufferSize);
        this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, charset);
        this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, bufferSize);
    }

    private void initialize(final File file, final boolean append, final String charset, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        // this.close();

        this.file = new File(file, "");
        this.id = id;
        this.charset = charset;

        try {
            this.fileOutputStream = new FileOutputStream(this.file, append);
            this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, bufferSize);
            this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, this.charset);
            this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, bufferSize);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedWriter.initialize (file): {}", this.charset, unsupportedEncodingException);
        }
    }

    public synchronized void initialize(final File file, final boolean append, final Charset charset, final int bufferSize, final String id)
            throws java.io.FileNotFoundException {
        this.close();

        this.file = new File(file, "");
        this.id = id;
        this.charset = charset.name();
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
        this.charset = DEFAULT_CHARSET;

        try {
            this.fileOutputStream = new FileOutputStream(this.file, append);
            this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, DEFAULT_BUFFER_SIZE);
            this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, this.charset);
            this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, DEFAULT_BUFFER_SIZE);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedWriter.initialize: {}", this.charset, unsupportedEncodingException);
        }
    }

    public synchronized void initialize(final String fileName, final boolean append, final String charset)
            throws java.io.FileNotFoundException {
        this.close();

        this.file = new File(fileName);
        this.id = fileName;
        this.charset = charset;

        try {
            this.fileOutputStream = new FileOutputStream(this.file, append);
            this.bufferedOutputStream = new BufferedOutputStream(this.fileOutputStream, DEFAULT_BUFFER_SIZE);
            this.outputStreamWriter = new OutputStreamWriter(this.bufferedOutputStream, this.charset);
            this.bufferedWriter = new BufferedWriter(this.outputStreamWriter, DEFAULT_BUFFER_SIZE);
        } catch (java.io.UnsupportedEncodingException unsupportedEncodingException) {
            logger.error("UnsupportedEncodingException in SynchronizedWriter.initialize: {}", this.charset, unsupportedEncodingException);
        }
    }

    public synchronized String getCharset() {
        return charset;
    }

    public synchronized void setCharset(final String charset) {
        this.charset = charset;
    }

    public synchronized String getId() {
        return this.id;
    }

    public synchronized long getUsableSpace() {
        if (this.file != null) {
            return this.file.getUsableSpace();
        } else {
            return 0L;
        }
    }

    @SuppressWarnings("AssignmentToMethodParameter")
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

    @SuppressWarnings("AssignmentToMethodParameter")
    public synchronized boolean write(String writeString) {
        boolean success;

        if (this.encryptionKey != 0) {
            writeString = Generic.encryptString(writeString, this.encryptionKey);
        }

        try {
            this.bufferedWriter.write(writeString);
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
