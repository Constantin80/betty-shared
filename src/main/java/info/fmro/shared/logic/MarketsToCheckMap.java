package info.fmro.shared.logic;

import info.fmro.shared.utility.Generic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MarketsToCheckMap
        implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(MarketsToCheckMap.class);
    @Serial
    private static final long serialVersionUID = 6317328995427024742L;
    private final TreeMap<String, Long> map = new TreeMap<>(); // marketId, timeStamp of request to check
    @SuppressWarnings("FieldNotUsedInToString")
    private final AtomicBoolean marketsToCheckExist;

    MarketsToCheckMap(@NotNull final AtomicBoolean marketsToCheckExist) {
        this.marketsToCheckExist = marketsToCheckExist;
    }

    public synchronized boolean contains(@NotNull final String marketId) {
        return this.map.containsKey(marketId);
    }

    public synchronized int put(@NotNull final Iterable<String> marketIds, @NotNull final Long value) {
        int elementsAdded = 0;
        for (final String marketId : marketIds) {
            elementsAdded += Generic.booleanToInt(put(marketId, value));
        }
        return elementsAdded;
    }

    public synchronized boolean put(@NotNull final String marketId, @NotNull final Long value) {
        final boolean elementAdded;
        final Long existingValue = this.map.get(marketId);
        if (existingValue == null || existingValue < value) {
            elementAdded = true;
            this.map.put(marketId, value);
        } else { // existingValue >= value
            elementAdded = false;
        }

        if (elementAdded) {
            this.marketsToCheckExist.set(true);
        } else { // nothing to be done on this branch
        }

//        logger.info("MarketsToCheckMap put {} time: {}", marketId, Generic.properTimeStamp(value));
        return elementAdded;
    }

    public synchronized boolean isEmpty() {
        return this.map.isEmpty();
    }

    public synchronized int size() {
        return this.map.size();
    }

    public synchronized Map.Entry<String, Long> poll() {
        return this.map.pollFirstEntry();
    }

    public synchronized void clear() {
        this.map.clear();
    }

    public synchronized boolean putAll(@NotNull final MarketsToCheckMap other) {
        int elementsAdded = 0;
        for (@NotNull final Map.Entry<String, Long> entry : other.map.entrySet()) {
            final String key = entry.getKey();
            final Long value = entry.getValue();
            if (value == null) {
                logger.error("null value in addAll for: {} {} {}", key, value, other);
            } else {
                elementsAdded += Generic.booleanToInt(this.put(key, value));
            }
        }

        final boolean elementAdded = elementsAdded > 0;
        if (elementAdded) {
            this.marketsToCheckExist.set(true);
        } else { // nothing to be done on this branch
        }
        return elementAdded;
    }

    @Override
    public synchronized String toString() {
        final StringBuilder returnStringBuilder = new StringBuilder("size:");
        returnStringBuilder.append(this.map.size());
        if (this.map.isEmpty()) { // nothing more to be appended
        } else {
            returnStringBuilder.append(" [");
            final Iterator<Map.Entry<String, Long>> iterator = this.map.entrySet().iterator();
            while (iterator.hasNext()) {
                @NotNull final Map.Entry<String, Long> entry = iterator.next();
                returnStringBuilder.append(entry.getKey()).append(':').append(entry.getValue());
                if (iterator.hasNext()) {
                    returnStringBuilder.append(", ");
                }
            }
            returnStringBuilder.append("]");
        }
        return returnStringBuilder.toString();
    }
}
