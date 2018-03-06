package jo.signal;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.array.TDoubleArrayList;
import jo.app.App;
import jo.model.Bars;
import jo.model.MarketData;

public class LastClosePositiveRestriction implements Signal {
    private int len;
    private Bars bars;

    public LastClosePositiveRestriction(int len) {
        this.len = len;
    }

    @Override
    public boolean isActive(App app, Contract contract, MarketData marketData) {
        if (bars == null) {
            bars = marketData.getBars(BarSize._5_secs);
        }
        TDoubleArrayList close = bars.getClose();

        int size = close.size();
        boolean isActive = true;
        if (size < len) {
            return false;
        }

        int end = size - 1;
        int start = end - len + 1;

        for (int i = start; i < end; i++) {
            double p1 = close.get(i);
            double p2 = close.get(i + 1);
            if (p2 < p1) {
                isActive = false;
                break;
            }
        }

        double openPrice = (marketData.getBidPrice() + marketData.getAskPrice()) / 2d;

        if (openPrice < close.get(end)) {
            return false;
        }

        return isActive;
    }

    @Override
    public String getName() {
        return "";
    };
}
