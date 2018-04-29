package jo.command;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Types.WhatToShow;

import jo.controller.IBroker;
import jo.model.IApp;
import jo.model.MarketData;

public class InitStockDataCommand implements AppCommand {
    private static final Logger log = LogManager.getLogger(InitStockDataCommand.class);
    private Contract contract;

    public InitStockDataCommand(Contract contract) {
        this.contract = contract;
    }

    @Override
    public void execute(IBroker ib, IApp app) {
        log.info("Init stock data for {}", contract.symbol());

        Map<String, MarketData> stockMarketDataMap = app.getMarketDataMap();

        MarketData marketData = new MarketData();
        stockMarketDataMap.put(contract.symbol(), marketData);

        // IB supports only 5 sec realtime bars
        // https://interactivebrokers.github.io/tws-api/realtime_bars.html#gsc.tab=0
        ib.reqRealTimeBars(contract, WhatToShow.TRADES, true, marketData);

        // 165 for Average Volume and Low/High XXX Weeks
        // 233 for RT Volume (Time & Sales) https://interactivebrokers.github.io/tws-api/tick_types.html#rt_volume&gsc.tab=0
        // 375 for RT Trade Volume
        ib.reqTopMktData(contract, "165,375", /* snapshot */false, marketData);
    }

}
