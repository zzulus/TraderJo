package jo.position;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.Uninterruptibles;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

import gnu.trove.list.TDoubleList;
import jo.controller.IBroker;
import jo.handler.IHistoricalDataHandler;
import jo.model.Bar;
import jo.model.Bars;
import jo.model.IApp;
import jo.model.MarketData;
import jo.util.AsyncExec;
import jo.util.AsyncVal;
import jo.util.Formats;

// TODO Extract base class BarTrailAmountStrategy
// TODO Use data for previous days
public class HistoricalHighLowAvgTrailAmountStrategy implements TrailAmountStrategy {
    protected final Logger log = LogManager.getLogger(this.getClass());
    private final double extra;
    private Double value;
    private final int periodDays;
    private final BarSize barSize;
    private final Contract contract;

    public HistoricalHighLowAvgTrailAmountStrategy(BarSize barSize, int periodDays, double extra, Contract contract) {
        this.barSize = barSize;
        this.periodDays = periodDays;
        this.extra = extra;
        this.contract = contract;
    }

    @Override
    public Double getTrailAmount(MarketData md) {
        return value;
    }

    @Override
    public void init(IBroker ib, IApp app) {
        AsyncVal<Bars> barsExchange = AsyncVal.create();
        log.info("Calculating historical trailing amount for " + contract.symbol());
        //AsyncExec.execute(() -> {
            ib.reqHistoricalData(contract, "20180425 23:59:59 GMT", periodDays, DurationUnit.DAY, barSize, WhatToShow.TRADES, true, new IHistoricalDataHandler() {
                private final Bars bars = new Bars();

                @Override
                public void historicalDataEnd() {
                    log.info("historicalDataEnd " + contract.symbol());
                    barsExchange.complete(bars);
                }

                @Override
                public void historicalData(Bar bar, boolean hasGaps) {
                    log.info("addBar " + contract.symbol());
                    bars.addBar(bar);
                }
            });
       // });

        Bars bars = barsExchange.get();

        TDoubleList low = bars.getLow();
        TDoubleList high = bars.getHigh();
        int size = bars.getSize();
        if (size == 0) {
            throw new RuntimeException("reqHistoricalData returned 0 bars");
        }

        log.info("Received {} bars", size);

        double[] hiLoDiffs = new double[size];
        for (int i = 0; i < size; i++) {
            double hiLoDiff = high.get(i) - low.get(i);
            hiLoDiffs[i] = hiLoDiff;
        }

        double[] out = new double[1];
        MInteger beginOut = new MInteger();
        MInteger lengthOut = new MInteger();

        int smaPeriod = hiLoDiffs.length;

        Core talib = new Core();
        RetCode retCode = talib.sma(smaPeriod - 1, smaPeriod - 1, hiLoDiffs, smaPeriod, beginOut, lengthOut, out);
        if (retCode != RetCode.Success) {
            throw new RuntimeException("Failed to calculate historical trailing amount for " + contract.symbol());
        }

        value = out[0] + extra;
        log.info("Trailing amount is " + Formats.fmt(value));
    }
}
