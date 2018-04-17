package jo.filter;

import com.ib.client.Contract;

import jo.model.IApp;
import jo.model.MarketData;

public interface Filter {
    boolean isActive(IApp app, Contract contract, MarketData marketData);

    String getName();
}
