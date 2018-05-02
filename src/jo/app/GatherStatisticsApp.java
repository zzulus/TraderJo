package jo.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;

import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.ConnectionHandlerAdapter;
import jo.handler.IHistoricalDataHandler;
import jo.model.Bar;
import jo.recording.event.RealTimeBarEvent;
import jo.util.AsyncExec;
import jo.util.AsyncVal;

public class GatherStatisticsApp {
    protected final static Logger log = LogManager.getLogger(GatherStatisticsApp.class);

    public static void main(String[] args) throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        Set<String> stockSymbols = getSymbols();
        IBService ib = new IBService();

        AsyncVal<String> ex = new AsyncVal<>();

        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            @Override
            public void connected() {
                AsyncExec.execute(() -> {
                    for (String symbol : stockSymbols) {
                        gatherData(objectMapper, ib, symbol);
                    }
                    ex.set("Done");
                });
            }
        });

        ex.get();
        System.out.println("All Done");

        System.exit(0);
    }

    private static void gatherData(ObjectMapper objectMapper, IBService ib, String symbol) {
        Contract contract = Stocks.smartOf(symbol);
        System.out.println(contract.symbol());

        File folder = new File("D:\\autobot\\TraderJo\\historical\\2018-05-01-1m-20d");
        folder.mkdirs();

        File file = new File(folder, symbol + ".log");
        if (file.exists() && file.length() > 0)
            return;

        AsyncVal<String> ex = new AsyncVal<>();

        try (PrintWriter ps = new PrintWriter(new FileOutputStream(file, true))) {
            ib.reqHistoricalData(contract, "", 20, DurationUnit.DAY, BarSize._1_min, WhatToShow.TRADES, true, new IHistoricalDataHandler() {
                @Override
                public void historicalDataEnd() {
                    ex.set(symbol);
                    System.out.println(symbol + " end");
                }

                @Override
                public void historicalData(Bar bar) {
                    try {
                        RealTimeBarEvent e = new RealTimeBarEvent(bar);
                        String str = objectMapper.writeValueAsString(e);
                        ps.println(str);
                    } catch (JsonProcessingException ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                }
            });

            ex.get();
            ps.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> getSymbols() {
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

        stockSymbols.addAll(MyStocks.TICKS_ONLY_STOCKS);
        stockSymbols.addAll(MyStocks.TICKS_ONLY_ETFS);
        return stockSymbols;
    }
}
