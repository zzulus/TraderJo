package jo.filter;

import com.ib.client.Contract;

import jo.model.IApp;
import jo.model.MarketData;

public class NotCloseToDailyHighFilter implements Filter {
    private double delta;

    public NotCloseToDailyHighFilter(double delta) {
        this.delta = delta;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        return marketData.getTodayHighPrice() - marketData.getLastPrice() > delta;
    }

    @Override
    public String getName() {
        return "Not close to daily high";
    };
}
