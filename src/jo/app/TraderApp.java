package jo.app;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
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
import jo.handler.IHistoricalDataHandler;
import jo.model.Bar;
import jo.model.Bars;
import jo.model.IApp;
import jo.model.MarketData;
import jo.position.DollarValuePositionSizeStrategy;
import jo.position.DollarValueTrailAmountStrategy;
import jo.position.HighLowAvgTrailAmountStrategy;
import jo.position.HistoricalHighLowAvgTrailAmountStrategy;
import jo.position.PositionSizeStrategy;
import jo.position.TrailAmountStrategy;
import jo.util.AsyncExec;
import jo.util.AsyncVal;

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

        Set<String> stockSymbols = new LinkedHashSet<>();
        stockSymbols.add("AAPL");
        stockSymbols.add("MSFT");
        stockSymbols.add("TSLA");
        stockSymbols.add("FB");
        stockSymbols.add("BABA");
        stockSymbols.add("NFLX");
        stockSymbols.add("NVDA");
        stockSymbols.add("CAT");
        stockSymbols.add("INTC");
        stockSymbols.add("WFC");
        stockSymbols.add("XLE");
        stockSymbols.add("PG");
        stockSymbols.add("C");
        stockSymbols.add("JPM");
        stockSymbols.add("SMH");
        stockSymbols.add("XOM");
        stockSymbols.add("MO");
        stockSymbols.add("MMM");
        stockSymbols.add("XLB");
        stockSymbols.add("V");
        stockSymbols.add("PYPL");

        stockSymbols.addAll(MarketRecorderStocks.TICKS_ONLY_STOCKS);

        List<Bot> bots = new ArrayList<>();
        for (String stockSymbol : stockSymbols) {
            Contract contract = Stocks.smartOf(stockSymbol);
            //TrailAmountStrategy trailAmountStrategy = new HistoricalHighLowAvgTrailAmountStrategy(BarSize._1_min, 1, 0, contract);
            TrailAmountStrategy trailAmountStrategy = HighLowAvgTrailAmountStrategy.createDefault();
            MovingAverageHLBot bot = new MovingAverageHLBot(contract, positionSizeStrategy, trailAmountStrategy);
            bots.add(bot);
        }

        postConnectCommands = Lists.newArrayList(new StartBotsCommand(bots));
    }

    public void start() {
        ib = new IBService();

        IConnectionHandler connHandler = new IConnectionHandler() {
            private boolean initialized = false;

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

                // id -1, errorCode 2104, errorMsg Market data farm connection is OK:usfarm.us
                // id -1, errorCode 2104, errorMsg Market data farm connection is OK:usopt
                // id -1, errorCode 2104, errorMsg Market data farm connection is OK:usfarm
                // id -1, errorCode 2106, errorMsg HMDS data farm connection is OK:ushmds.us
                // id -1, errorCode 2106, errorMsg HMDS data farm connection is OK:ushmds
                if (id == -1 && errorCode == 2106 && errorMsg.endsWith("OK:ushmds")) {
                    if (!initialized) {
                        log.info("Data connection is active, executing post connect commands");

                        AsyncExec.execute(() -> {
                            for (AppCommand command : postConnectCommands) {
                                command.execute(ib, TraderApp.this);
                            }
                        });

                        initialized = true;
                    }
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
                log.info("connected, waiting for data connection status");
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
