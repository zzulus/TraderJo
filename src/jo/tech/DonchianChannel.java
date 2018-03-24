package jo.tech;

import javax.annotation.Nullable;

import gnu.trove.list.array.TDoubleArrayList;
import jo.model.Bars;

public class DonchianChannel {
    private final Bars bars;
    private final TDoubleArrayList high;
    private final TDoubleArrayList low;
    private final int upperPeriod;
    private final int lowerPeriod;
    private Channel channel;
    private int prevBarSize = -1;

    public DonchianChannel(Bars bars, int lowerPeriod, int upperPeriod) {
        this.bars = bars;
        this.low = bars.getLow();
        this.high = bars.getHigh();
        this.lowerPeriod = lowerPeriod;
        this.upperPeriod = upperPeriod;
    }

    @Nullable
    public Channel get() {
        int barSize = bars.getSize();
        if (barSize < upperPeriod || barSize < lowerPeriod) {
            return null;
        }

        // do some caching
        if (prevBarSize == barSize) {
            return channel;
        }

        double upperBound = Double.MIN_VALUE;
        int upperBegin = barSize - upperPeriod;
        int upperEnd = barSize;
        for (int i = upperBegin; i < upperEnd; i++) {
            upperBound = Math.max(upperBound, high.get(i));
        }

        double lowerBound = Double.MAX_VALUE;
        int lowerBegin = barSize - lowerPeriod;
        int lowerEnd = barSize;
        for (int i = lowerBegin; i < lowerEnd; i++) {
            lowerBound = Math.min(lowerBound, low.get(i));
        }

        prevBarSize = barSize;
        channel = new Channel(upperBound, lowerBound);

        return channel;
    }
}
