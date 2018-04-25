package jo.position;

import jo.controller.IBroker;
import jo.model.IApp;
import jo.model.MarketData;

public interface TrailAmountStrategy {
    Double getTrailAmount(MarketData md);

    void init(IBroker ib, IApp app);
}
