package jo.tech;

import javax.annotation.Nullable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

import gnu.trove.list.TDoubleList;
import jo.collection.TDoubleNakedArrayList;
import jo.model.BarType;
import jo.model.Bars;

public class SMA {
    private final int period;
    private final TDoubleNakedArrayList series;
    private int prevSize = -1;
    private int offset = 0;
    private Double value;
    private Core talib;

    public SMA(Bars bars, BarType type, int period, int offset) {
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

        int end = size - offset - 1;

        double[] out = new double[1];
        MInteger beginOut = new MInteger();
        MInteger lengthOut = new MInteger();

        RetCode retCode = series.executeFunction((arr) -> talib.sma(end, end, arr, period, beginOut, lengthOut, out));
        if (retCode != RetCode.Success) {
            return null;
        }

        value = out[0];
        prevSize = size;

        return value;
    }

    public static double of(TDoubleList series) {
        Core talib = new Core();
        int size = series.size();
        int end = size - 1;
        double[] arr = series.toArray();
        double[] out = new double[1];

        MInteger beginOut = new MInteger();
        MInteger lengthOut = new MInteger();

        RetCode retCode = talib.sma(end, end, arr, size, beginOut, lengthOut, out);
        if (retCode != RetCode.Success) {
            throw new RuntimeException(retCode.toString());
        }

        return out[0];
    }

    public static void main(String[] args) {
        TDoubleNakedArrayList series = new TDoubleNakedArrayList();
        series.add(0);
        series.add(1);
        series.add(20);

        System.out.println(of(series));
    }
}
