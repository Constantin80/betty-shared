package info.fmro.shared.stream.objects;

import info.fmro.shared.stream.cache.order.OrderMarketRunner;
import info.fmro.shared.stream.definitions.OrderRunnerChange;
import info.fmro.shared.stream.enums.Side;

public interface OrdersThreadInterface {
    boolean addCancelOrder(String marketId, RunnerId runnerId, Side side, double price, double size, String betId, Double sizeReduction);

    void reportStreamChange(OrderMarketRunner orderMarketRunner, OrderRunnerChange orderRunnerChange);

    double addPlaceOrder(String marketId, RunnerId runnerId, Side side, double price, double size);

    void checkTemporaryOrdersExposure(String marketId, RunnerId runnerId, OrderMarketRunner orderMarketRunner);
}
