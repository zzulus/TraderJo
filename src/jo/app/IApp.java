package jo.app;

import java.util.Map;

import com.ib.client.Contract;

import jo.controller.IBroker;
import jo.model.MarketData;

public interface IApp {

    MarketData getMarketData(String symbol);

    Map<String, MarketData> getMarketDataMap();

    IBroker getIb();

    void initMarketData(Contract contract);
}
