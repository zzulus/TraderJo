package jo.model;

import com.google.common.base.Preconditions;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import jo.collection.TDoubleNakedArrayList;
import jo.util.SyncSignal;

public class Bars {
    private int size = 0;
    private final SyncSignal signal = new SyncSignal();
    private final TLongList time = new TLongArrayList();
    private final TDoubleNakedArrayList high = new TDoubleNakedArrayList();
    private final TDoubleNakedArrayList low = new TDoubleNakedArrayList();
    private final TDoubleNakedArrayList open = new TDoubleNakedArrayList();
    private final TDoubleNakedArrayList close = new TDoubleNakedArrayList();
    private final TDoubleNakedArrayList wap = new TDoubleNakedArrayList();
    private final TLongList volume = new TLongArrayList();
    private final TIntList count = new TIntArrayList();

    public Bar getLastBar() {
        Preconditions.checkArgument(size > 0, "No data yet");
        return getBarFromEnd(0);
    }

    /*
     * Get Nth bar from the beginning, typical List behavior.
     */
    public Bar get(int i) {
        Bar bar = new Bar();
        bar.setTime(time.get(i));
        bar.setHigh(high.get(i));
        bar.setLow(low.get(i));
        bar.setOpen(open.get(i));
        bar.setClose(close.get(i));
        bar.setWap(wap.get(i));
        bar.setVolume(volume.get(i));
        bar.setCount(count.get(i));

        return bar;
    }

    public Bar getBarFromEnd(int offsetFromEnd) {
        Preconditions.checkArgument(size > 0, "No data yet");

        int offset = size - offsetFromEnd - 1;
        Preconditions.checkArgument(offset > -1, "Index out of bound: lastPos %s, offsetFromEnd %s", size, offsetFromEnd);
        return get(offset);
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

    public TDoubleNakedArrayList getDoubleSeries(BarType type) {
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
