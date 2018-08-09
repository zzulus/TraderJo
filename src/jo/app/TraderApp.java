package jo.app;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;

import jo.bot.Bot;
import jo.bot.MovingAverageAtrLimitBot;
import jo.command.AppCommand;
import jo.command.GetNextOrderIdCommand;
import jo.command.StartBotsCommand;
import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.IConnectionHandler;
import jo.model.Context;
import jo.model.DefaultContext;
import jo.position.DollarValueWithRiskPositionSizeStrategy;
import jo.position.FixedDollarValuePositionSizeStrategy;
import jo.position.PositionSizeStrategy;
import jo.util.AsyncExec;

public class TraderApp {
    private static final Logger log = LogManager.getLogger(TraderApp.class);
    private List<AppCommand> postConnectCommands = new ArrayList<>();

    public static void main(String[] args) {
        new TraderApp().start();
    }

    public TraderApp() {
        PositionSizeStrategy positionSizeStrategy = new DollarValueWithRiskPositionSizeStrategy(500, 1.0);
        positionSizeStrategy = new FixedDollarValuePositionSizeStrategy(1000);

        Set<String> stockSymbols = new LinkedHashSet<>();
        stockSymbols.addAll(MyStocks.EARNINGS_STOCKS);
        stockSymbols.addAll(MyStocks.STOCKS_TO_TRADE);
        stockSymbols.addAll(MyStocks.TICKS_ONLY_ETFS);

        List<Bot> bots = new ArrayList<>();
        for (String stockSymbol : stockSymbols) {
            Contract contract = Stocks.smartOf(stockSymbol);
            MovingAverageAtrLimitBot bot = new MovingAverageAtrLimitBot(contract, positionSizeStrategy);
            bots.add(bot);
        }

        postConnectCommands = new ArrayList<>();
        postConnectCommands.add(new GetNextOrderIdCommand());
        postConnectCommands.add(new StartBotsCommand(bots));
    }

    public void start() {
        IBService ib = new IBService();
        Context ctx = new DefaultContext(ib);

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
                                command.execute(ctx);
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

    public void setPostConnectCommands(List<AppCommand> postConnectCommands) {
        this.postConnectCommands = postConnectCommands;
    }
}
