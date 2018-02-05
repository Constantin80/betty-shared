package info.fmro.shared.utility;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IgnorableDatabase
      ---  implements Serializable {
//fixme dsalkdjaskld
//todo faklfjalfkaj
//todo fdaslfjkdklfjklfjalkfjasldkfjaslfkajsdfklajsf
    private static final Logger logger = LoggerFactory.getLogger(IgnorableDatabase.class);
    private static final long serialVersionUID = 1721089746563166850L;
    private final HashSet<Ignorable> idsResetAndAwaitingProcessing = new HashSet<>(2);
    private final HashMap<Ignorable, Long> idsStillIgnored = new HashMap<>(2);

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

    // best -> don't write or read, just generate (remove Serializable to prevent accidental read/write)
    
    public synchronized void syncIgnorableDatabase() {
        // used in readObjectsFromFiles, right after all objects have been read
        // it should also synchronize on all used maps, although this method is to be used before threads are started

        --replace with generate() and add generated int, so it gives error at 2nd+ generation

        if (password.equals("is used right after all objects have been read")){
        
        final Iterator<Ignorable> iterator = this.idsResetAndAwaitingProcessing.iterator();
        while (iterator.hasNext()) {
            final Ignorable existingElement = iterator.next();
            final Class<? extends Ignorable> clazz = existingElement.getClass();


// public static <T extends Ignorable> SynchronizedMap<?, T> getIgnorableMap(Class<T> clazz) 


} // end while

        final Iterator<Ignorable> iterator = this.idsStillIgnored.keySet().iterator();
        -
                
                
                }else
        {
            //bad password provided, function probably used in the wrong place, will print error and not run in
            logger.error("bad password for syncIgnorableDatabase: {}", password);
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
            