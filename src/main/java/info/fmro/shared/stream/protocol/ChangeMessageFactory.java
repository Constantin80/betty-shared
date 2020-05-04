package info.fmro.shared.stream.protocol;

import info.fmro.shared.stream.definitions.MarketChange;
import info.fmro.shared.stream.definitions.MarketChangeMessage;
import info.fmro.shared.stream.definitions.OrderChangeMessage;
import info.fmro.shared.stream.definitions.OrderMarketChange;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

// Adapts market or order changes to a common change message
@SuppressWarnings("UtilityClass")
public final class ChangeMessageFactory {
    @Contract(pure = true)
    private ChangeMessageFactory() {
    }

    @NotNull
    public static ChangeMessage<MarketChange> ToChangeMessage(final int clientId, @NotNull final MarketChangeMessage message) {
        final ChangeMessage<MarketChange> change = new ChangeMessage<>(clientId);
        change.setId(message.getId());
        change.setPublishTime(message.getPt());
        change.setClk(message.getClk());
        change.setInitialClk(message.getInitialClk());
        change.setConflateMs(message.getConflateMs());
        change.setHeartbeatMs(message.getHeartbeatMs());
        change.setItems(message.getMc());

        change.setSegmentType(message.getSegmentType());
        change.setChangeType(message.getCt());

        return change;
    }

    @NotNull
    public static ChangeMessage<OrderMarketChange> ToChangeMessage(final int clientId, @NotNull final OrderChangeMessage message) {
        final ChangeMessage<OrderMarketChange> change = new ChangeMessage<>(clientId);
        change.setId(message.getId());
        change.setPublishTime(message.getPt());
        change.setClk(message.getClk());
        change.setInitialClk(message.getInitialClk());
        change.setConflateMs(message.getConflateMs());
        change.setHeartbeatMs(message.getHeartbeatMs());
        change.setItems(message.getOc());

        change.setSegmentType(message.getSegmentType());
        change.setChangeType(message.getCt());

        return change;
    }
}
