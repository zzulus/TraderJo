package jo.signal;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.array.TDoubleArrayList;
import jo.app.IApp;
import jo.model.Bars;
import jo.model.MarketData;

public class BelowSimpleAverageSignal implements Signal {
    private int period;
    private double delta;
    private Bars bars;

    public BelowSimpleAverageSignal(int smaSize, double delta) {
        this.period = smaSize;
        this.delta = delta;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        if (bars == null) {
            bars = marketData.getBars(BarSize._5_secs);
        }

        TDoubleArrayList close = bars.getClose();
        int size = close.size();
        if (size < period) {
            return false;
        }

        double acc = 0;
        for (int i = size - period - 1; i < period; i++) {
            acc = acc + close.get(i);
        }
        double saValue = acc / period;

        // log.info("MA {}, diff {}", saValue, saValue - marketData.getLastPrice());

        return saValue - marketData.getLastPrice() > delta;
    }

    @Override
    public String getName() {
        return "Below indicator";
    }

}
