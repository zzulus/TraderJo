package jo.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.ib.client.Contract;

import jo.bot.Bot;
import jo.bot.DonchianBot;
import jo.command.AppCommand;
import jo.command.InitStockDataCommand;
import jo.command.StartBotsCommand;
import jo.constant.Stocks;
import jo.controller.IBService;
import jo.controller.IBroker;
import jo.handler.IConnectionHandler;
import jo.model.MarketData;

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
        
        Bot spyBot = new DonchianBot(spy, 25, 0.27);
        Bot xleBot = new DonchianBot(xle , 50, 0.12);
        Bot xlbBot = new DonchianBot(xlb , 50, 0.12);
        Bot ibbBot = new DonchianBot(ibb , 25, 0.2);
        Bot xluBot = new DonchianBot(xlu , 50, 0.13);
        
        List<Bot> bots = Lists.newArrayList(spyBot/*,
         xleBot, 
         xlbBot, 
         ibbBot, 
         xluBot*/);

        postConnectCommands = Lists.newArrayList(
                new InitStockDataCommand(spy),
                new InitStockDataCommand(xle),
                new InitStockDataCommand(xlb),
                new InitStockDataCommand(ibb),
                new InitStockDataCommand(xlu),                
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
}

