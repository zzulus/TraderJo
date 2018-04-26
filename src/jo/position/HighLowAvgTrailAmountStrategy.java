package jo.position;

import com.ib.client.Types.BarSize;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

import jo.controller.IBroker;
import jo.model.Bars;
import jo.model.IApp;
import jo.model.MarketData;

// TODO Extract base class BarTrailAmountStrategy
// TODO Use data for previous days
public class HighLowAvgTrailAmountStrategy implements TrailAmountStrategy {
    private final int period;
    private BarSize barSize;
    private Bars bars;
    private int prevBarsSize = -1;
    private Double value;
    private double extra;
    private Core talib = new Core();

    public static HighLowAvgTrailAmountStrategy createDefault() {
        return new HighLowAvgTrailAmountStrategy(BarSize._1_min, 60, 0);
    }

    public HighLowAvgTrailAmountStrategy(BarSize barSize, int period, double extra) {
        this.period = period;
        this.barSize = barSize;
        this.extra = extra;
    }

    public HighLowAvgTrailAmountStrategy(Bars bars, int period, double extra) {
        this.period = period;
        this.bars = bars;
        this.extra = extra;
    }

    @Override
    public Double getTrailAmount(MarketData md) {
        if (bars == null) {
            bars = md.getBars(barSize);
        }

        int barsSize = bars.getSize();
        if (barsSize < 2) { // at least 2 bars for simple average
            return null;
        }

        if (barsSize == prevBarsSize) {
            return value;
        }

        int end = barsSize;
        int begin = Math.max(end - period, 0);

        double[] hiLoDiffs = new double[end - begin];
        for (int i = begin; i < end; i++) {
            double high = bars.getHigh().get(i);
            double low = bars.getLow().get(i);
            hiLoDiffs[i - begin] = Math.abs(high - low);
        }

        double[] out = new double[1];
        MInteger beginOut = new MInteger();
        MInteger lengthOut = new MInteger();

        int smaPeriod = hiLoDiffs.length;

        RetCode retCode = talib.sma(0, smaPeriod - 1, hiLoDiffs, smaPeriod, beginOut, lengthOut, out);
        if (retCode != RetCode.Success) {
            return null;
        }

        value = out[0] + extra;
        prevBarsSize = barsSize;

        return value;
    }

    @Override
    public void init(IBroker ib, IApp app) {
    }

}
