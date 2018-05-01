package jo.app;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.IConnectionHandler;
import jo.recording.MarketRecorder;
import jo.recording.Recorder;

public class MarketRecorderApp {
    private static final Logger log = LogManager.getLogger(MarketRecorderApp.class);
    private IBService ib;

    public static void main(String[] args) {
        new MarketRecorderApp().start();
    }

    public void start() {
        List<Recorder> records = new ArrayList<>();
        records.add(new MarketRecorder(Stocks.AAPL(true)).withDeepBook(true));
        records.add(new MarketRecorder(Stocks.TQQQ(true)).withDeepBook(true));
        records.add(new MarketRecorder(Stocks.QQQ(true)).withDeepBook(true));

        for (String stock : MarketRecorderStocks.TICKS_ONLY_ETFS) {
            records.add(new MarketRecorder(Stocks.of(stock, true)).withDeepBook(false));
        }

        for (String stock : MarketRecorderStocks.TICKS_ONLY_STOCKS) {
            records.add(new MarketRecorder(Stocks.of(stock, true)).withDeepBook(false));
        }

        ib = new IBService();
        ib.setClientId(0);

        IConnectionHandler connHandler = new IConnectionHandler() {

            @Override
            public void show(String string) {
                log.info("show: " + string);
            }

            @Override
            public void message(int id, int errorCode, String errorMsg) {
                log.info("message: id {}, errorCode {}, errorMsg {}", id, errorCode, errorMsg);
                if (errorMsg.contains("Connectivity between IB and Trader Workstation has been lost")) {
                    for (Recorder recorder : records) {
                        recorder.stop();
                    }
                    System.exit(0);
                }

                for (Recorder recorder : records) {
                    recorder.error(id, errorCode, errorMsg);
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
                log.info("connected, starting recorders");

                for (Recorder recorder : records) {
                    recorder.start(ib);
                    try {
                        Thread.sleep(1000); // avoid throttling
                    } catch (InterruptedException e) {
                    }
                }
            }

            @Override
            public void accountList(List<String> accounts) {
            }
        };

        ib.connectLocalhostLive(connHandler);
    }

}
