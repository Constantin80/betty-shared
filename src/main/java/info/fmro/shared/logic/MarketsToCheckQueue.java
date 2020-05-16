package info.fmro.shared.logic;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MarketsToCheckQueue<T>
        implements Serializable {
    private static final long serialVersionUID = 6317328995427024742L;
    private final Queue<T> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean marketsToCheckExist;

    @SuppressWarnings("WeakerAccess")
    public MarketsToCheckQueue(@NotNull final AtomicBoolean marketsToCheckExist) {
        this.marketsToCheckExist = marketsToCheckExist;
    }

    public synchronized boolean add(@NotNull final T marketId) {
        final boolean elementAdded = this.concurrentLinkedQueue.add(marketId);
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

    public synchronized boolean addAll(final Collection<? extends T> c) {
        final boolean elementAdded = this.concurrentLinkedQueue.addAll(c);
        if (elementAdded) {
            this.marketsToCheckExist.set(true);
        } else { // nothing to be done on this branch
        }
        return elementAdded;
    }

    public synchronized boolean addAll(final MarketsToCheckQueue<? extends T> c) {
        final boolean elementAdded = c != null && this.concurrentLinkedQueue.addAll(c.concurrentLinkedQueue);
        if (elementAdded) {
            this.marketsToCheckExist.set(true);
        } else { // nothing to be done on this branch
        }
        return elementAdded;
    }
}
