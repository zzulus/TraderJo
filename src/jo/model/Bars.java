package jo.model;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

public class Bars {
    private static final Logger log = LogManager.getLogger(Bars.class);

    private int lastPos = -1;

    private final TLongArrayList time = new TLongArrayList();
    private final TDoubleArrayList high = new TDoubleArrayList();
    private final TDoubleArrayList low = new TDoubleArrayList();
    private final TDoubleArrayList open = new TDoubleArrayList();
    private final TDoubleArrayList close = new TDoubleArrayList();
    private final TDoubleArrayList wap = new TDoubleArrayList();
    private final TLongArrayList volume = new TLongArrayList();
    private final TIntArrayList count = new TIntArrayList();

    public Bar getLastBar() {
        return getBar(0);
    }

    public Bar getBar(int offsetFromEnd) {
        Preconditions.checkArgument(lastPos > -1, "No data yet");

        int offset = lastPos - offsetFromEnd;
        Preconditions.checkArgument(offset > -1, "Index out of bound: lastPos %s, offsetFromEnd %s", lastPos, offsetFromEnd);

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
        //log.info("AddBar: {}", bar);

        time.add(bar.getTime());
        high.add(bar.getHigh());
        low.add(bar.getLow());
        open.add(bar.getOpen());
        close.add(bar.getClose());
        wap.add(bar.getWap());
        volume.add(bar.getVolume());
        count.add(bar.getCount());

        lastPos++;
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
        return lastPos + 1;
    }
}
