package info.fmro.shared.logic;

import info.fmro.shared.utility.SynchronizedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("WeakerAccess")
public class ManagedEventsMap
        extends SynchronizedMap<String, ManagedEvent>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedEventsMap.class);
    private static final long serialVersionUID = 3165547218025993674L;

    public ManagedEventsMap() {
        super();
    }

    public ManagedEventsMap(final int initialSize) {
        super(initialSize);
    }

    public ManagedEventsMap(final int initialSize, final float loadFactor) {
        super(initialSize, loadFactor);
    }

    @Nullable
    public synchronized ManagedEvent get(final String key) {
        logger.error("don't use get(String) method, use get(String, AtomicBoolean) !");

        return null;
    }

    @NotNull
    public synchronized ManagedEvent get(final String key, @NotNull final AtomicBoolean rulesHaveChanged) {
        ManagedEvent managedEvent = super.get(key);
        if (managedEvent == null) {
            managedEvent = new ManagedEvent(key);
            put(key, managedEvent);
            rulesHaveChanged.set(true);
        } else { // I got the event and I'll return it, nothing else to be done
        }

        return managedEvent;
    }
}
