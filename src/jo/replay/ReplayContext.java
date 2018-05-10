package jo.replay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ib.client.Contract;

import jo.controller.IBroker;
import jo.model.Context;
import jo.model.MarketData;
import jo.trade.TradeBook;

public class ReplayContext implements Context {
    private final Map<String, MarketData> marketDataMap = new ConcurrentHashMap<>();
    private ReplayBroker replayBroker;
    private ReplayOrderManager orderManager;
    private final TradeBook tradeBook = new TradeBook();

    public MarketData initMarketData(String symbol) {
        return marketDataMap.computeIfAbsent(symbol, (k) -> new MarketData());
    }

    @Override
    public MarketData getMarketData(String symbol) {
        return marketDataMap.get(symbol);
    }

    @Override
    public MarketData initMarketData(Contract contract) {
        String symbol = contract.symbol();
        return marketDataMap.computeIfAbsent(symbol, (k) -> new MarketData());
    }

    @Override
    public IBroker getIb() {
        return replayBroker;
    }

    public ReplayBroker getReplayBroker() {
        return replayBroker;
    }

    public ReplayOrderManager getOrderManager() {
        return orderManager;
    }

    public void setReplayBroker(ReplayBroker replayBroker) {
        this.replayBroker = replayBroker;
    }

    public void setOrderManager(ReplayOrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @Override
    public TradeBook getTradeBook() {
        return tradeBook;
    }
}
