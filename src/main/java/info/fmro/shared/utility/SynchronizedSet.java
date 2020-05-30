package info.fmro.shared.utility;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
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

    @Contract(pure = true)
    public SynchronizedSet() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    @Contract(pure = true)
    public SynchronizedSet(final int initialSize) {
        this(initialSize, DEFAULT_LOAD_FACTOR);
    }

    @Contract(pure = true)
    public SynchronizedSet(final int initialSize, final float loadFactor) {
        this.set = new HashSet<>(initialSize, loadFactor);
    }

    public SynchronizedSet(final Collection<? extends E> collection) {
        this.set = new HashSet<>(collection);
    }

    public synchronized boolean copyFrom(final SynchronizedSet<? extends E> other) { // doesn't copy static final or transient; does update the set
        final boolean readSuccessful;
        if (!this.set.isEmpty()) {
            logger.error("not empty set in SynchronizedSet copyFrom: {}", Generic.objectToString(this)); // this error might not be fatal, read can still be successful
        }
        if (other == null) {
            logger.error("null other in copyFrom for: {}", Generic.objectToString(this));
            readSuccessful = false;
        } else if (other.set == null) {
            logger.error("null other.set in copyFrom for: {}", Generic.objectToString(other));
            readSuccessful = false;
        } else {
//            Generic.updateObject(this, other);

            this.timeStamp = other.timeStamp;
            this.timeStampRemoved = other.timeStampRemoved;
            this.timeClean = other.timeClean;
            this.set.clear();
            this.set.addAll(other.set);

            readSuccessful = true;
        }

        return readSuccessful;
    }

    public synchronized HashSet<E> copy() {
        return new HashSet<>(this.set);
    }

    public synchronized HashSet<E> copyAndClear() {
        final HashSet<E> returnValue = new HashSet<>(this.set);
        clear();
        return returnValue;
    }

    public synchronized boolean add(final E element) {
        return this.set.add(element);
    }

    public synchronized boolean addAll(final Collection<? extends E> c) {
        return this.set.addAll(c);
    }

    public synchronized HashSet<E> clear() {
        final HashSet<E> copy = new HashSet<>(this.set);
        this.set.clear();
        return copy;
    }

    public synchronized E getEqualElement(final E elementToFind) {
        @Nullable E returnValue = null;
        if (this.set == null) {
            logger.error("null set in getEqualElement for: {} {}", elementToFind, Generic.objectToString(this));
            returnValue = null;
        } else {
            for (final E existingElement : this.set) {
                if (Objects.equals(elementToFind, existingElement)) {
                    returnValue = existingElement;
                    break;
                } else { // not equal, nothing to be done, while will continue
                }
            } // end while
        }

        return returnValue;
    }

    public synchronized boolean contains(final Object object) {
        return this.set.contains(object);
    }

    public synchronized boolean containsAll(final Collection<?> c) {
        return this.set.containsAll(c);
    }

    public synchronized boolean isEmpty() {
        return this.set.isEmpty();
    }

    public synchronized boolean remove(final Object object) {
        return this.set.remove(object);
    }

    public synchronized boolean removeAll(final Collection<?> c) {
        return this.set.removeAll(c);
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public synchronized boolean retainAll(final Collection<?> c) {
        return this.set.retainAll(c);
    }

    public synchronized int size() {
        return this.set.size();
    }

    public synchronized long getTimeStamp() {
        return this.timeStamp;
    }

    public synchronized void setTimeStamp(final long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public synchronized void timeStamp() {
        this.timeStamp = System.currentTimeMillis();
    }

    public synchronized long getTimeStampRemoved() {
        return this.timeStampRemoved;
    }

    public synchronized void setTimeStampRemoved(final long timeStampRemoved) {
        this.timeStampRemoved = timeStampRemoved;
    }

    public synchronized void timeStampRemoved() {
        this.timeStampRemoved = System.currentTimeMillis();
    }

    public synchronized long getTimeClean() {
        return this.timeClean;
    }

    public synchronized void setTimeClean(final long timeClean) {
        this.timeClean = timeClean;
    }

    public synchronized void timeCleanStamp() {
        this.timeClean = System.currentTimeMillis();
    }

    public synchronized void timeCleanStamp(final long timeToAdd) {
        final long currentTime = System.currentTimeMillis();
        if (currentTime - this.timeClean >= timeToAdd) {
            this.timeClean = currentTime + timeToAdd;
        } else {
            this.timeClean += timeToAdd;
        }
    }

    @Override
    public synchronized int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.set);
        return hash;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public synchronized boolean equals(final Object obj) { // other.set not synchronized, meaning equals result not guaranteed to be correct, but behaviour is acceptable
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
