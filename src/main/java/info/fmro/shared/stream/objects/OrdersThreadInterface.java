package info.fmro.shared.stream.objects;

import info.fmro.shared.objects.Exposure;
import info.fmro.shared.stream.cache.order.OrderMarketRunner;
import info.fmro.shared.stream.definitions.OrderRunnerChange;
import info.fmro.shared.stream.enums.Side;
import org.jetbrains.annotations.NotNull;

// OrdersThreadInterface implementation in client Statics is null; if I ever want to send such objects from server to client, some changes might be needed
@SuppressWarnings({"InterfaceNeverImplemented", "RedundantSuppression"})
public interface OrdersThreadInterface {
    boolean addCancelOrder(String marketId, RunnerId runnerId, Side side, double price, double size, String betId, Double sizeReduction);

    void reportStreamChange(OrderMarketRunner orderMarketRunner, OrderRunnerChange orderRunnerChange);

    double addPlaceOrder(String marketId, RunnerId runnerId, Side side, double price, double size);

    void checkTemporaryOrdersExposure(String marketId, RunnerId runnerId, @NotNull Exposure exposure);
}
