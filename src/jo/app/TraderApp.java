package jo.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import jo.bot.Bot;
import jo.bot.RandomBot;
import jo.command.AppCommand;
import jo.command.InitStockDataCommand;
import jo.command.StartBotsCommand;
import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.IConnectionHandler;
import jo.model.MarketData;

public class TraderApp {
    private static final Logger log = LogManager.getLogger(TraderApp.class);
    private List<AppCommand> postConnectCommands = new ArrayList<>();
    private IBService ib;
    // Key - Stock name, e.g. SPY
    private Map<String, MarketData> stockMarketDataMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new TraderApp().start();
    }

    public TraderApp() {
        Bot bot1 = new RandomBot(Stocks.TQQQ(true), 50, 0.5d);
        // Bot bot2 = new RandomBot(TQQQ_SMART(), 50, 0.10d);
        // Bot bot3 = new BelowSimpleAverageBot(TQQQ_SMART(), 50, /* periodSeconds */ 90, /* belowAverageVal */ 0.03, 0.10);
        // Bot bot4 = new BelowSimpleAverageBot(TQQQ_SMART(), 50, /* periodSeconds */ 900, /* belowAverageVal */ 0.20, 0.20);

        List<Bot> bots = Lists.newArrayList(bot1);

        postConnectCommands = Lists.newArrayList(
                new InitStockDataCommand(Stocks.TQQQ(true)),
                new StartBotsCommand(bots));
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

    public IBService getIb() {
        return ib;
    }

    public Map<String, MarketData> getStockMarketDataMap() {
        return stockMarketDataMap;
    }

    public MarketData getStockMarketData(String symbol) {
        return stockMarketDataMap.get(symbol);
    }

    public void setPostConnectCommands(List<AppCommand> postConnectCommands) {
        this.postConnectCommands = postConnectCommands;
    }
}
