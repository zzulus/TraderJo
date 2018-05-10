package jo.tech;

import javax.annotation.Nullable;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jo.model.BarType;
import jo.model.Bars;

public class ROC {
    private final int period;
    private final TDoubleList series;
    private int prevSize = -1;
    private int offset = 0;
    private Double value;

    public ROC(Bars bars, BarType type, int period, int offset) {
        this(bars.getDoubleSeries(type), period, offset);
    }

    public ROC(TDoubleList series, int period, int offset) {
        this.series = series;
        this.period = period;
        this.offset = offset;
    }

    @Nullable
    public Double get() {
        int size = series.size();
        if (size < period + offset + 1) {
            return null;
        }

        // do some caching
        if (prevSize == size) {
            return value;
        }

        int lastPriceIdx = size - offset - 1;
        int nPeriodsAgoIdx = lastPriceIdx - period;

        double lastPrice = series.get(lastPriceIdx);
        double nPeriodsAgoPrice = series.get(nPeriodsAgoIdx);

        value = (lastPrice - nPeriodsAgoPrice) / nPeriodsAgoPrice * 100d;
        prevSize = size;

        return value;
    }
}
