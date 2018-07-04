package info.fmro.shared.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;

public class SynchronizedSet<E>
//        extends Ignorable
        implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedSet.class);
    public static final int DEFAULT_INITIAL_SIZE = 0;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final long serialVersionUID = -4718857828033491585L;
    private final HashSet<E> set;
    private long timeStamp, timeStampRemoved, timeClean; // timeStamp for general use; timeStampRemoved auto updated at element removal

    public SynchronizedSet() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    public SynchronizedSet(int initialSize) {
        this(initialSize, DEFAULT_LOAD_FACTOR);
    }

    public SynchronizedSet(int initialSize, float loadFactor) {
        this.set = new HashSet<>(initialSize, loadFactor);
    }

    public SynchronizedSet(Collection<? extends E> collection) {
        this.set = new HashSet<>(collection);
    }

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public synchronized void copyFrom(SynchronizedSet<E> other) { // doesn't copy static final or transient; does update the set
        if (!this.set.isEmpty()) {
            logger.error("not empty set in SynchronizedSet copyFrom: {}", Generic.objectToString(this));
        }
        this.timeStamp = other.timeStamp;
        this.timeStampRemoved = other.timeStampRemoved;
        this.timeClean = other.timeClean;
        this.set.clear();
        this.set.addAll(other.set);
    }

    public synchronized HashSet<E> copy() {
        return new HashSet<>(set);
    }

    public synchronized boolean add(E element) {
        return this.set.add(element);
    }

    public synchronized boolean addAll(Collection<? extends E> c) {
        return this.set.addAll(c);
    }

    public synchronized void clear() {
        this.set.clear();
    }

    public synchronized E getEqualElement(E elementToFind) {
        E returnValue = null;
        if (this.set == null) {
            logger.error("null set in getEqualElement for: {} {}", elementToFind, Generic.objectToString(this));
            returnValue = null;
        } else {
            final Iterator<E> iterator = this.set.iterator();
            while (iterator.hasNext()) {
                final E existingElement = iterator.next();
                if (Objects.equals(elementToFind, existingElement)) {
                    returnValue = existingElement;
                    break;
                } else { // not equal, nothing to be done, while will continue
                }
            } // end while
        }

        return returnValue;
    }

    public synchronized boolean contains(Object object) {
        //noinspection SuspiciousMethodCalls
        return this.set.contains(object);
    }

    public synchronized boolean containsAll(Collection<?> c) {
        return this.set.containsAll(c);
    }

    public synchronized boolean isEmpty() {
        return this.set.isEmpty();
    }

    public synchronized boolean remove(Object object) {
        //noinspection SuspiciousMethodCalls
        return this.set.remove(object);
    }

    public synchronized boolean removeAll(Collection<?> c) {
        return this.set.removeAll(c);
    }

    public synchronized boolean retainAll(Collection<?> c) {
        return this.set.retainAll(c);
    }

    public synchronized int size() {
        return this.set.size();
    }

    public synchronized long getTimeStamp() {
        return timeStamp;
    }

    public synchronized void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public synchronized void timeStamp() {
        this.timeStamp = System.currentTimeMillis();
    }

    public synchronized long getTimeStampRemoved() {
        return timeStampRemoved;
    }

    public synchronized void setTimeStampRemoved(long timeStampRemoved) {
        this.timeStampRemoved = timeStampRemoved;
    }

    public synchronized void timeStampRemoved() {
        this.timeStampRemoved = System.currentTimeMillis();
    }

    public synchronized long getTimeClean() {
        return timeClean;
    }

    public synchronized void setTimeClean(long timeClean) {
        this.timeClean = timeClean;
    }

    public synchronized void timeCleanStamp() {
        this.timeClean = System.currentTimeMillis();
    }

    public synchronized void timeCleanStamp(long timeStamp) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - this.timeClean >= timeStamp) {
            this.timeClean = currentTime + timeStamp;
        } else {
            this.timeClean += timeStamp;
        }
    }

    @Override
    public synchronized int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.set);
        return hash;
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public synchronized boolean equals(Object obj) { // other.set not synchronized, meaning equals result not guaranteed to be correct, but behaviour is acceptable
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SynchronizedSet<?> other = (SynchronizedSet<?>) obj;
        return Objects.equals(this.set, other.set);
    }
}
