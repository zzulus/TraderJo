package jo.app.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;
import jo.app.daynight.Megabar;
import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.ConnectionHandlerAdapter;
import jo.handler.IHistoricalDataHandler;
import jo.model.Bar;
import jo.recording.event.RealTimeBarEvent;
import jo.util.AsyncExec;
import jo.util.AsyncVal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GatherBarsForNightVsDayApp {
    protected final static Logger log = LogManager.getLogger(GatherBarsForNightVsDayApp.class);
    private IBService ib;
    private ObjectMapper objectMapper;

    public static void main(String[] args) throws InterruptedException {
        new GatherBarsForNightVsDayApp().run();
        System.out.println("All Done");

        System.exit(0);
    }

    private void run() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.ib = new IBService();

        Set<String> stockSymbols = getSymbols();
        AsyncVal<String> ex = new AsyncVal<>();

        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            @Override
            public void connected() {
                AsyncExec.execute(() -> {
                    ExecutorService pool = Executors.newFixedThreadPool(5);
                    try {
                        for (String symbol : stockSymbols) {
                            pool.submit(() -> gatherData(symbol));
                        }
                        pool.shutdown();
                        pool.awaitTermination(1, TimeUnit.HOURS);
                    } catch (InterruptedException e) {
                    }
                    ex.set("Done");
                });
            }
        });

        ex.get();
    }

    private void gatherData(String symbol) {
        Contract contract = Stocks.smartOf(symbol);
        System.out.println(contract.symbol());

        File folder = new File("TraderJo/historical/day-vs-night");
        folder.mkdirs();

        File file = new File(folder, symbol + ".json");
        if (file.exists() && file.length() > 0)
            return;

        try {
            Map<Long, Bar> tradesR = request(contract, WhatToShow.TRADES, true);
            Map<Long, Bar> tradesE = request(contract, WhatToShow.TRADES, false);
            Map<Long, Bar> bidR = request(contract, WhatToShow.BID, true);
            Map<Long, Bar> bidE = request(contract, WhatToShow.BID, false);
            Map<Long, Bar> askR = request(contract, WhatToShow.ASK, true);
            Map<Long, Bar> askE = request(contract, WhatToShow.ASK, false);
            Map<Long, Bar> midR = request(contract, WhatToShow.MIDPOINT, true);
            Map<Long, Bar> midE = request(contract, WhatToShow.MIDPOINT, false);

            Set<Long> keys = new TreeSet<>();
            keys.addAll(tradesR.keySet());
            keys.addAll(tradesE.keySet());
            keys.addAll(bidR.keySet());
            keys.addAll(bidE.keySet());
            keys.addAll(askR.keySet());
            keys.addAll(askE.keySet());
            keys.addAll(midR.keySet());
            keys.addAll(midE.keySet());

            try (PrintWriter ps = new PrintWriter(new FileOutputStream(file, false))) {
                for (Long key : keys) {
                    Megabar megabar = new Megabar(key, tradesR, tradesE, bidR, bidE, askR, askE, midR, midE);
                    try {
                        String str = objectMapper.writeValueAsString(megabar);
                        ps.println(str);
                    } catch (JsonProcessingException ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                }
                ps.flush();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println(contract.symbol() + " done");

        } catch (Exception e) {
            System.out.println(contract.symbol() + " failed");
            e.printStackTrace();
            return;
        }

    }

    private Set<String> getSymbols() {
        return new LinkedHashSet<String>() {{
            add("AAPL");
            add("MSFT");
            add("AMZN");
            add("TSLA");
            add("FB");
            add("GOOG");

//            add("SPY");
//            add("QQQ");
//            add("XLF");
//            add("SQQQ");
//            add("EEM");
//            add("GDX");
//            add("TQQQ");
//            add("SPXS");
//            add("EFA");
//            add("IWM");
//            add("VXX");
//            add("HYG");
//            add("XLE");
//            add("SPXU");
//            add("EWZ");
//            add("FXI");
//            add("VEA");
//            add("SLV");
//            add("IAU");
//            add("SPXL");
//            add("IEMG");
//            add("VWO");
//            add("SH");
//            add("XLU");
//            add("GDXJ");
//            add("SDS");
//            add("XLK");
//            add("IEFA");
//            add("LQD");
//            add("USO");
//            add("TNA");
//            add("XLI");
//            add("XLP");
//            add("UVXY");
//            add("UPRO");
//            add("XLV");
//            add("TLT");
//            add("GLD");
//            add("XOP");
//            add("SDOW");
//            add("EWJ");
//            add("XLRE");
//            add("SCHF");
//            add("KRE");
//            add("GOVT");
//            add("TZA");
//            add("QID");
//            add("IYR");
//            add("UCO");
//            add("BKLN");
//            add("JNK");
//            add("VNQ");
//            add("XLB");
//            add("IVV");
//            add("RSX");
//            add("SOXS");
//            add("EWH");
//            add("AGG");
//            add("IJR");
//            add("VOO");
//            add("PSQ");
//            add("VEU");
//            add("XBI");
//            add("AMLP");
//            add("UGAZ");
//            add("VTI");
//            add("PFF");
//            add("TVIX");
//            add("INDA");
//            add("DIA");
//            add("USMV");
//            add("SPLV");
//            add("UDOW");
//            add("VGK");
//            add("XRT");
//            add("EZU");
//            add("SHY");
//            add("XLY");
//            add("EWT");
//            add("FAS");
//            add("BND");
//            add("ACWI");
//            add("SVXY");
//            add("EWG");
//            add("VIXY");
//            add("IEF");
//            add("NUGT");
//            add("SPLG");
//            add("ERX");
//            add("BIL");
//            add("SJNK");
//            add("EWY");
//            add("SMH");
//            add("SPTS");
//            add("ASHR");
//            add("XLC");
//            add("SCO");
//            add("VXUS");
//            add("EMB");
//            add("LABD");
        }};
    }

    private Map<Long, Bar> request(Contract contract, WhatToShow whatToShow, boolean regularHoursOnly) {
        PumpAndDumpHandler h = new PumpAndDumpHandler();
        ib.reqHistoricalData(contract, "", 10, DurationUnit.YEAR, BarSize._1_day, whatToShow, regularHoursOnly, h);
        return h.async.get();
    }
}

class PumpAndDumpHandler implements IHistoricalDataHandler {
    AsyncVal<Map<Long, Bar>> async = new AsyncVal<>();
    Map<Long, Bar> bars = new HashMap<>();

    @Override
    public void historicalDataEnd() {
        async.set(bars);
    }

    @Override
    public void historicalData(Bar bar) {
        bars.put(bar.getTime(), bar);
    }
}

