package jo.tech;

import java.text.DecimalFormat;
import java.text.Format;

import gnu.trove.list.TDoubleList;
import jo.model.BarType;
import jo.model.Bars;

public class PctChange implements Change {
    private final TDoubleList series;
    private int offset;

    public PctChange(Bars bars, BarType barType, int offset) {
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
    
    public static void main(String[] args) {
        Format FMT2 = new DecimalFormat( "#,##0.0000");
        System.out.println(FMT2.format(of(31.46, 31.44)));
    }
}
