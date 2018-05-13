package jo.app;

import static jo.util.PriceUtils.fixPriceVariance;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.bag.TreeBag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;

import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.ConnectionHandlerAdapter;
import jo.handler.IHistoricalDataHandler;
import jo.model.Bar;
import jo.model.BarType;
import jo.model.Bars;
import jo.tech.ChangeList;
import jo.tech.EMA;
import jo.tech.BarsPctChange;
import jo.util.AsyncVal;
import jo.util.Formats;
import jo.util.NullUtils;

public class TestBotEntryApp {
    protected Logger log = LogManager.getLogger(this.getClass());
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");
    private static final DecimalFormat CHANGE_FMT = new DecimalFormat("#,##0.00000");

    private Contract contract = Stocks.smartOf("BABA");
    private int period = 18;
    private Bars srcBars;
    private Bars maBars;
    private EMA maEdge0;
    private EMA maEdge1;
    private EMA maEdge2;

    private BarsPctChange changeO0;
    private BarsPctChange changeO1;
    private BarsPctChange changeO2;
    private BarsPctChange changeC0;
    private BarsPctChange changeC1;
    private BarsPctChange changeC2;
    private ChangeList ocChanges;

    int trades = 0;

    public static void main(String[] args) throws InterruptedException {
        TestBotEntryApp app = new TestBotEntryApp();
        app.init();

        System.out.println();

        app.run();

        System.out.println("Trades " + app.trades);
    }

    private void run() {
        int size = srcBars.getSize();
        for (int i = 1; i < size; i++) {
            Bar bar = srcBars.getBarFromEnd(size - i);
            maBars.addBar(bar);
            mayBeOpenPosition();
        }
    }

    private void mayBeOpenPosition() {
        int barSize = maBars.getSize();
        if (barSize < period + 3)
            return;

        Double maEdgeVal0 = fixPriceVariance(maEdge0.get());
        Double maEdgeVal1 = fixPriceVariance(maEdge1.get());
        Double maEdgeVal2 = fixPriceVariance(maEdge2.get());

        double barLow0 = maBars.getLastBar(BarType.LOW, 0);
        double barHigh0 = maBars.getLastBar(BarType.HIGH, 0);

        if (NullUtils.anyNull(maEdgeVal0, maEdgeVal1, maEdgeVal2))
            return;

        Bar lastBar = maBars.getLastBar();
        String timeStr = TIME_FMT.format((lastBar.getTime() + TimeUnit.HOURS.toSeconds(3)) * 1000);

        double maEdgeValChange1 = BarsPctChange.of(maEdgeVal2, maEdgeVal1);
        double maEdgeValChange0 = BarsPctChange.of(maEdgeVal1, maEdgeVal0);

        //        boolean maEdgeGoingUp = maEdgeValChange1 > 0.0005 && maEdgeValChange0 > 0.0005;
        //        boolean maEdgeGoingDown = maEdgeValChange1 < -0.0005 && maEdgeValChange0 < -0.0005;

        boolean maEdgeGoingUp = maEdgeValChange1 > 0 && maEdgeValChange0 > 0;
        boolean maEdgeGoingDown = maEdgeValChange1 < 0 && maEdgeValChange0 < 0;

        boolean openLong = maEdgeGoingUp
                && barLow0 - maEdgeVal0 > 0.02
        //&& ocChanges.allPositive()
        ;

        boolean openShort = maEdgeGoingDown
                && barHigh0 - maEdgeVal0 < -0.02
        //&& ocChanges.allNegative()
        ;

        if (openLong) {
            String x = "  | " + CHANGE_FMT.format(maEdgeValChange1) + " " + CHANGE_FMT.format(maEdgeValChange0);
            System.out.println("Long @ " + timeStr + x);
            trades++;
        }

        if (openShort) {
            String x = "  | " + CHANGE_FMT.format(maEdgeValChange1) + " " + CHANGE_FMT.format(maEdgeValChange0);
            System.out.println("Short @ " + timeStr + x);
            trades++;
        }
    }

    private void init() {
        this.srcBars = getBars(contract);

        this.maBars = new Bars();

        this.maEdge0 = new EMA(maBars, BarType.CLOSE, period, 0);
        this.maEdge1 = new EMA(maBars, BarType.CLOSE, period, 1);
        this.maEdge2 = new EMA(maBars, BarType.CLOSE, period, 2);

        this.changeO0 = new BarsPctChange(maBars, BarType.OPEN, 0);
        this.changeO1 = new BarsPctChange(maBars, BarType.OPEN, 1);
        this.changeO2 = new BarsPctChange(maBars, BarType.OPEN, 2);
        this.changeC0 = new BarsPctChange(maBars, BarType.CLOSE, 0);
        this.changeC1 = new BarsPctChange(maBars, BarType.CLOSE, 1);
        this.changeC2 = new BarsPctChange(maBars, BarType.CLOSE, 2);

        this.ocChanges = ChangeList.of(changeO0, changeO1, changeO2, changeC0, changeC1, changeC2);
    }

    private Bars getBars(Contract contract) {
        IBService ib = new IBService();
        AsyncVal<Bars> barsEx = new AsyncVal<>();

        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            @Override
            public void connected() {
                ib.reqHistoricalData(contract, "", 1, DurationUnit.DAY, BarSize._1_min, WhatToShow.TRADES, true, new IHistoricalDataHandler() {
                    final Bars bars = new Bars();

                    @Override
                    public void historicalDataEnd() {
                        barsEx.set(bars);
                    }

                    @Override
                    public void historicalData(Bar bar) {
                        bars.addBar(bar);
                    }
                });
            }
        });

        Bars bars = barsEx.get();

        try {
            ib.disconnect();
        } catch (Exception e) {
            // ignore
        }
        return bars;
    }
}
