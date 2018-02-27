package jo.signal;

import com.ib.client.Contract;

import jo.app.App;
import jo.model.MarketData;

public interface Signal {
    boolean isActive(App app, Contract contract, MarketData marketData);

    String getName();
}
