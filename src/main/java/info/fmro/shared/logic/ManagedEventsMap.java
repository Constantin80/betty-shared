package info.fmro.shared.logic;

import info.fmro.shared.utility.SynchronizedMap;

import java.io.Serial;
import java.io.Serializable;

@SuppressWarnings("WeakerAccess")
public class ManagedEventsMap
        extends SynchronizedMap<String, ManagedEvent>
        implements Serializable {
    @Serial
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

//    @NotNull
//    public synchronized ManagedEvent getOrCreate(final String key, @NotNull final AtomicBoolean rulesHaveChanged) {
//        ManagedEvent managedEvent = get(key);
//        if (managedEvent == null) {
//            managedEvent = new ManagedEvent(key);
//            put(key, managedEvent);
//            rulesHaveChanged.set(true);
//        } else { // I got the event and I'll return it, nothing else to be done
//        }
//
//        return managedEvent;
//    }
}
