package jo.position;

import jo.controller.IApp;
import jo.controller.IBroker;
import jo.model.MarketData;

public interface TrailAmountStrategy {
    Double getTrailAmount(MarketData md);

    default void init(IBroker ib, IApp app) {
    }
}
