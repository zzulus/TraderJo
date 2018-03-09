package jo.signal;

import com.ib.client.Contract;

import jo.app.IApp;
import jo.model.MarketData;

public class OpenAfterTimeRestriction implements Signal {
    private long openAfterTimeMillis = 0;

    public OpenAfterTimeRestriction(long openAfterTimeMillis) {
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
        return "Not close to daily high";
    };
}
