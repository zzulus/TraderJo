package jo.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Types.WhatToShow;

import jo.controller.IBroker;
import jo.trade.TradeBook;

public class DefaultContext implements Context {
    private static final Logger log = LogManager.getLogger(DefaultContext.class);
    private final Map<String, MarketData> marketDataMap = new ConcurrentHashMap<>();
    private final TradeBook tradeBook = new TradeBook();
    private IBroker ib;

    public DefaultContext(IBroker ib) {
        this.ib = ib;
    }

    @Override
    public MarketData getMarketData(String symbol) {
        return marketDataMap.get(symbol);
    }

    @Override
    public synchronized MarketData initMarketData(Contract contract) {
        log.info("Init stock data for {}", contract.symbol());

        MarketData marketData = marketDataMap.get(contract.symbol());

        if (marketData != null) {
            log.info("Already subscribed for {}", contract.symbol());
        } else {
            marketData = new MarketData(contract);
            marketData.startRecording();

            marketDataMap.put(contract.symbol(), marketData);

            // IB supports only 5 sec realtime bars
            // https://interactivebrokers.github.io/tws-api/realtime_bars.html#gsc.tab=0
            ib.reqRealTimeBars(contract, WhatToShow.TRADES, true, marketData);

            // 165 for Average Volume and Low/High XXX Weeks
            // 233 for RT Volume (Time & Sales) https://interactivebrokers.github.io/tws-api/tick_types.html#rt_volume&gsc.tab=0
            // 375 for RT Trade Volume
            ib.reqTopMktData(contract, "165,375", /* snapshot */false, marketData);
        }

        return marketData;
    }

    @Override
    public IBroker getIb() {
        return ib;
    }

    public void setIb(IBroker ib) {
        this.ib = ib;
    }

    @Override
    public TradeBook getTradeBook() {
        return tradeBook;
    }

}
