package info.fmro.shared.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

public class SynchronizedSafeSet<E extends SafeObjectInterface>
        extends SynchronizedSet<E>
        implements Serializable {  // it already implements Serializable from super
    private static final Logger logger = LoggerFactory.getLogger(SynchronizedSafeSet.class);
    private static final long serialVersionUID = -9093408222795168259L;

    public SynchronizedSafeSet() {
        super();
    }

    public SynchronizedSafeSet(int initialSize) {
        super(initialSize);
    }

    public SynchronizedSafeSet(int initialSize, float loadFactor) {
        super(initialSize, loadFactor);
    }

    public SynchronizedSafeSet(Collection<? extends E> collection) {
        super(collection);
    }

    // copyFrom seems just fine and doesn't need to be overridden

    @Override
    public synchronized boolean add(E element) {
        final boolean isAdded = super.add(element);

        if (isAdded) {
            if (element == null) {
                logger.error("have added null element in SynchronizedSafeSet: {}", Generic.objectToString(this));
            } else {
                element.runOnAdd();
            }
        } else { // not added, nothing to be done
        }

        return isAdded;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        boolean elementWasAdded = false;
        for (E element : c) {
            if (!elementWasAdded && this.add(element)) {
                elementWasAdded = true;
            } else { // not added, or elementWasAdded already true; nothing to do
            }
        }

        return elementWasAdded;
    }

    @Override
    public synchronized void clear() {
        final HashSet<E> copy = super.copy();
        for (E element : copy) {
            if (element != null) {
                element.runOnRemoval();
            } else { // null elements are allowed, nothing to be done
            }
        }
        super.clear();
    }

    @Override
    public synchronized boolean remove(Object object) {
        E existingElement;
        try {
            //noinspection unchecked
            existingElement = this.getEqualElement((E) object);
        } catch (ClassCastException classCastException) {
            logger.error("existingElement not the right class in SynchronizedSafeSet.remove: {} {} {}", object == null ? null : object.getClass(), Generic.objectToString(object), Generic.objectToString(this));
            existingElement = null;
        }

        final boolean isRemoved = super.remove(object);

        if (isRemoved) {
            if (existingElement != null) {
                existingElement.runOnRemoval();
            } else {
                if (object == null) { // normal behavior, nothing to be done
                } else {
                    logger.error("existingElement null and removed object not null in SynchronizedSafeSet.remove: {}", Generic.objectToString(object));
                }
            }
        } else { // nothing removed, nothing to be done
        }

        return isRemoved;
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        boolean elementWasRemoved = false;
        for (Object object : c) {
            if (!elementWasRemoved && this.remove(object)) {
                elementWasRemoved = true;
            } else { // not added, or elementWasRemoved already true; nothing to do
            }
        }
        return elementWasRemoved;
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        final HashSet<E> copy = super.copy();
        for (E element : copy) {
            if (element != null && !c.contains(element)) {
                element.runOnRemoval();
            } else { // null elements are allowed, nothing to be done
            }
        }
        return super.retainAll(c);
    }
}
