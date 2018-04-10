package jo.tech;

import javax.annotation.Nullable;

import gnu.trove.list.array.TDoubleArrayList;
import jo.model.BarType;
import jo.model.Bars;

public class SMA {
    private final int period;
    private final TDoubleArrayList series;
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

}
