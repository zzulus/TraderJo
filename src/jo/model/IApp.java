package jo.model;

import java.util.Map;

import com.ib.client.Contract;

import jo.controller.IBroker;

public interface IApp {

    MarketData getMarketData(String symbol);

    Map<String, MarketData> getMarketDataMap();

    IBroker getIb();

    void initMarketData(Contract contract);
}
