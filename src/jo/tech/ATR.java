package jo.tech;

import javax.annotation.Nullable;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

import jo.collection.TDoubleNakedArrayList;
import jo.model.Bar;
import jo.model.BarType;
import jo.model.Bars;

public class ATR {
    private final int period;
    private final TDoubleNakedArrayList highs;
    private final TDoubleNakedArrayList lows;
    private final TDoubleNakedArrayList close;

    private int prevSize = -1;
    private int offset = 0;
    private Double value;
    private Core talib = new Core();

    public ATR(Bars bars, int period, int offset) {
        this.highs = bars.getDoubleSeries(BarType.HIGH);
        this.lows = bars.getDoubleSeries(BarType.LOW);
        this.close = bars.getDoubleSeries(BarType.CLOSE);
        this.period = period;
        this.offset = offset;
    }

    @Nullable
    public Double get() {
        int size = highs.size();
        if (size < period + offset) {
            return null;
        }

        // do some caching
        if (prevSize == size) {
            return value;
        }

        int end = size - offset - 1;
        int start = end - period;
        double[] out = new double[size]; // TODO Find right size

        MInteger begin = new MInteger();
        MInteger length = new MInteger();

        double[] highsArr = highs.toArray();
        double[] lowsArr = lows.toArray();
        double[] closeArr = close.toArray();

        RetCode retCode = talib.atr(start, end, highsArr, lowsArr, closeArr, period, begin, length, out);
        if (retCode != RetCode.Success || length.value == 0)
            return null;

        value = out[length.value - 1];
        prevSize = size;

        return value;
    }

    public static void main(String[] args) {
        System.out.println(BarsPctChange.of(1, 2));
    }
}