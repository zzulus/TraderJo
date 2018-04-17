package jo.filter;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.TDoubleList;
import jo.model.Bars;
import jo.model.IApp;
import jo.model.MarketData;

public class TwoMAFilter implements Filter {
    private int shortPeriod;
    private int longPeriod;
    private Bars bars;

    public TwoMAFilter(int shortPeriod, int longPeriod) {
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        if (bars == null) {
            bars = marketData.getBars(BarSize._5_secs);
        }

        try {
            TDoubleList close = bars.getClose();
            int size = close.size();
            if (size < longPeriod) {
                return false;
            }

            double accShort = 0;
            for (int i = size - shortPeriod; i < size; i++) {
                accShort = accShort + close.get(i);
            }
            double shortAvg = accShort / shortPeriod;

            double accLong = 0;
            for (int i = size - longPeriod; i < size; i++) {
                accLong = accLong + close.get(i);
            }
            double longAvg = accLong / longPeriod;

            return shortAvg > longAvg;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public String getName() {
        return "Below indicator";
    }

}
