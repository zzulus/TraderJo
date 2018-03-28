package jo.filter;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import jo.app.IApp;
import jo.model.Bars;
import jo.model.MarketData;

public class HasAtLeastNBarsFilter implements Filter {
    private int cnt;
    private Bars bars;

    public HasAtLeastNBarsFilter(int cnt) {
        this.cnt = cnt;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        if (bars == null) {
            bars = marketData.getBars(BarSize._5_secs);
        }
        return bars.getSize() > cnt;
    }

    @Override
    public String getName() {
        return "Has > " + cnt + " bars";
    }
}
