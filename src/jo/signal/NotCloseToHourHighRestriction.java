package jo.signal;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.array.TDoubleArrayList;
import jo.app.TraderApp;
import jo.model.MarketData;

public class NotCloseToHourHighRestriction implements Signal {
    private static final int BARS_IN_HOUR = (60 * 60) / 5; // number of 5 sec bars in 1 hour
    private double delta;
    private TDoubleArrayList highs;

    public NotCloseToHourHighRestriction(double delta) {
        this.delta = delta;
    }

    @Override
    public boolean isActive(TraderApp app, Contract contract, MarketData marketData) {
        if (highs == null) {
            highs = marketData.getBars(BarSize._5_secs).getHigh();
        }

        int size = highs.size();
        int end = size - 1;
        int start = Math.max(0, end - BARS_IN_HOUR);

        double maxPrice = 0;
        for (int i = start; i <= end; i++) {
            maxPrice = Math.max(maxPrice, highs.get(i));
        }

        return maxPrice - marketData.getLastPrice() > delta;
    }

    @Override
    public String getName() {
        return "Not close to daily high";
    };
}
