package jo.tech;

import javax.annotation.Nullable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

import gnu.trove.list.TDoubleList;
import jo.model.BarType;
import jo.model.Bars;

public class EMA {
    private final int period;
    private final TDoubleList series;
    private int prevSize = -1;
    private int offset = 0;
    private Double value;
    private Core talib;

    public EMA(Bars bars, BarType type, int period, int offset) {
        this.series = bars.getDoubleSeries(type);
        this.period = period;
        this.offset = offset;
        this.talib = new Core();
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

    public static void main(String[] args) {
        Core c = new Core();
        int period = 4;
        int emaLookback = c.emaLookback(period);
        double[] closePrice = new double[] { 1, 2, 43, 4, 35, 65, 7, 28, 69, 3, 10, 2, 3, 74, 25, 65, 17, 8, 9, 3 };
        int end = closePrice.length - 1;
        {
            double[] out = new double[closePrice.length];

            MInteger begin = new MInteger();
            MInteger length = new MInteger();

            RetCode retCode2 = c.ema(0, end, closePrice, period, begin, length, out);
            System.out.println("EMA: " + out[length.value - 1]);

            RetCode retCode3 = c.ema(end, end, closePrice, period, begin, length, out);
            System.out.println("EMA: " + out[length.value - 1]);

            RetCode retCode4 = c.ema(end - emaLookback, end, closePrice, period, begin, length, out);
            System.out.println("EMA: " + out[length.value - 1]);

            RetCode retCode5 = c.ema(end - period * 2, end, closePrice, period, begin, length, out);
            System.out.println("EMA: " + out[length.value - 1]);

            System.out.println("emaLookback: " + emaLookback);
        }
    }
}
