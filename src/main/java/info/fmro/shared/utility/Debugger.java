package info.fmro.shared.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Debugger {
    private static final Logger logger = LoggerFactory.getLogger(Debugger.class);
    private int debugLevel;
    private int encryption;
    private LinkedHashMap<SynchronizedWriter, Integer> writersMap = new LinkedHashMap<>(4, 0.75f); // sorted by values at all times
    private long timeLastCheckDiskSpace;

    @SuppressWarnings("unused")
    public Debugger(final int level, final int encryptionKey) {
        this.debugLevel = level;
        this.encryption = encryptionKey;
    }

    @SuppressWarnings("unused")
    public Debugger(final int encryptionKey) {
        this.encryption = encryptionKey;
    }

    public Debugger() {
    }

    public synchronized void setDebugLevel(final int newValue) {
        this.debugLevel = newValue;
    }

    public synchronized void setEncryption(final int newValue) {
        this.encryption = newValue;
    }

    public synchronized void addWriter(final String fileName, final boolean append, final String charsetName, final int bufferSize, final String id, final int minDebugLevel)
            throws java.io.FileNotFoundException {
        this.writersMap.put(new SynchronizedWriter(fileName, append, charsetName, bufferSize, id), minDebugLevel);
        this.writersMap = Generic.sortByValue(this.writersMap, true);
    }

    public synchronized void addWriter(final String fileName, final boolean append, final String id, final int minDebugLevel)
            throws java.io.FileNotFoundException {
        this.writersMap.put(new SynchronizedWriter(fileName, append, id), minDebugLevel);
        this.writersMap = Generic.sortByValue(this.writersMap, true);
    }

    public synchronized void addWriter(final String fileName, final boolean append, final int minDebugLevel)
            throws java.io.FileNotFoundException {
        this.writersMap.put(new SynchronizedWriter(fileName, append), minDebugLevel);
        this.writersMap = Generic.sortByValue(this.writersMap, true);
    }

    public synchronized void checkDiskSpace() {
        if (this.debugLevel >= 1 && (new File(".").getUsableSpace() < 50L * Generic.MEGABYTE || !this.enoughAvailableSpace(50L * Generic.MEGABYTE))) {
            logger.info("Less than 50Mb remain on disk. Reducing debugLevel. (space = {})", new File(".").getUsableSpace());

            this.flush(1, this.debugLevel);
            this.debugLevel = 0;
        } else if (this.debugLevel >= 2 && !this.enoughAvailableSpace(200L * Generic.MEGABYTE)) {
            logger.info("Less than 200Mb remain on disk. Reducing debugLevel.");

            this.flush(2, this.debugLevel);
            this.debugLevel = 1;
        } else if (this.debugLevel >= 3 && !this.enoughAvailableSpace(500L * Generic.MEGABYTE)) {
            logger.info("Less than 500Mb remain on disk. Reducing debugLevel.");

            this.flush(3, this.debugLevel);
            this.debugLevel = 2;
        } else { // debugLevel doesn't need to be reduced
        }

        timeLastCheckDiskSpaceStamp();
    }

    public synchronized boolean enoughAvailableSpace(final long neededSpace) {
        boolean enoughSpace = true;

        for (final Map.Entry<SynchronizedWriter, Integer> entry : this.writersMap.entrySet()) {
            if (entry.getValue() <= this.debugLevel) {
                if (entry.getKey().getUsableSpace() < neededSpace) {
                    enoughSpace = false;
                    break;
                }
            } else {
                break; // the following values are all greater than this.debugLevel
            }
        }
        return enoughSpace;
    }

    public synchronized int getDebugLevel() {
        return this.debugLevel;
    }

    public synchronized int getEncryption() {
        return this.encryption;
    }

    public synchronized long getTimeLastCheckDiskSpace() {
        return this.timeLastCheckDiskSpace;
    }

    public synchronized void timeLastCheckDiskSpaceStamp() {
        this.timeLastCheckDiskSpace = System.currentTimeMillis();
    }

    public synchronized boolean write(final String writeString, final String writerId, final int minDebugLevel) {
        return this.debugLevel >= minDebugLevel && write(writeString, writerId);
    }

    public synchronized boolean write(final String writeString, final String writerId) {
        boolean success = false;
        for (final Map.Entry<SynchronizedWriter, Integer> entry : this.writersMap.entrySet()) {
            final SynchronizedWriter writer = entry.getKey();
            if (entry.getValue() <= this.debugLevel) {
                final String currentWriterId = writer.getId();

                if (Objects.equals(currentWriterId, writerId)) {
                    success = writer.write(writeString, this.encryption);
                    if (!success) {
                        logger.error("STRANGE no success in Debugger.write");
                        checkDiskSpace();
                    }
                    break;
                }
            } else {
                break; // the following values are all greater than this.debugLevel
            }
        } // end for

        return success;
    }

    public synchronized void flush(final int minDebugLevel, final int maxDebugLevel) {
        for (final Map.Entry<SynchronizedWriter, Integer> entry : this.writersMap.entrySet()) {
            final int writerDebugLevel = entry.getValue();

            if (writerDebugLevel >= minDebugLevel) {
                if (writerDebugLevel <= maxDebugLevel) {
                    entry.getKey().flush();
                } else {
                    break; // the following values are all greater than maxDebugLevel
                }
            }
        }
    }

    public synchronized void flush() {
        for (final SynchronizedWriter writer : this.writersMap.keySet()) {
            writer.flush();
        }
    }

    public synchronized void close() {
        for (final SynchronizedWriter writer : this.writersMap.keySet()) {
            writer.close();
        }
    }
}
