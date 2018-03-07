package jo.signal;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.array.TDoubleArrayList;
import jo.app.TraderApp;
import jo.model.MarketData;

public class LastIsGreaterThanCloseRestriction implements Signal {
    private TDoubleArrayList open;
    private TDoubleArrayList close;

    @Override
    public boolean isActive(TraderApp app, Contract contract, MarketData marketData) {
        if (close == null) {
            open = marketData.getBars(BarSize._5_secs).getOpen();
            close = marketData.getBars(BarSize._5_secs).getClose();
        }

        int size = close.size();
        if (size < 3) {
            return false;
        }

        int end = size - 1;

        double lastClose = close.get(end);
        double lastTrade = marketData.getLastPrice();

        return lastTrade > lastClose;
    }

    @Override
    public String getName() {
        return "";
    };
}
