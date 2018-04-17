package jo.model.calc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Types.BarSize;

import jo.model.Bar;
import jo.model.Bars;
import jo.model.MarketData;
import jo.util.BarSizeUtils;

public class RealtimeBarAggregator {
    private static final Logger LOG = LogManager.getLogger(RealtimeBarAggregator.class);
    private final MarketData md;
    private final Bars srcBars;
    private final int targetBarSizeRatio;
    private final BarSize barSize;

    public RealtimeBarAggregator(MarketData md, Bars srcBars, BarSize barSize) {
        this.md = md;
        this.srcBars = srcBars;
        this.targetBarSizeRatio = BarSizeUtils.getRatio(barSize, BarSizeUtils.REALTIME_BAR_SIZE);
        this.barSize = barSize;
    }

    public void update() {
        int srcSize = srcBars.getSize();
        if (srcSize > 0 && srcSize % targetBarSizeRatio == 0) {
            int to = srcSize;
            int from = srcSize - targetBarSizeRatio;

            long time = srcBars.getTime().get(from);
            double high = srcBars.getHigh().get(from);
            double low = srcBars.getLow().get(from);
            double open = srcBars.getOpen().get(from);
            double close = srcBars.getClose().get(to - 1);
            double wap = 0;
            long volume = 0;
            int count = 0;

            for (int i = from; i < to; i++) {
                high = Math.max(high, srcBars.getHigh().get(i));
                low = Math.min(low, srcBars.getLow().get(i));
                wap += srcBars.getWap().get(i);
                volume += srcBars.getVolume().get(i);
                count += srcBars.getCount().get(i);
            }

            wap = wap / targetBarSizeRatio; // TODO redo with volume weighted average

            Bar bar = new Bar();
            bar.setTime(time);
            bar.setHigh(high);
            bar.setLow(low);
            bar.setOpen(open);
            bar.setClose(close);
            bar.setWap(wap);
            bar.setVolume(volume);
            bar.setCount(count);

            md.addBar(barSize, bar);
        }
    }

}
