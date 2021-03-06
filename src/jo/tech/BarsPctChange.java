package jo.tech;

import gnu.trove.list.TDoubleList;
import jo.model.BarType;
import jo.model.Bars;

public class BarsPctChange implements Change {
    private final TDoubleList series;
    private int offset;

    public BarsPctChange(Bars bars, BarType barType, int offset) {
        this.series = bars.getDoubleSeries(barType);
        this.offset = offset;
    }

    @Override
    public Double getChange() {
        int size = series.size();
        if (size < 2 + offset)
            return null;

        double past = series.get(size - 2 - offset);
        double current = series.get(size - 1 - offset);
        double diff = current - past;

        return diff / past;
    }

    public static double of(double past, double current) {
        return (current - past) / past;
    }    
}
