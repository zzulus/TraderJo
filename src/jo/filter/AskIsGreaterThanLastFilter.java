package jo.filter;

import com.ib.client.Contract;

import jo.model.IApp;
import jo.model.MarketData;

public class AskIsGreaterThanLastFilter implements Filter {

    public AskIsGreaterThanLastFilter() {
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
