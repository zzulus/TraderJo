package jo.filter;

import com.ib.client.Contract;

import jo.model.IApp;
import jo.model.MarketData;

public class OpenAfterTimeFilter implements Filter {
    private long openAfterTimeMillis = 0;

    public OpenAfterTimeFilter(long openAfterTimeMillis) {
        this.openAfterTimeMillis = openAfterTimeMillis;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        return System.currentTimeMillis() >= openAfterTimeMillis;
    }

    public void setOpenAfterTimeMillis(long openAfterTimeMillis) {
        this.openAfterTimeMillis = openAfterTimeMillis;
    }

    @Override
    public String getName() {
        return "";
    };
}
