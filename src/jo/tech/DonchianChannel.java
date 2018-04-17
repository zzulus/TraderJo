package jo.tech;

import javax.annotation.Nullable;

import gnu.trove.list.TDoubleList;
import jo.model.BarType;
import jo.model.Bars;

public class DonchianChannel {
    private final Bars bars;
    private final TDoubleList upper;
    private final TDoubleList lower;
    private final int upperPeriod;
    private final int lowerPeriod;
    private Channel channel;
    private int prevBarSize = -1;
    private int offset = 0;

    public DonchianChannel(Bars bars, BarType lowerType, BarType upperType, int lowerPeriod, int upperPeriod) {
        this.bars = bars;
        this.lower = bars.getDoubleSeries(lowerType);
        this.upper = bars.getDoubleSeries(upperType);
        this.lowerPeriod = lowerPeriod;
        this.upperPeriod = upperPeriod;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Nullable
    public Channel get() {
        int barSize = bars.getSize();
        if (barSize < upperPeriod + offset || barSize < lowerPeriod + offset) {
            return null;
        }

        // do some caching
        if (prevBarSize == barSize) {
            return channel;
        }

        double upperBound = Double.MIN_VALUE;
        int upperBegin = barSize - upperPeriod - offset;
        int upperEnd = barSize - offset;
        for (int i = upperBegin; i < upperEnd; i++) {
            upperBound = Math.max(upperBound, upper.get(i));
        }

        double lowerBound = Double.MAX_VALUE;
        int lowerBegin = barSize - lowerPeriod - offset;
        int lowerEnd = barSize - offset;        
        for (int i = lowerBegin; i < lowerEnd; i++) {
            lowerBound = Math.min(lowerBound, lower.get(i));
        }

        prevBarSize = barSize;
        channel = new Channel(lowerBound, upperBound);

        return channel;
    }
}
