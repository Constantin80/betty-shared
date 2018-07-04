package info.fmro.shared.utility;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynchronizedMap<K, V>
        implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizedMap.class);
    public static final int DEFAULT_INITIAL_SIZE = 0;
    public static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final long serialVersionUID = -7460171730311386991L;
    private final HashMap<K, V> map;
    private transient Set<Entry<K, V>> mapEntries;
    private transient Set<K> mapKeys;
    private transient Collection<V> mapValues;
    private long timeStamp, timeStampRemoved, timeClean; // timeStamp for general use; timeStampRemoved auto updated at element removal

    public SynchronizedMap() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    public SynchronizedMap(int initialSize) {
        this(initialSize, DEFAULT_LOAD_FACTOR);
    }

    public SynchronizedMap(int initialSize, float loadFactor) {
        this.map = new HashMap<>(initialSize, loadFactor);
        this.mapEntries = this.map.entrySet();
        this.mapKeys = this.map.keySet();
        this.mapValues = this.map.values();
    }

    public SynchronizedMap(Map<? extends K, ? extends V> map) {
        this.map = new HashMap<>(map);
        this.mapEntries = this.map.entrySet();
        this.mapKeys = this.map.keySet();
        this.mapValues = this.map.values();
    }

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public synchronized void copyFrom(SynchronizedMap<K, V> other) { // doesn't copy static final or transient; does update the map
        if (!this.map.isEmpty()) {
            logger.error("not empty map in SynchronizedMap copyFrom: {}", Generic.objectToString(this));
        }
        this.timeStamp = other.timeStamp;
        this.timeStampRemoved = other.timeStampRemoved;
        this.timeClean = other.timeClean;
        this.map.clear();
        this.map.putAll(other.map);
    }

    private void readObject(ObjectInputStream objectInputStream)
            throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();

        this.mapEntries = this.map.entrySet();
        this.mapKeys = this.map.keySet();
        this.mapValues = this.map.values();
    }

    public synchronized HashMap<K, V> copy() {
        return new HashMap<>(map);
    }

    public synchronized Set<Entry<K, V>> entrySetCopy() {
        return new HashSet<>(mapEntries);
    }

    public synchronized Set<K> keySetCopy() {
        return new HashSet<>(mapKeys);
    }

    public synchronized Collection<V> valuesCopy() {
        return new ArrayList<>(mapValues);
    }

    public synchronized void clear() {
        this.map.clear();
    }

    public synchronized boolean containsKey(K key) {
        return this.map.containsKey(key);
    }

    public synchronized boolean containsValue(V value) {
        return this.map.containsValue(value);
    }

    public synchronized V get(K key) {
        return this.map.get(key);
    }

    public synchronized boolean isEmpty() {
        return this.map.isEmpty();
    }

    public synchronized V put(K key, V value, boolean intentionalPutInsteadOfPutIfAbsent) {
        if (!intentionalPutInsteadOfPutIfAbsent) {
            final Class<?> valueClass = value == null ? null : value.getClass();
            if (!Long.class.equals(valueClass)) {
                final Class<?> keyClass = key == null ? null : key.getClass();
                logger.error("Synchronized map put used, but putIfAbsent is advisable, as it gives more control: {} {} {} {}", keyClass, valueClass, Generic.objectToString(key),
                        Generic.objectToString(value));
            } else { // method allowed for this class
            }
        }
        return this.map.put(key, value);
    }

    public synchronized V put(K key, V value) {
        return put(key, value, false);
    }

    public synchronized V putIfAbsent(K key, V value) {
        if (!map.containsKey(key)) {
            return this.map.put(key, value);
        } else {
            return this.map.get(key);
        }
    }

    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        this.map.putAll(m);
    }

    public synchronized V remove(K key) {
        V existingValue = this.map.remove(key);
        if (existingValue != null) { // the case of null elements is not checked on, for speed
            timeStampRemoved();
        }
        return existingValue;
    }

    public synchronized boolean remove(K key, V value) {
        boolean modified = this.map.remove(key, value);
        if (modified) { // the case of null elements is not checked on, for speed
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized int size() {
        return this.map.size();
    }

    public synchronized boolean containsEntry(Entry<K, V> entry) {
        return this.mapEntries.contains(entry);
    }

    public synchronized boolean containsAllEntries(Collection<?> c) {
        return this.mapEntries.containsAll(c);
    }

    public synchronized boolean removeEntry(Entry<K, V> entry) {
        boolean modified = this.mapEntries.remove(entry);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean removeAllEntries(Collection<?> c) {
        boolean modified = this.mapEntries.removeAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean retainAllEntries(Collection<?> c) {
        boolean modified = this.mapEntries.retainAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean containsAllKeys(Collection<?> c) {
        return this.mapKeys.containsAll(c);
    }

    public synchronized boolean removeAllKeys(Collection<?> c) {
        boolean modified = this.mapKeys.removeAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean retainAllKeys(Collection<?> c) {
        boolean modified = this.mapKeys.retainAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean containsAllValues(Collection<?> c) {
        return this.mapValues.containsAll(c);
    }

    public synchronized boolean removeValue(V value) {
        boolean modified = this.mapValues.remove(value);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean removeValueAll(V value) {
        boolean modified = this.mapValues.remove(value);
        if (modified) {
            while (this.mapValues.remove(value)) { // intentionally empty
            }
            timeStampRemoved();
        }
        return modified;
    }

    public synchronized boolean removeAllValues(Collection<?> c) {
        boolean modified = this.mapValues.removeAll(c);
        if (modified) {
            timeStampRemoved();
        }
        if (c == null) {
            logger.error("Collection null in SynchronizedMap removeAllValues; removeValueAll might be required");
        }

        return modified;
    }

    public synchronized boolean retainAllValues(Collection<?> c) {
        boolean modified = this.mapValues.retainAll(c);
        if (modified) {
            timeStampRemoved();
        }
        return modified;
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
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.map);
        return hash;
    }

    @Override
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public synchronized boolean equals(Object obj) { // other.map not synchronized, meaning equals result not guaranteed to be correct, but behaviour is acceptable
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SynchronizedMap<?, ?> other = (SynchronizedMap<?, ?>) obj;
        return Objects.equals(this.map, other.map);
    }
}
