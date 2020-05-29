package info.fmro.shared.logic;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MarketsToCheckQueue<T>
        implements Serializable {
    private static final long serialVersionUID = 6317328995427024742L;
    private final Queue<T> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
    @SuppressWarnings("FieldNotUsedInToString")
    private final AtomicBoolean marketsToCheckExist;

    public MarketsToCheckQueue(@NotNull final AtomicBoolean marketsToCheckExist) {
        this.marketsToCheckExist = marketsToCheckExist;
    }

    public synchronized boolean contains(@NotNull final T marketId) {
        return this.concurrentLinkedQueue.contains(marketId);
    }

    public synchronized boolean add(@NotNull final T marketId) {
        final boolean elementAdded;
        if (contains(marketId)) {
            elementAdded = false; // won't add duplicates
        } else {
            elementAdded = this.concurrentLinkedQueue.add(marketId);
        }
        if (elementAdded) {
            this.marketsToCheckExist.set(true);
        } else { // nothing to be done on this branch
        }
        return elementAdded;
    }

    public synchronized boolean isEmpty() {
        return this.concurrentLinkedQueue.isEmpty();
    }

    public synchronized int size() {
        return this.concurrentLinkedQueue.size();
    }

    public synchronized T poll() {
        return this.concurrentLinkedQueue.poll();
    }

    public synchronized void clear() {
        this.concurrentLinkedQueue.clear();
    }

    public synchronized boolean addAll(@NotNull final Collection<? extends T> c) {
        final Collection<? extends T> localC = new HashSet<>(c); // I'd rather not modify the original collection
        localC.removeAll(this.concurrentLinkedQueue); // no duplicates allowed
        final boolean elementAdded = this.concurrentLinkedQueue.addAll(localC);
        if (elementAdded) {
            this.marketsToCheckExist.set(true);
        } else { // nothing to be done on this branch
        }
        return elementAdded;
    }

    public synchronized boolean addAll(@NotNull final MarketsToCheckQueue<? extends T> c) {
        final Collection<? extends T> localC = new HashSet<>(c.concurrentLinkedQueue); // I'd rather not modify the original collection
        localC.removeAll(this.concurrentLinkedQueue); // no duplicates allowed
        final boolean elementAdded = this.concurrentLinkedQueue.addAll(localC);
        if (elementAdded) {
            this.marketsToCheckExist.set(true);
        } else { // nothing to be done on this branch
        }
        return elementAdded;
    }

    @Override
    public synchronized String toString() {
        final StringBuilder returnStringBuilder = new StringBuilder("size:");
        returnStringBuilder.append(this.concurrentLinkedQueue.size());
        if (this.concurrentLinkedQueue.isEmpty()) { // nothing more to be appended
        } else {
            returnStringBuilder.append(" [");
            final Iterator<? extends T> iterator = this.concurrentLinkedQueue.iterator();
            while (iterator.hasNext()) {
                returnStringBuilder.append(iterator.next());
                if (iterator.hasNext()) {
                    returnStringBuilder.append(", ");
                }
            }
            returnStringBuilder.append("]");
        }
        return returnStringBuilder.toString();
    }
}
