package info.fmro.shared.utility;

import info.fmro.shared.objects.SafeObjectInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public SynchronizedSafeSet(final int initialSize) {
        super(initialSize);
    }

    private SynchronizedSafeSet(final int initialSize, final float loadFactor) {
        super(initialSize, loadFactor);
    }

    private SynchronizedSafeSet(final Collection<? extends E> collection) {
        super(collection);
    }

    // copyFrom seems just fine and doesn't need to be overridden

    @Override
    public synchronized boolean add(final E element) {
        final boolean isAdded = super.add(element);

        if (isAdded) {
            if (element == null) {
                logger.error("have added null element in SynchronizedSafeSet: {}", Generic.objectToString(this));
            } else {
                element.runAfterAdd();
            }
        } else { // not added, nothing to be done
        }

        return isAdded;
    }

    @Override
    public synchronized boolean addAll(@NotNull final Collection<? extends E> c) {
        boolean elementWasAdded = false;
        for (final E element : c) {
            if (!elementWasAdded && this.add(element)) {
                elementWasAdded = true;
            } else { // not added, or elementWasAdded already true; nothing to do
            }
        }

        return elementWasAdded;
    }

    @Override
    public synchronized HashSet<E> clear() {
        final HashSet<E> copy = super.clear();
        for (final E element : copy) {
            if (element != null) {
                element.runAfterRemoval();
            } else { // null elements are allowed, nothing to be done
            }
        }
        return copy;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized boolean remove(final Object object) {
        @Nullable E existingElement;
        try {
            //noinspection unchecked
            existingElement = object != null ? this.getEqualElement((E) object) : null;
        } catch (ClassCastException classCastException) {
            logger.error("existingElement not the right class in SynchronizedSafeSet.remove: {} {} {}", object.getClass(), Generic.objectToString(object), Generic.objectToString(this));
            existingElement = null;
        }

        final boolean isRemoved = super.remove(object);

        if (isRemoved) {
            if (existingElement != null) {
                existingElement.runAfterRemoval();
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
    public synchronized boolean removeAll(@NotNull final Collection<?> c) {
        boolean elementWasRemoved = false;
        for (final Object object : c) {
            if (!elementWasRemoved && this.remove(object)) {
                elementWasRemoved = true;
            } else { // not added, or elementWasRemoved already true; nothing to do
            }
        }
        return elementWasRemoved;
    }

    @Override
    public synchronized boolean retainAll(final Collection<?> c) {
        final HashSet<E> copy = copy();
        final boolean result = super.retainAll(c);
        for (final E element : copy) {
            if (element != null && !c.contains(element)) {
                element.runAfterRemoval();
            } else { // null elements are allowed, nothing to be done
            }
        }
        return result;
    }
}
