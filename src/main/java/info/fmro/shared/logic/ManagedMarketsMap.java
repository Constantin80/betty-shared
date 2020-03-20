package info.fmro.shared.logic;

import info.fmro.shared.utility.Generic;
import info.fmro.shared.utility.SynchronizedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass"})
public class ManagedMarketsMap
        extends SynchronizedMap<String, ManagedMarket>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedMarketsMap.class);
    private static final long serialVersionUID = -8982542372821302647L;
    private boolean isInitialized;
    @Nullable
    private transient ManagedEvent parentEvent;

    ManagedMarketsMap(@NotNull final ManagedEvent parentEvent) {
        super();
        this.parentEvent = parentEvent;
    }

    ManagedMarketsMap(@NotNull final ManagedEvent parentEvent, @NotNull final RulesManager rulesManager) {
        this(parentEvent);
        initializeMap(rulesManager);
    }

    private void readObject(@NotNull final java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        logger.error("readObject invoked for ManagedMarketsMap; this should never happen, the fields with this class should always be transient: {}", Generic.objectToString(this));
        this.parentEvent = null;
    }
//    private ManagedMarketsMap(final int initialSize, final String eventId) {
//        super(initialSize);
//        this.eventId = eventId; // the exact object reference
//    }
//
//    private ManagedMarketsMap(final int initialSize, final float loadFactor, final String eventId) {
//        super(initialSize, loadFactor);
//        this.eventId = eventId; // the exact object reference
//    }

    private synchronized void initializeMap(@NotNull final RulesManager rulesManager) {
        if (this.isInitialized) { // already initialized, won't initialize again
        } else {
            this.isInitialized = true; // in the beginning, to avoid cycle
            if (this.parentEvent == null) {
                logger.error("null parentEvent during initializeMap for: {}", Generic.objectToString(this));
            } else {
                final String eventId = this.parentEvent.getId();
                for (@NotNull final Map.Entry<String, ManagedMarket> entry : rulesManager.markets.entrySetCopy()) {
                    final ManagedMarket market = entry.getValue();
                    final String marketId = entry.getKey();
                    final String marketParentId = market == null ? null : market.simpleGetParentEventId();
                    if (Objects.equals(eventId, marketParentId)) {
                        this.putIfAbsent(marketId, market, rulesManager);
                    } else { // market not belonging to this event, nothing to be done
                    }
                }
                for (final String marketId : this.parentEvent.marketIds.copy()) {
                    final ManagedMarket market = rulesManager.markets.get(marketId);
                    if (market == null) {
                        logger.error("null managedMarket found during initializeMap in rulesManager markets map for: {}", marketId);
                        this.parentEvent.marketIds.remove(marketId);
                    } else { // normal case
                        this.putIfAbsent(marketId, market, rulesManager);
                    }
                }
            }
        }
    }

    @Nullable
    @Override
    public synchronized HashMap<String, ManagedMarket> copy() {
        @Nullable final HashMap<String, ManagedMarket> returnValue;
        if (this.isInitialized) {
            returnValue = super.copy();
        } else {
            logger.error("not isInitialized in copy method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized HashMap<String, ManagedMarket> copy(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.copy();
    }

    @Nullable
    @Override
    public synchronized Set<Map.Entry<String, ManagedMarket>> entrySetCopy() {
        @Nullable final Set<Map.Entry<String, ManagedMarket>> returnValue;
        if (this.isInitialized) {
            returnValue = super.entrySetCopy();
        } else {
            logger.error("not isInitialized in entrySetCopy method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized Set<Map.Entry<String, ManagedMarket>> entrySetCopy(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.entrySetCopy();
    }

    @Nullable
    @Override
    public synchronized Set<String> keySetCopy() {
        @Nullable final Set<String> returnValue;
        if (this.isInitialized) {
            returnValue = super.keySetCopy();
        } else {
            logger.error("not isInitialized in keySetCopy method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized Set<String> keySetCopy(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.keySetCopy();
    }

    @Nullable
    @Override
    public synchronized Collection<ManagedMarket> valuesCopy() {
        @Nullable final Collection<ManagedMarket> returnValue;
        if (this.isInitialized) {
            returnValue = super.valuesCopy();
        } else {
            logger.error("not isInitialized in valuesCopy method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized Collection<ManagedMarket> valuesCopy(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.valuesCopy();
    }

    @Nullable
    @Override
    public synchronized HashMap<String, ManagedMarket> clear() {
        @Nullable final HashMap<String, ManagedMarket> returnValue;
        if (this.isInitialized) {
            returnValue = super.clear();
        } else {
            logger.error("not isInitialized in clear method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized HashMap<String, ManagedMarket> clear(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.clear();
    }

    @Override
    public synchronized boolean containsKey(final String key) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.containsKey(key);
        } else {
            logger.error("not isInitialized in containsKey method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean containsKey(final String key, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(final ManagedMarket value) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.containsValue(value);
        } else {
            logger.error("not isInitialized in containsValue method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean containsValue(final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsValue(value);
    }

    @Nullable
    @Override
    public synchronized ManagedMarket get(final String key) {
        @Nullable final ManagedMarket returnValue;
        if (this.isInitialized) {
            returnValue = super.get(key);
        } else {
            logger.error("not isInitialized in get method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized ManagedMarket get(final String key, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.get(key);
    }

    @Override
    public synchronized boolean isEmpty() {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.isEmpty();
        } else {
            logger.error("not isInitialized in isEmpty method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean isEmpty(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.isEmpty();
    }

    @Nullable
    @Override
    public synchronized ManagedMarket put(final String key, final ManagedMarket value, final boolean intentionalPutInsteadOfPutIfAbsent) {
        @Nullable final ManagedMarket returnValue;
        if (this.isInitialized) {
            returnValue = super.put(key, value, intentionalPutInsteadOfPutIfAbsent);
        } else {
            logger.error("not isInitialized in put(3 args) method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized ManagedMarket put(final String key, final ManagedMarket value, final boolean intentionalPutInsteadOfPutIfAbsent, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.put(key, value, intentionalPutInsteadOfPutIfAbsent);
    }

    @Nullable
    @Override
    public synchronized ManagedMarket put(final String key, final ManagedMarket value) {
        @Nullable final ManagedMarket returnValue;
        if (this.isInitialized) {
            returnValue = super.put(key, value);
        } else {
            logger.error("not isInitialized in put(2 args) method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized ManagedMarket put(final String key, final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.put(key, value);
    }

    @Nullable
    @Override
    public synchronized ManagedMarket putIfAbsent(final String key, final ManagedMarket value) {
        @Nullable final ManagedMarket returnValue;
        if (this.isInitialized) {
            returnValue = super.putIfAbsent(key, value);
        } else {
            logger.error("not isInitialized in putIfAbsent method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized ManagedMarket putIfAbsent(final String key, final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.putIfAbsent(key, value);
    }

    @Override
    public synchronized void putAll(final Map<? extends String, ? extends ManagedMarket> m) {
        if (this.isInitialized) {
            super.putAll(m);
        } else {
            logger.error("not isInitialized in putAll method !");
        }
    }

    public synchronized void putAll(final Map<String, ? extends ManagedMarket> m, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        super.putAll(m);
    }

    @Nullable
    @Override
    public synchronized ManagedMarket remove(final String key) {
        @Nullable final ManagedMarket returnValue;
        if (this.isInitialized) {
            returnValue = super.remove(key);
        } else {
            logger.error("not isInitialized in remove method !");
            returnValue = null;
        }
        return returnValue;
    }

    public synchronized ManagedMarket remove(final String key, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.remove(key);
    }

    @Override
    public synchronized boolean remove(final String key, final ManagedMarket value) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.remove(key, value);
        } else {
            logger.error("not isInitialized in remove(2 args) method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean remove(final String key, final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.remove(key, value);
    }

    @Override
    public synchronized int size() {
        final int returnValue;
        if (this.isInitialized) {
            returnValue = super.size();
        } else {
            logger.error("not isInitialized in size method !");
            returnValue = -1;
        }
        return returnValue;
    }

    public synchronized int size(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.size();
    }

    @Override
    public synchronized boolean containsEntry(final Map.Entry<String, ManagedMarket> entry) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.containsEntry(entry);
        } else {
            logger.error("not isInitialized in containsEntry method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean containsEntry(final Map.Entry<String, ManagedMarket> entry, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsEntry(entry);
    }

    @Override
    public synchronized boolean containsAllEntries(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.containsAllEntries(c);
        } else {
            logger.error("not isInitialized in containsAllEntries method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean containsAllEntries(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsAllEntries(c);
    }

    @Override
    public synchronized boolean removeEntry(final Map.Entry<String, ManagedMarket> entry) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.removeEntry(entry);
        } else {
            logger.error("not isInitialized in removeEntry method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean removeEntry(final Map.Entry<String, ManagedMarket> entry, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeEntry(entry);
    }

    @Override
    public synchronized boolean removeAllEntries(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.removeAllEntries(c);
        } else {
            logger.error("not isInitialized in removeAllEntries method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean removeAllEntries(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeAllEntries(c);
    }

    @Override
    public synchronized boolean retainAllEntries(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.retainAllEntries(c);
        } else {
            logger.error("not isInitialized in retainAllEntries method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean retainAllEntries(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.retainAllEntries(c);
    }

    @Override
    public synchronized boolean containsAllKeys(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.containsAllEntries(c);
        } else {
            logger.error("not isInitialized in containsAllEntries method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean containsAllKeys(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsAllKeys(c);
    }

    @Override
    public synchronized boolean removeAllKeys(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.removeAllKeys(c);
        } else {
            logger.error("not isInitialized in removeAllKeys method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean removeAllKeys(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeAllKeys(c);
    }

    @Override
    public synchronized boolean retainAllKeys(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.retainAllKeys(c);
        } else {
            logger.error("not isInitialized in retainAllKeys method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean retainAllKeys(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.retainAllKeys(c);
    }

    @Override
    public synchronized boolean containsAllValues(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.containsAllValues(c);
        } else {
            logger.error("not isInitialized in containsAllValues method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean containsAllValues(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsAllValues(c);
    }

    @Override
    public synchronized boolean removeValue(final ManagedMarket value) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.removeValue(value);
        } else {
            logger.error("not isInitialized in removeValue method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean removeValue(final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeValue(value);
    }

    @Override
    public synchronized boolean removeValueAll(final ManagedMarket value) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.removeValueAll(value);
        } else {
            logger.error("not isInitialized in removeValueAll method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean removeValueAll(final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeValueAll(value);
    }

    @Override
    public synchronized boolean removeAllValues(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.removeAllValues(c);
        } else {
            logger.error("not isInitialized in removeAllValues method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean removeAllValues(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeAllValues(c);
    }

    @Override
    public synchronized boolean retainAllValues(final Collection<?> c) {
        final boolean returnValue;
        if (this.isInitialized) {
            returnValue = super.retainAllValues(c);
        } else {
            logger.error("not isInitialized in retainAllValues method !");
            returnValue = false;
        }
        return returnValue;
    }

    public synchronized boolean retainAllValues(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.retainAllValues(c);
    }

//    @Override
//    public synchronized int hashCode() {
//        initializeMap();
//        return super.hashCode();
//    }
//
//    @Contract(value = "null -> false", pure = true)
//    @Override
//    public synchronized boolean equals(final Object obj) {
//        initializeMap();
//        return super.equals(obj);
//    }
}
