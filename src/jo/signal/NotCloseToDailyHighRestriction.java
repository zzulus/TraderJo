package jo.signal;

import com.ib.client.Contract;

import jo.app.TraderApp;
import jo.model.MarketData;

public class NotCloseToDailyHighRestriction implements Signal {
    private double delta;

    public NotCloseToDailyHighRestriction(double delta) {
        this.delta = delta;
    }

    @Override
    public boolean isActive(TraderApp app, Contract contract, MarketData marketData) {
        return marketData.getTodayHighPrice() - marketData.getLastPrice() > delta;
    }

    @Override
    public String getName() {
        return "Not close to daily high";
    };
}
