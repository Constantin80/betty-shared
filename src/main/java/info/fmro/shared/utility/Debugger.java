package info.fmro.shared.utility;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Debugger {

    private static final Logger logger = LoggerFactory.getLogger(Debugger.class);
    private int debugLevel, encryption;
    private LinkedHashMap<SynchronizedWriter, Integer> writersMap = new LinkedHashMap<>(4, 0.75f); // sorted by values at all times
    private long timeLastCheckDiskSpace;

    public Debugger(int debugLevel, int encryption) {
        this.debugLevel = debugLevel;
        this.encryption = encryption;
    }

    public Debugger(int encryption) {
        this.encryption = encryption;
    }

    public Debugger() {
    }

    public synchronized void setDebugLevel(int newValue) {
        this.debugLevel = newValue;
    }

    public synchronized void setEncryption(int newValue) {
        this.encryption = newValue;
    }

    public synchronized void addWriter(String fileName, boolean append, String charsetName, int bufferSize, String id, int minDebugLevel)
            throws java.io.FileNotFoundException {
        this.writersMap.put(new SynchronizedWriter(fileName, append, charsetName, bufferSize, id), minDebugLevel);
        this.writersMap = Generic.sortByValue(this.writersMap, true);
    }

    public synchronized void addWriter(String fileName, boolean append, String id, int minDebugLevel)
            throws java.io.FileNotFoundException {
        this.writersMap.put(new SynchronizedWriter(fileName, append, id), minDebugLevel);
        this.writersMap = Generic.sortByValue(this.writersMap, true);
    }

    public synchronized void addWriter(String fileName, boolean append, int minDebugLevel)
            throws java.io.FileNotFoundException {
        this.writersMap.put(new SynchronizedWriter(fileName, append), minDebugLevel);
        this.writersMap = Generic.sortByValue(this.writersMap, true);
    }

    public synchronized void checkDiskSpace() {
        try {
            if (this.debugLevel >= 1 && (new File(".").getUsableSpace() < (long) 50 * 1024 * 1024 || !this.enoughAvailableSpace((long) 50 * 1024 * 1024))) {
                logger.info("Less than 50Mb remain on disk. Reducing debugLevel. (space = {})", new File(".").getUsableSpace());

                this.flush(1, this.debugLevel);
                this.debugLevel = 0;
            } else if (this.debugLevel >= 2 && !this.enoughAvailableSpace((long) 200 * 1024 * 1024)) {
                logger.info("Less than 200Mb remain on disk. Reducing debugLevel.");

                this.flush(2, this.debugLevel);
                this.debugLevel = 1;
            } else if (this.debugLevel >= 3 && !this.enoughAvailableSpace((long) 500 * 1024 * 1024)) {
                logger.info("Less than 500Mb remain on disk. Reducing debugLevel.");

                this.flush(3, this.debugLevel);
                this.debugLevel = 2;
            } else { // debugLevel doesn't need to be reduced
            }

            timeLastCheckDiskSpaceStamp();
        } catch (IOException iOException) {
            logger.error("STRANGE IOException inside Debugger.checkDiskSpace(), timeStamp={}", System.currentTimeMillis(), iOException);
        }
    }

    public synchronized boolean enoughAvailableSpace(long neededSpace) {
        boolean enoughSpace = true;

        for (SynchronizedWriter writer : this.writersMap.keySet()) {
            if (this.writersMap.get(writer) <= this.debugLevel) {
                if (writer.getUsableSpace() < neededSpace) {
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

    public synchronized boolean write(String writeString, String writerId, int minDebugLevel) {
        if (this.debugLevel >= minDebugLevel) {
            return write(writeString, writerId);
        } else {
            return false;
        }
    }

    public synchronized boolean write(String writeString, String writerId) {
        boolean success = false;
        for (SynchronizedWriter writer : this.writersMap.keySet()) {
            if (this.writersMap.get(writer) <= this.debugLevel) {
                String currentWriterId = writer.getId();

                if (currentWriterId == null ? writerId == null : currentWriterId.equals(writerId)) {
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

    public synchronized void flush(int minDebugLevel, int maxDebugLevel)
            throws java.io.IOException {
        for (SynchronizedWriter writer : this.writersMap.keySet()) {
            int writerDebugLevel = this.writersMap.get(writer);

            if (writerDebugLevel >= minDebugLevel) {
                if (writerDebugLevel <= maxDebugLevel) {
                    writer.flush();
                } else {
                    break; // the following values are all greater than maxDebugLevel
                }
            }
        }
    }

    public synchronized void flush()
            throws java.io.IOException {
        for (SynchronizedWriter writer : this.writersMap.keySet()) {
            writer.flush();
        }
    }

    public synchronized void close() {
        for (SynchronizedWriter writer : this.writersMap.keySet()) {
            writer.close();
        }
    }
}
