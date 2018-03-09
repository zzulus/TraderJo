package jo.app;

import java.util.Map;

import jo.controller.IBroker;
import jo.model.MarketData;

public interface IApp {

    MarketData getMarketData(String symbol);

    Map<String, MarketData> getMarketDataMap();

    IBroker getIb();

}
