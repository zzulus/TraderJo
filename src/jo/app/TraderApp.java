package jo.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.WhatToShow;

import jo.bot.Bot;
import jo.bot.DonchianBot;
import jo.bot.MovingAverageHLBot;
import jo.command.AppCommand;
import jo.command.StartBotsCommand;
import jo.constant.Stocks;
import jo.controller.IBService;
import jo.controller.IBroker;
import jo.handler.IConnectionHandler;
import jo.model.IApp;
import jo.model.MarketData;
import jo.position.DollarValuePositionSizeStrategy;
import jo.position.DollarValueTrailAmountStrategy;
import jo.position.HighLowAvgTrailAmountStrategy;
import jo.position.HistoricalHighLowAvgTrailAmountStrategy;
import jo.position.PositionSizeStrategy;
import jo.position.TrailAmountStrategy;

public class TraderApp implements IApp {
    private static final Logger log = LogManager.getLogger(TraderApp.class);
    private List<AppCommand> postConnectCommands = new ArrayList<>();
    private IBroker ib;
    // Key - Stock name, e.g. SPY
    private Map<String, MarketData> stockMarketDataMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new TraderApp().start();
    }

    public TraderApp() {
        Contract spy = Stocks.smartOf("SPY");
        Contract xle = Stocks.smartOf("XLE");
        Contract xlb = Stocks.smartOf("XLB");
        Contract ibb = Stocks.smartOf("IBB");
        Contract xlu = Stocks.smartOf("XLU");

        PositionSizeStrategy positionSizeStrategy = new DollarValuePositionSizeStrategy(6650);

        List<String> stockSymbols = Lists.newArrayList(
                "AAPL",
                "MSFT",
                "TSLA",
                "FB",
                "BABA",
                "NFLX",
                "NVDA",
                "CAT",
                "INTC",
                "WFC",
                "XLE",
                "PG",
                "C",
                "JPM",
                "SMH",
                "XOM",
                "MO",
                "MMM",
                "XLB",
                "V",
                "PYPL"

        );

        List<Bot> bots = new ArrayList<>();
        for (String stockSymbol : stockSymbols) {
            Contract contract = Stocks.smartOf(stockSymbol);
            TrailAmountStrategy trailAmountStrategy = new HistoricalHighLowAvgTrailAmountStrategy(BarSize._1_min, 1, 0, contract);
            MovingAverageHLBot bot = new MovingAverageHLBot(contract, positionSizeStrategy, trailAmountStrategy);
            bots.add(bot);
        }

        postConnectCommands = Lists.newArrayList(new StartBotsCommand(bots));
    }

    public void start() {
        ib = new IBService();

        IConnectionHandler connHandler = new IConnectionHandler() {

            @Override
            public void show(String string) {
                log.info("show: " + string);
            }

            @Override
            public void message(int id, int errorCode, String errorMsg) {
                log.info("message: id {}, errorCode {}, errorMsg {}", id, errorCode, errorMsg);
                if (errorMsg.contains("Connectivity between IB and Trader Workstation has been lost")) {
                    System.exit(0);
                }
            }

            @Override
            public void error(Exception e) {
                log.error("Error", e);
                e.printStackTrace();
            }

            @Override
            public void disconnected() {
                log.info("disconnected");
            }

            @Override
            public void connected() {
                log.info("connected, executing post connect commands");

                for (AppCommand command : postConnectCommands) {
                    command.execute(ib, TraderApp.this);
                }
            }

            @Override
            public void accountList(List<String> accounts) {
            }
        };

        ib.connectLocalhostLive(connHandler);
    }

    @Override
    public IBroker getIb() {
        return ib;
    }

    @Override
    public Map<String, MarketData> getMarketDataMap() {
        return stockMarketDataMap;
    }

    @Override
    public MarketData getMarketData(String symbol) {
        return stockMarketDataMap.get(symbol);
    }

    public void setPostConnectCommands(List<AppCommand> postConnectCommands) {
        this.postConnectCommands = postConnectCommands;
    }

    @Override
    public synchronized void initMarketData(Contract contract) {
        log.info("Init stock data for {}", contract.symbol());

        Map<String, MarketData> stockMarketDataMap = getMarketDataMap();
        if (stockMarketDataMap.containsKey(contract.symbol())) {
            log.info("Already subscribed for {}", contract.symbol());
            return;
        }

        MarketData marketData = new MarketData();
        stockMarketDataMap.put(contract.symbol(), marketData);

        // IB supports only 5 sec realtime bars
        // https://interactivebrokers.github.io/tws-api/realtime_bars.html#gsc.tab=0
        ib.reqRealTimeBars(contract, WhatToShow.TRADES, true, (bar) -> marketData.addBar(BarSize._5_secs, bar));

        // 165 for Average Volume and Low/High XXX Weeks
        // 233 for RT Volume (Time & Sales) https://interactivebrokers.github.io/tws-api/tick_types.html#rt_volume&gsc.tab=0
        // 375 for RT Trade Volume
        ib.reqTopMktData(contract, "165,375", /* snapshot */false, marketData.getTopMktDataHandler());

        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

    }
}
