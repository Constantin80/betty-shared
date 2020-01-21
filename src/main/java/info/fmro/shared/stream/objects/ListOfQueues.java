package info.fmro.shared.stream.objects;

import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ListOfQueues
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ListOfQueues.class);
    private static final long serialVersionUID = 8246293410799290222L;
    private final ArrayList<LinkedBlockingQueue<StreamObjectInterface>> list = new ArrayList<>(1);

    public synchronized boolean registerQueue(@NotNull final LinkedBlockingQueue<StreamObjectInterface> queue, @NotNull final StreamObjectInterface initialObject) {
        final boolean addedQueue;

        queue.add(initialObject.getCopy());
        addedQueue = this.list.add(queue);
        if (addedQueue) { // normal case, nothing to be done
        } else {
            logger.error("queue not added in list during registerQueue for: {} {} {}", this.list.size(), Generic.objectToString(queue), Generic.objectToString(this.list));
        }
        return addedQueue;
    }

    public synchronized boolean removeQueue(final LinkedBlockingQueue<StreamObjectInterface> queue) {
        final boolean foundQueue;
        foundQueue = this.list.remove(queue);
        if (foundQueue) { // normal case, nothing to be done
        } else {
            logger.error("queue not found in list during removeQueue for: {} {} {}", this.list.size(), Generic.objectToString(queue), Generic.objectToString(this.list));
        }

        return foundQueue;
    }

    public synchronized void send(final StreamObjectInterface object) { // will do nothing if list is empty
        for (final LinkedBlockingQueue<StreamObjectInterface> queue : this.list) {
            queue.add(object);
        }
    }

    public synchronized int size() {
        return this.list.size();
    }
}
