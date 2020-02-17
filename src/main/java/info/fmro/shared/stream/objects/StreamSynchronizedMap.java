package info.fmro.shared.stream.objects;

import info.fmro.shared.enums.SynchronizedMapModificationCommand;
import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("NonPrivateFieldAccessedInSynchronizedContext")
public class StreamSynchronizedMap<K extends Serializable, V extends Serializable>
        extends SynchronizedMap<K, V>
        implements StreamObjectInterface, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(StreamSynchronizedMap.class);
    private static final long serialVersionUID = -4577933961359844025L;
    public transient ListOfQueues listOfQueues = new ListOfQueues();
    private final Class<? super V> clazz;

    public StreamSynchronizedMap(final Class<? super V> clazz) {
        super();
        this.clazz = clazz;
    }

    public StreamSynchronizedMap(final Class<? super V> clazz, final int initialSize) {
        super(initialSize);
        this.clazz = clazz;
    }

    public StreamSynchronizedMap(final Class<? super V> clazz, final Map<? extends K, ? extends V> map) {
        super(map);
        this.clazz = clazz;
    }

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.listOfQueues = new ListOfQueues();
    }

    public synchronized boolean copyFrom(final StreamSynchronizedMap<? extends K, ? extends V> other) { // doesn't copy static final or transient; does update the map
        final boolean readSuccessful = super.copyFrom(other);

        final int nQueues = this.listOfQueues.size();
        if (nQueues == 0) { // normal case, nothing to be done
        } else {
            logger.error("existing queues during StreamSynchronizedMap.copyFrom: {} {}", nQueues, Generic.objectToString(this));
            this.listOfQueues.send(this.getCopy());
        }

        return readSuccessful;
    }

    public synchronized boolean copyFromStream(final StreamSynchronizedMap<? extends K, ? extends V> other) { // doesn't copy static final or transient; does update the map
        this.clear(); // to avoid error message if map not empty, because in this case it can actually have elements
        return this.copyFrom(other);

//        final int nQueues = this.listOfQueues.size();
//        if (nQueues == 0) { // normal case, nothing to be done
//        } else {
//            logger.error("existing queues during StreamSynchronizedMap.copyFromStream: {} {}", nQueues, Generic.objectToString(this));
//            this.listOfQueues.clear();
//        }
    }

    public synchronized StreamSynchronizedMap<K, V> getCopy() {
        return SerializationUtils.clone(this);
    }

    public synchronized Class<? super V> getClazz() {
        return this.clazz;
    }

    @Override
    public synchronized HashMap<K, V> clear() {
        final HashMap<K, V> result = super.clear();
        if (result.isEmpty()) { // no modification made, I won't send anything
        } else {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.clear, this.clazz));
        }
        return result;
    }

    @Override
    public synchronized V put(final K key, @NotNull final V value, final boolean intentionalPutInsteadOfPutIfAbsent) {
        // intentionalPutInsteadOfPutIfAbsent should be true only when I check that there's no previous value or I have no need for the previous value, like in the case of a primitive
        final V result = super.put(key, value, intentionalPutInsteadOfPutIfAbsent);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.put, this.clazz, key, value, intentionalPutInsteadOfPutIfAbsent));
        }
        return result;
    }

    @Override
    public synchronized V put(final K key, @NotNull final V value) {
        final V result = super.put(key, value);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.put, this.clazz, key, value));
        }
        return result;
    }

    @Override
    public synchronized V putIfAbsent(final K key, @NotNull final V value) {
        final V result = super.putIfAbsent(key, value);
        if (Objects.equals(value, result)) { // no modification made, I won't send anything
        } else {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.putIfAbsent, this.clazz, key, value));
        }
        return result;
    }

    @Override
    public synchronized void putAll(final Map<? extends K, ? extends V> m) {
        super.putAll(m);
        this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.putAll, this.clazz, new HashMap<>(m)));
    }

    @Override
    public synchronized V remove(final K key) {
        if (containsKey(key)) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.remove, this.clazz, key));
        } else { // no key removed, nothing to be done
        }
        return super.remove(key);
    }

    @Override
    public synchronized boolean remove(final K key, final V value) {
        final boolean result = super.remove(key, value);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.remove, this.clazz, key, value));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean removeEntry(final Map.Entry<K, V> entry) {
        final boolean result = super.removeEntry(entry);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.removeEntry, this.clazz, new AbstractMap.SimpleEntry<>(entry)));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean removeAllEntries(final Collection<?> c) {
        final boolean result = super.removeAllEntries(c);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.removeAllEntries, this.clazz, new HashSet<>(c)));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean retainAllEntries(final Collection<?> c) {
        final boolean result = super.retainAllEntries(c);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.retainAllEntries, this.clazz, new HashSet<>(c)));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean removeAllKeys(final Collection<?> c) {
        final boolean result = super.removeAllKeys(c);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.removeAllKeys, this.clazz, new HashSet<>(c)));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean retainAllKeys(final Collection<?> c) {
        final boolean result = super.retainAllKeys(c);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.retainAllKeys, this.clazz, new HashSet<>(c)));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean removeValue(final V value) {
        final boolean result = super.removeValue(value);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.removeValue, this.clazz, value));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean removeValueAll(final V value) {
        final boolean result = super.removeValueAll(value);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.removeValueAll, this.clazz, value));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean removeAllValues(final Collection<?> c) {
        final boolean result = super.removeAllValues(c);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.removeAllValues, this.clazz, new HashSet<>(c)));
        } else { // no modification made, I won't send anything
        }
        return result;
    }

    @Override
    public synchronized boolean retainAllValues(final Collection<?> c) {
        final boolean result = super.retainAllValues(c);
        if (result) {
            this.listOfQueues.send(new SerializableObjectModification<>(SynchronizedMapModificationCommand.retainAllValues, this.clazz, new HashSet<>(c)));
        } else { // no modification made, I won't send anything
        }
        return result;
    }
}
