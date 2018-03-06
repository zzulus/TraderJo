package jo.command;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.WhatToShow;

import jo.app.TraderApp;
import jo.controller.IBService;
import jo.model.Bars;
import jo.model.MarketData;

public class InitStockDataCommand implements AppCommand {
    private static final Logger log = LogManager.getLogger(InitStockDataCommand.class);
    private List<BarSize> barSizes = Lists.newArrayList(BarSize._5_secs, BarSize._15_secs, BarSize._1_min, BarSize._2_mins, BarSize._5_mins);
    private Contract contract;

    public InitStockDataCommand(Contract contract) {
        this.contract = contract;
    }

    @Override
    public void execute(IBService ib, TraderApp app) {
        log.info("Init stock data for {}", contract.symbol());

        Map<String, MarketData> stockMarketDataMap = app.getStockMarketDataMap();

        MarketData marketData = new MarketData();
        stockMarketDataMap.put(contract.symbol(), marketData);

        for (BarSize barSize : barSizes) {
            marketData.initBars(barSize);
        }

        // IB supports only 5 sec realtime bars
        // https://interactivebrokers.github.io/tws-api/realtime_bars.html#gsc.tab=0
        Bars bars = marketData.getBars(BarSize._5_secs);
        ib.reqRealTimeBars(contract, WhatToShow.TRADES, true, (bar) -> bars.addBar(bar));

        // 165 for Average Volume and Low/High XXX Weeks
        // 233 for RT Volume (Time & Sales) https://interactivebrokers.github.io/tws-api/tick_types.html#rt_volume&gsc.tab=0
        // 375 for RT Trade Volume
        ib.reqTopMktData(contract, "165,375", /* snapshot */false, marketData.getTopMktDataHandler());
    }

}
