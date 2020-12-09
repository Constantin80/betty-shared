package info.fmro.shared.utility;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings({"ClassWithTooManyMethods", "BooleanMethodIsAlwaysInverted", "WeakerAccess", "RedundantSuppression"})
public class SynchronizedMap<K, V>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(SynchronizedMap.class);
    public static final int DEFAULT_INITIAL_SIZE = 0;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    @Serial
    private static final long serialVersionUID = -7460171730311386991L;
    private final HashMap<K, V> map;
    private transient Set<Entry<K, V>> mapEntries;
    private transient Set<K> mapKeys;
    private transient Collection<V> mapValues;
    private long timeStamp, timeStampRemoved, timeClean; // timeStamp for general use; timeStampRemoved auto updated at element removal

    public SynchronizedMap() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    public SynchronizedMap(final int initialSize) {
        this(initialSize, DEFAULT_LOAD_FACTOR);
    }

    protected SynchronizedMap(final int initialSize, final float loadFactor) {
        this.map = new HashMap<>(initialSize, loadFactor);
        this.mapEntries = this.map.entrySet();
        this.mapKeys = this.map.keySet();
        this.mapValues = this.map.values();
    }

    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
    public SynchronizedMap(final Map<? extends K, ? extends V> map) {
        this.map = new HashMap<>(map);
        this.mapEntries = this.map.entrySet();
        this.mapKeys = this.map.keySet();
        this.mapValues = this.map.values();
    }

    public synchronized boolean copyFrom(final SynchronizedMap<? extends K, ? extends V> other) {
        return copyFrom(other, false);
    }

    public synchronized boolean copyFrom(final SynchronizedMap<? extends K, ? extends V> other, final boolean mapAllowedToBeNotEmpty) { // doesn't copy static final or transient; does update the map
        final boolean readSuccessful;
        if (!this.map.isEmpty() && !mapAllowedToBeNotEmpty) {
            logger.error("not empty map in SynchronizedMap copyFrom: {}", this.map.keySet()); // this error might not be fatal, read can still be successful
        }
        if (other == null) {
            logger.error("null other in copyFrom for: {}", this.map.keySet());
            readSuccessful = false;
        } else if (other.map == null) {
            logger.error("null other.map in copyFrom for: {}", this.map.keySet());
            readSuccessful = false;
        } else {
//            Generic.updateObject(this, other);
            this.timeStamp = other.timeStamp;
            this.timeStampRemoved = other.timeStampRemoved;
            this.timeClean = other.timeClean;
            this.map.clear();
            this.map.putAll(other.map);
            // constructor or readObject has been called when the object was created, so the transient collections are already instantiated

            readSuccessful = true;
        }
        return readSuccessful;
    }

    @Serial
    private void readObject(@NotNull final ObjectInputStream objectInputStream)
            throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();

        this.mapEntries = this.map.entrySet();
        this.mapKeys = this.map.keySet();
        this.mapValues = this.map.values();
    }

    public synchronized HashMap<K, V> copy() {
        return new HashMap<>(this.map);
    }

    public synchronized Set<Entry<K, V>> entrySetCopy() {
        return new HashSet<>(this.mapEntries);
    }

    public synchronized Set<K> keySetCopy() {
        return new HashSet<>(this.mapKeys);
    }

    public synchronized Collection<V> valuesCopy() {
        return new ArrayList<>(this.mapValues);
    }

    public synchronized HashMap<K, V> clear() {
        final HashMap<K, V> copy = new HashMap<>(this.map);
        this.map.clear();
        return copy;
    }

    public synchronized boolean containsKey(final K key) {
        return this.map.containsKey(key);
    }

    public synchronized boolean containsValue(final V value) {
        return this.map.containsValue(value);
    }

    public synchronized V get(final K key) {
        return this.map.get(key);
    }

    public synchronized boolean isEmpty() {
        return this.map.isEmpty();
    }

    public synchronized V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) {
        return this.map.computeIfAbsent(key, mappingFunction);
    }

    public synchronized V put(final K key, final V value, final boolean intentionalPutInsteadOfPutIfAbsent) {
        // intentionalPutInsteadOfPutIfAbsent should be true only when I check that there's no previous value or I have no need for the previous value, like in the case of a primitive
        if (!intentionalPutInsteadOfPutIfAbsent) {
            final Class<?> valueClass = value == null ? null : value.getClass();
            if (Long.class.equals(valueClass)) { // method allowed for this class
            } else {
                final Class<?> keyClass = key == null ? null : key.getClass();
                logger.error("Synchronized map put used, but putIfAbsent is advisable, as it gives more control: {} {} {} {}", keyClass, valueClass, Generic.objectToString(key), Generic.objectToString(value));
            }
        }
        return this.map.put(key, value);
    }

    public synchronized V put(final K key, final V value) {
        return put(key, value, false);
    }

    public synchronized V putIfAbsent(final K key, final V value) {
        return this.map.containsKey(key) ? this.map.get(key) : this.map.put(key, value);
    }

    public synchronized void putAll(final Map<? extends K, ? extends V> m) {
        this.map.putAll(m);
    }

    // copyFrom is used instead
//    public synchronized void putAll(SynchronizedMap<? extends K, ? extends V> m) {
//        this.map.putAll(m.map);
//    }

    public synchronized V remove(final K key) {
        final V existingValue = this.map.remove(key);
        if (existingValue != null) { // the case of null elements is not checked on, for speed
            timeStampRemoved();
        }
        return existingValue;
    }

    public synchronized boolean remove(final K key, final V value) {
        final boolean modified = this.map.remove(key, value);
        if (modified) { // the case of null elements is not checked on, for speed
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized int size() {
        return this.map.size();
    }

    public synchronized boolean containsEntry(final Entry<K, V> entry) {
        return this.mapEntries.contains(entry);
    }

    public synchronized boolean containsAllEntries(final Collection<?> c) {
        return this.mapEntries.containsAll(c);
    }

    public synchronized boolean removeEntry(final Entry<K, V> entry) {
        final boolean modified = this.mapEntries.remove(entry);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean removeAllEntries(final Collection<?> c) {
        final boolean modified = this.mapEntries.removeAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean retainAllEntries(final Collection<?> c) {
        final boolean modified = this.mapEntries.retainAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean containsAllKeys(final Collection<?> c) {
        return this.mapKeys.containsAll(c);
    }

    public synchronized boolean removeAllKeys(final Collection<?> c) {
        final boolean modified = this.mapKeys.removeAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean retainAllKeys(final Collection<?> c) {
        final boolean modified = this.mapKeys.retainAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean containsAllValues(final Collection<?> c) {
        return this.mapValues.containsAll(c);
    }

    public synchronized boolean removeValue(final V value) {
        final boolean modified = this.mapValues.remove(value);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean removeValueAll(final V value) {
        final boolean modified = this.mapValues.remove(value);
        if (modified) {
            while (this.mapValues.remove(value)) { // intentionally empty
            }
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean removeAllValues(final Collection<?> c) {
        final boolean modified;
        if (c == null) {
            logger.error("Collection null in SynchronizedMap removeAllValues; removeValueAll might be required");
            modified = false;
        } else {
            modified = this.mapValues.removeAll(c);
            if (modified) {
                timeStampRemoved();
            }
        }

        return modified;
    }

    public synchronized boolean retainAllValues(final Collection<?> c) {
        final boolean modified = this.mapValues.retainAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
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

    private synchronized void timeStampRemoved() {
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

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SynchronizedMap<?, ?> that = (SynchronizedMap<?, ?>) obj;
        return Objects.equals(this.map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.map);
    }
}
