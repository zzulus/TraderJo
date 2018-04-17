package jo.tech;

import javax.annotation.Nullable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

import gnu.trove.list.TDoubleList;
import jo.model.BarType;
import jo.model.Bars;

public class SMA {
    private final int period;
    private final TDoubleList series;
    private int prevSize = -1;
    private int offset = 0;
    private Double value;

    public SMA(Bars bars, BarType type, int period, int offset) {
        this.series = bars.getDoubleSeries(type);
        this.period = period;
        this.offset = offset;
    }

    @Nullable
    public Double get() {
        int size = series.size();
        if (size < period + offset) {
            return null;
        }

        // do some caching
        if (prevSize == size) {
            return value;
        }

        int from = size - period - offset;
        int to = from + period;
        double acc = 0;

        for (int i = from; i < to; i++) {
            acc = acc + series.get(i);
        }

        value = acc / period;
        prevSize = size;

        return value;
    }

    public Double gget() {
        Core c = new Core();
        //        c.ema(startIdx, endIdx, inReal, optInTimePeriod, outBegIdx, outNBElement, outReal)
        //        
        //        
        //        double[] closePrice = new double[TOTAL_PERIODS];
        //        double[] out = new double[TOTAL_PERIODS];
        //        MInteger begin = new MInteger();
        //        MInteger length = new MInteger();
        //
        //        for (int i = 0; i < closePrice.length; i++) {
        //            closePrice[i] = (double) i;
        //        }
        //
        //        RetCode retCode = c.sma(0, closePrice.length - 1, closePrice, PERIODS_AVERAGE, begin, length, out);
        return null;
    }

    public static void main(String[] args) {
        {
            Core c = new Core();
            double[] closePrice = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 3 };
            double[] out = new double[1];

            MInteger begin = new MInteger();
            MInteger length = new MInteger();

            int end = closePrice.length - 1;
            int period = 4;
            RetCode retCode = c.sma(end, end, closePrice, period, begin, length, out);
            System.out.println(retCode);
            System.out.println(out[0]);
        }

        {
            Core c = new Core();
            double[] closePrice = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 3 };
            double[] out = new double[closePrice.length];

            MInteger begin = new MInteger();
            MInteger length = new MInteger();

            int end = closePrice.length - 1;
            int period = 4;

            RetCode retCode2 = c.ema(0, end, closePrice, period, begin, length, out);
            System.out.println(retCode2);
            System.out.println(out[0]);
        }
    }

}
