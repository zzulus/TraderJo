package jo.filter;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.TDoubleList;
import jo.controller.IApp;
import jo.model.MarketData;

public class LastIsGreaterThanCloseFilter implements Filter {
    private TDoubleList open;
    private TDoubleList close;

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
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
