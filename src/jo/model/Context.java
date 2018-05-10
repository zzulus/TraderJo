package jo.model;

import com.ib.client.Contract;

import jo.controller.IBroker;
import jo.model.MarketData;
import jo.trade.TradeBook;

public interface Context {

    MarketData getMarketData(String symbol);

    MarketData initMarketData(Contract contract);

    IBroker getIb();

    TradeBook getTradeBook();

}
