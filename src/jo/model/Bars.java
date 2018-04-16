package jo.model;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import jo.util.SyncSignal;

public class Bars {
    private static final Logger log = LogManager.getLogger(Bars.class);
    private int size = 0;
    private final SyncSignal signal = new SyncSignal();

    private final TLongArrayList time = new TLongArrayList();
    private final TDoubleArrayList high = new TDoubleArrayList();
    private final TDoubleArrayList low = new TDoubleArrayList();
    private final TDoubleArrayList open = new TDoubleArrayList();
    private final TDoubleArrayList close = new TDoubleArrayList();
    private final TDoubleArrayList wap = new TDoubleArrayList();
    private final TLongArrayList volume = new TLongArrayList();
    private final TIntArrayList count = new TIntArrayList();

    public Bar getLastBar() {
        Preconditions.checkArgument(size > 0, "No data yet");
        return getBar(0);
    }

    public Bar getBar(int offsetFromEnd) {
        Preconditions.checkArgument(size > 0, "No data yet");

        int offset = size - offsetFromEnd - 1;
        Preconditions.checkArgument(offset > -1, "Index out of bound: lastPos %s, offsetFromEnd %s", size, offsetFromEnd);

        Bar bar = new Bar();
        bar.setTime(time.get(offset));
        bar.setHigh(high.get(offset));
        bar.setLow(low.get(offset));
        bar.setOpen(open.get(offset));
        bar.setClose(close.get(offset));
        bar.setWap(wap.get(offset));
        bar.setVolume(volume.get(offset));
        bar.setCount(count.get(offset));

        return bar;
    }

    public synchronized void addBar(Bar bar) {
        // log.info("AddBar: {}", bar);

        time.add(bar.getTime());
        high.add(bar.getHigh());
        low.add(bar.getLow());
        open.add(bar.getOpen());
        close.add(bar.getClose());
        wap.add(bar.getWap());
        volume.add(bar.getVolume());
        count.add(bar.getCount());

        size++;
    }

    @Nullable
    public Double getAverageClose(int period) {
        int size = close.size();
        if (size < period) {
            return null;
        }

        double acc = 0;
        for (int i = 0; i < period; i++) {
            acc = acc + close.get(size - i);
        }

        return acc / period;
    }

    public TLongArrayList getTime() {
        return time;
    }

    public TDoubleArrayList getHigh() {
        return high;
    }

    public TDoubleArrayList getLow() {
        return low;
    }

    public TDoubleArrayList getOpen() {
        return open;
    }

    public TDoubleArrayList getClose() {
        return close;
    }

    public TDoubleArrayList getWap() {
        return wap;
    }

    public TLongArrayList getVolume() {
        return volume;
    }

    public TIntArrayList getCount() {
        return count;
    }

    public int getSize() {
        return size;
    }

    public SyncSignal getSignal() {
        return signal;
    }

    public TDoubleArrayList getDoubleSeries(BarType type) {
        TDoubleArrayList arr;
        switch (type) {
        case OPEN:
            arr = open;
            break;

        case LOW:
            arr = low;
            break;

        case HIGH:
            arr = high;
            break;

        case CLOSE:
            arr = close;
            break;

        case WAP:
            arr = wap;
            break;

        default:
            throw new IllegalArgumentException("Unsupported BarType");
        }

        return arr;
    }

    public double getLastBar(BarType type) {
        return getLastBar(type, 0);
    }

    public double getLastBar(BarType type, int shift) {
        TDoubleArrayList series = getDoubleSeries(type);
        int offset = size - 1 - shift;
        return series.get(offset);
    }

}
