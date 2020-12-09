package info.fmro.shared.stream.definitions;

import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// objects of this class are read from the stream
public class OrderMarketChange
        implements Serializable {
    @Serial
    private static final long serialVersionUID = -4363278428265553269L;
    private Long accountId;
    private Boolean closed;
    private Boolean fullImage;
    private String id; // Market Id - the id of the market the order is on
    @Nullable
    private List<OrderRunnerChange> orc; // Order Changes - a list of changes to orders on a selection

    public synchronized Long getAccountId() {
        return this.accountId;
    }

    public synchronized void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    public synchronized Boolean getClosed() {
        return this.closed;
    }

    public synchronized void setClosed(final Boolean closed) {
        this.closed = closed;
    }

    public synchronized Boolean getFullImage() {
        return this.fullImage;
    }

    public synchronized void setFullImage(final Boolean fullImage) {
        this.fullImage = fullImage;
    }

    public synchronized String getId() {
        return this.id;
    }

    public synchronized void setId(final String id) {
        this.id = id;
    }

    @Nullable
    public synchronized List<OrderRunnerChange> getOrc() {
        return this.orc == null ? null : new ArrayList<>(this.orc);
    }

    public synchronized void setOrc(final List<? extends OrderRunnerChange> orc) {
        this.orc = orc == null ? null : new ArrayList<>(orc);
    }
}
