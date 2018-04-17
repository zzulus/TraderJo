package jo.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import jo.util.SyncSignal;

public class Bars {
    private static final Logger log = LogManager.getLogger(Bars.class);
    private int size = 0;
    private final SyncSignal signal = new SyncSignal();

    private final TLongList time = new TLongArrayList();
    private final TDoubleList high = new TDoubleArrayList();
    private final TDoubleList low = new TDoubleArrayList();
    private final TDoubleList open = new TDoubleArrayList();
    private final TDoubleList close = new TDoubleArrayList();
    private final TDoubleList wap = new TDoubleArrayList();
    private final TLongList volume = new TLongArrayList();
    private final TIntList count = new TIntArrayList();

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

        signal.signalAll();
    }

    public TLongList getTime() {
        return time;
    }

    public TDoubleList getHigh() {
        return high;
    }

    public TDoubleList getLow() {
        return low;
    }

    public TDoubleList getOpen() {
        return open;
    }

    public TDoubleList getClose() {
        return close;
    }

    public TDoubleList getWap() {
        return wap;
    }

    public TLongList getVolume() {
        return volume;
    }

    public TIntList getCount() {
        return count;
    }

    public int getSize() {
        return size;
    }

    public SyncSignal getSignal() {
        return signal;
    }

    public TDoubleList getDoubleSeries(BarType type) {
        switch (type) {
        case OPEN:
            return open;

        case LOW:
            return low;

        case HIGH:
            return high;

        case CLOSE:
            return close;

        case WAP:
            return wap;
        }

        throw new IllegalArgumentException("Unsupported BarType " + type);
    }

    public double getLastBar(BarType type) {
        return getLastBar(type, 0);
    }

    public double getLastBar(BarType type, int shift) {
        TDoubleList series = getDoubleSeries(type);
        int offset = size - 1 - shift;
        return series.get(offset);
    }

}
