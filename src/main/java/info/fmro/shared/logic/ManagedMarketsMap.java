package info.fmro.shared.logic;

import info.fmro.shared.utility.SynchronizedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ManagedMarketsMap
        extends SynchronizedMap<String, ManagedMarket>
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ManagedMarketsMap.class);
    private static final long serialVersionUID = -1774486587659030612L;
    private final String eventId;
    private transient boolean isInitialized;

    public ManagedMarketsMap(final String eventId) {
        super();
        this.eventId = eventId; // the exact object reference
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
            final ManagedEvent managedEvent = rulesManager.events.get(this.eventId, rulesManager.rulesHaveChanged);

            for (final String marketId : managedEvent.marketIds.copy()) {
                final ManagedMarket market = rulesManager.markets.get(marketId);
                if (market == null) { // I'll print error message, but I'll still add the null value to the returnMap
                    logger.error("null managedMarket found during initializeMap in rulesManager markets map for: {}", marketId);
                } else { // normal case, nothing to be done on branch
                }
                this.put(marketId, market);
            }

            this.isInitialized = true;
        }
    }

    @Nullable
    @Override
    public synchronized HashMap<String, ManagedMarket> copy() {
        logger.error("don't use this copy method !");
        return null;
    }

    public synchronized HashMap<String, ManagedMarket> copy(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.copy();
    }

    @Nullable
    @Override
    public synchronized Set<Map.Entry<String, ManagedMarket>> entrySetCopy() {
        logger.error("don't use this entrySetCopy method !");
        return null;
    }

    public synchronized Set<Map.Entry<String, ManagedMarket>> entrySetCopy(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.entrySetCopy();
    }

    @Nullable
    @Override
    public synchronized Set<String> keySetCopy() {
        logger.error("don't use this keySetCopy method !");
        return null;
    }

    public synchronized Set<String> keySetCopy(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.keySetCopy();
    }

    @Nullable
    @Override
    public synchronized Collection<ManagedMarket> valuesCopy() {
        logger.error("don't use this valuesCopy method !");
        return null;
    }

    public synchronized Collection<ManagedMarket> valuesCopy(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.valuesCopy();
    }

    @Nullable
    @Override
    public synchronized HashMap<String, ManagedMarket> clear() {
        logger.error("don't use this clear method !");
        return null;
    }

    public synchronized HashMap<String, ManagedMarket> clear(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.clear();
    }

    @Override
    public synchronized boolean containsKey(final String key) {
        logger.error("don't use this containsKey method !");
        return false;
    }

    public synchronized boolean containsKey(final String key, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(final ManagedMarket value) {
        logger.error("don't use this containsValue method !");
        return false;
    }

    public synchronized boolean containsValue(final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsValue(value);
    }

    @Nullable
    @Override
    public synchronized ManagedMarket get(final String key) {
        logger.error("don't use this get method !");
        return null;
    }

    public synchronized ManagedMarket get(final String key, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.get(key);
    }

    @Override
    public synchronized boolean isEmpty() {
        logger.error("don't use this isEmpty method !");
        return false;
    }

    public synchronized boolean isEmpty(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.isEmpty();
    }

    @Nullable
    @Override
    public synchronized ManagedMarket put(final String key, final ManagedMarket value, final boolean intentionalPutInsteadOfPutIfAbsent) {
        logger.error("don't use this put(3 args) method !");
        return null;
    }

    public synchronized ManagedMarket put(final String key, final ManagedMarket value, final boolean intentionalPutInsteadOfPutIfAbsent, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.put(key, value, intentionalPutInsteadOfPutIfAbsent);
    }

    @Nullable
    @Override
    public synchronized ManagedMarket put(final String key, final ManagedMarket value) {
        logger.error("don't use this put(2 args) method !");
        return null;
    }

    public synchronized ManagedMarket put(final String key, final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.put(key, value);
    }

    @Nullable
    @Override
    public synchronized ManagedMarket putIfAbsent(final String key, final ManagedMarket value) {
        logger.error("don't use this putIfAbsent method !");
        return null;
    }

    public synchronized ManagedMarket putIfAbsent(final String key, final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.putIfAbsent(key, value);
    }

    @Override
    public synchronized void putAll(final Map<? extends String, ? extends ManagedMarket> m) {
        logger.error("don't use this putAll method !");
    }

    public synchronized void putAll(final Map<String, ? extends ManagedMarket> m, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        super.putAll(m);
    }

    @Nullable
    @Override
    public synchronized ManagedMarket remove(final String key) {
        logger.error("don't use this remove(1 arg) method !");
        return null;
    }

    public synchronized ManagedMarket remove(final String key, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.remove(key);
    }

    @Override
    public synchronized boolean remove(final String key, final ManagedMarket value) {
        logger.error("don't use this remove(2 args) method !");
        return false;
    }

    public synchronized boolean remove(final String key, final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.remove(key, value);
    }

    @Override
    public synchronized int size() {
        logger.error("don't use this size method !");
        return -1;
    }

    public synchronized int size(@NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.size();
    }

    @Override
    public synchronized boolean containsEntry(final Map.Entry<String, ManagedMarket> entry) {
        logger.error("don't use this containsEntry method !");
        return false;
    }

    public synchronized boolean containsEntry(final Map.Entry<String, ManagedMarket> entry, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsEntry(entry);
    }

    @Override
    public synchronized boolean containsAllEntries(final Collection<?> c) {
        logger.error("don't use this containsAllEntries method !");
        return false;
    }

    public synchronized boolean containsAllEntries(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsAllEntries(c);
    }

    @Override
    public synchronized boolean removeEntry(final Map.Entry<String, ManagedMarket> entry) {
        logger.error("don't use this removeEntry method !");
        return false;
    }

    public synchronized boolean removeEntry(final Map.Entry<String, ManagedMarket> entry, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeEntry(entry);
    }

    @Override
    public synchronized boolean removeAllEntries(final Collection<?> c) {
        logger.error("don't use this removeAllEntries method !");
        return false;
    }

    public synchronized boolean removeAllEntries(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeAllEntries(c);
    }

    @Override
    public synchronized boolean retainAllEntries(final Collection<?> c) {
        logger.error("don't use this retainAllEntries( method !");
        return false;
    }

    public synchronized boolean retainAllEntries(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.retainAllEntries(c);
    }

    @Override
    public synchronized boolean containsAllKeys(final Collection<?> c) {
        logger.error("don't use this containsAllKeys method !");
        return false;
    }

    public synchronized boolean containsAllKeys(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsAllKeys(c);
    }

    @Override
    public synchronized boolean removeAllKeys(final Collection<?> c) {
        logger.error("don't use this removeAllKeys method !");
        return false;
    }

    public synchronized boolean removeAllKeys(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeAllKeys(c);
    }

    @Override
    public synchronized boolean retainAllKeys(final Collection<?> c) {
        logger.error("don't use this retainAllKeys method !");
        return false;
    }

    public synchronized boolean retainAllKeys(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.retainAllKeys(c);
    }

    @Override
    public synchronized boolean containsAllValues(final Collection<?> c) {
        logger.error("don't use this containsAllValues method !");
        return false;
    }

    public synchronized boolean containsAllValues(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.containsAllValues(c);
    }

    @Override
    public synchronized boolean removeValue(final ManagedMarket value) {
        logger.error("don't use this removeValue method !");
        return false;
    }

    public synchronized boolean removeValue(final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeValue(value);
    }

    @Override
    public synchronized boolean removeValueAll(final ManagedMarket value) {
        logger.error("don't use this removeValueAll method !");
        return false;
    }

    public synchronized boolean removeValueAll(final ManagedMarket value, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeValueAll(value);
    }

    @Override
    public synchronized boolean removeAllValues(final Collection<?> c) {
        logger.error("don't use this removeAllValues method !");
        return false;
    }

    public synchronized boolean removeAllValues(final Collection<?> c, @NotNull final RulesManager rulesManager) {
        initializeMap(rulesManager);
        return super.removeAllValues(c);
    }

    @Override
    public synchronized boolean retainAllValues(final Collection<?> c) {
        logger.error("don't use this retainAllValues method !");
        return false;
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
