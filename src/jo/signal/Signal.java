package jo.signal;

import com.ib.client.Contract;

import jo.app.TraderApp;
import jo.model.MarketData;

public interface Signal {
    boolean isActive(TraderApp app, Contract contract, MarketData marketData);

    String getName();
}
