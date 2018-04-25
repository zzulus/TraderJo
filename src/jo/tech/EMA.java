package jo.tech;

import javax.annotation.Nullable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

import jo.collection.TDoubleNakedArrayList;
import jo.model.BarType;
import jo.model.Bars;

public class EMA {
    private final int period;
    private final TDoubleNakedArrayList series;
    private int prevSize = -1;
    private int offset = 0;
    private Double value;
    private Core talib = new Core();

    public EMA(Bars bars, BarType type, int period, int offset) {
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

        int desiredRange = period * 4;
        int end = size - offset - 1;
        int start = Math.max(0, end - desiredRange);
        double[] out = new double[size]; // TODO Find right size

        MInteger begin = new MInteger();
        MInteger length = new MInteger();

        RetCode retCode = series.executeFunction((arr) -> talib.ema(start, end, arr, period, begin, length, out));
        if (retCode != RetCode.Success || length.value == 0)
            return null;

        value = out[length.value - 1];
        prevSize = size;

        return value;
    }

    public static void main(String[] args) {
        Core c = new Core();
        int period = 4;
        int emaLookback = c.emaLookback(period);
        double[] closePrice = new double[] { 1, 2, 43, 4, 35, 65, 7, 28, 69, 3, 10, 2, 3, 74, 25, 65, 17, 8, 9, 3, 1, 2, 43, 4, 35, 65, 7, 28, 69, 3, 10, 2, 3, 74, 25, 65, 17, 8, 9, 3 };
        int end = closePrice.length - 1;
        {
            double[] out = new double[closePrice.length];

            MInteger begin = new MInteger();
            MInteger length = new MInteger();

            c.ema(0, end, closePrice, period, begin, length, out);
            System.out.println("EMA 0-end: " + out[length.value - 1]);

            c.ema(end, end, closePrice, period, begin, length, out);
            System.out.println("EMA end-end: " + out[length.value - 1]);

            c.ema(end - emaLookback, end, closePrice, period, begin, length, out);
            System.out.println("EMA end-emaLookback: " + out[length.value - 1]);

            c.ema(end - period * 2, end, closePrice, period, begin, length, out);
            System.out.println("EMA end - period*2: " + out[length.value - 1]);

            c.ema(end - period * 3, end, closePrice, period, begin, length, out);
            System.out.println("EMA end - period*3: " + out[length.value - 1]);

            c.ema(end - period * 4, end, closePrice, period, begin, length, out);
            System.out.println("EMA end - period*4: " + out[length.value - 1]);

            System.out.println("emaLookback: " + emaLookback);
        }
    }
}
