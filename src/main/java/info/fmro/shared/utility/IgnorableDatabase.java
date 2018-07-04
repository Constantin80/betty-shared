/*
package info.fmro.shared.utility;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IgnorableDatabase {
    // not read or written, just generated
    private static final Logger logger = LoggerFactory.getLogger(IgnorableDatabase.class);
    private static final long serialVersionUID = 1721089746563166850L;
    private final HashSet<Ignorable> idsResetAndAwaitingProcessing = new HashSet<>(2);
    private final HashMap<Ignorable, Long> idsStillIgnored = new HashMap<>(2);
    private int isGenerated;

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public synchronized void copyFrom(IgnorableDatabase other) { // doesn't copy static final or transient
        if (!this.idsResetAndAwaitingProcessing.isEmpty() || !this.idsStillIgnored.isEmpty()) {
            logger.error("not empty map or set in IgnorableDatabase copyFrom: {}", Generic.objectToString(this));
        }
        this.idsResetAndAwaitingProcessing.clear();
        this.idsResetAndAwaitingProcessing.addAll(other.idsResetAndAwaitingProcessing);
        this.idsStillIgnored.clear();
        this.idsStillIgnored.putAll(other.idsStillIgnored);
    }

    public void syncIgnorableDatabase(AtomicBoolean programIsRunningMultiThreaded) {
        // it should synchronize on all used maps, although this method is to be used before threads are started


//        logger.info("getting locks on all maps for updating IgnorableDatabase");
// beginning of sync blocks; this looks terrible and is a safety feature that's currently unused, as threads are not started when this method is used
//        synchronized (Statics.betradarEventsMap) {
//            synchronized (Statics.coralEventsMap) {
//                synchronized (Statics.eventsMap) {
//                    synchronized (Statics.marketCaraloguesMap) {
//                        synchronized (Statics.safeMarketsMap) {

        if (!programIsRunningMultiThreaded.get()) {
            final Iterator<Ignorable> iterator = this.idsResetAndAwaitingProcessing.iterator();
            final HashSet<Ignorable> tempHashSet = new HashSet<>(2);
            while (iterator.hasNext()) {
                final Ignorable existingElement = iterator.next();
                final Class<? extends Ignorable> clazz = existingElement.getClass();
                final SynchronizedMap<T, ? extends Ignorable> synchronizedMap = (SynchronizedMap<T, ? extends Ignorable>) Formulas.getIgnorableMap(clazz);

// public static <T extends Ignorable> SynchronizedMap<?, T> getIgnorableMap(Class<T> clazz) 


            } // end while

            final Iterator<Ignorable> secondIterator = this.idsStillIgnored.keySet().iterator();
            while (secondIterator.hasNext()) {
                final Ignorable existingElement = secondIterator.next();
                final Class<? extends Ignorable> clazz = existingElement.getClass();


// public static <T extends Ignorable> SynchronizedMap<?, T> getIgnorableMap(Class<T> clazz)


            } // end while


//                        }
//                    }
//                }
//            }
//        }
// end of sync blocks
//        logger.info("locks on all maps released after updating IgnorableDatabase");

            isGenerated++;
            if (isGenerated == 1) {
                logger.info("database generated");
            } else {
                logger.error("database generated more than once: {}", isGenerated);
            }
        } else {
            logger.error("trying to syncIgnorableDatabase while the programIsRunningMultiThreaded");
        }
    }

    public void IsGenerated() {
        if (isGenerated != 1) {
            logger.error("testIsgenerated failed: {}", isGenerated);
        }
    }


    public synchronized // problem, Ignorable doesn't implement hash/equals/compareTo; with save/reload from disk, this can be an issue
// adding a method for sync of objects in database and those in the regular maps would partially solve the issue
// sync immediately after copy of objects from disk (could be added to readObjects method, after reading all objects)
// error message if ignorableDatabase object not found in the regular maps, and object removed from database
// still a problem if reset and addIgnore are applied to 2 normally equal objects, 1 from map and another one that has just been created
// support can be added for removal of doubles of objects, if needed, in the thread
// more than this support, in the updateIgnorable, only the object being updated will be left in the ignorableDatabase, as the other object normally gets discarded
// resetTempRemoved support
*/