package jo.signal;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.array.TDoubleArrayList;
import jo.app.TraderApp;
import jo.model.Bars;
import jo.model.MarketData;

public class LastClosePositiveRestriction implements Signal {
    private int len;
    private Bars bars;

    public LastClosePositiveRestriction(int len) {
        this.len = len;
    }

    @Override
    public boolean isActive(TraderApp app, Contract contract, MarketData marketData) {
        if (bars == null) {
            bars = marketData.getBars(BarSize._5_secs);
        }
        TDoubleArrayList close = bars.getClose(); // close or positive bars?

        int size = close.size();
        boolean isActive = true;
        if (size < len) {
            return false;
        }

        int end = size - 1;
        int start = size - len;

        for (int i = start; i < end; i++) {
            double p1 = close.get(i);
            double p2 = close.get(i + 1);
            if (p1 > p2) {
                isActive = false;
                break;
            }
        }

        return isActive;
    }

    @Override
    public String getName() {
        return "";
    };
}
