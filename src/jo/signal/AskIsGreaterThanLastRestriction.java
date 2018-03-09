package jo.signal;

import com.ib.client.Contract;

import jo.app.IApp;
import jo.model.MarketData;

public class AskIsGreaterThanLastRestriction implements Signal {

    public AskIsGreaterThanLastRestriction() {
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        // bid < last < ask
        return marketData.getAskPrice() > marketData.getLastPrice();
    }

    @Override
    public String getName() {
        return "";
    };
}
