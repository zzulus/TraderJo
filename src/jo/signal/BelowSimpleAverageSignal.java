package jo.signal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.array.TDoubleArrayList;
import jo.app.App;
import jo.model.Bars;
import jo.model.MarketData;

public class BelowSimpleAverageSignal implements Signal {
    private static final Logger log = LogManager.getLogger(BelowSimpleAverageSignal.class);

    private int period;
    private double delta;
    private Bars bars;

    public BelowSimpleAverageSignal(int smaSize, double delta) {
        this.period = smaSize;
        this.delta = delta;
    }

    @Override
    public boolean isActive(App app, Contract contract, MarketData marketData) {
        if (bars == null) {
            bars = marketData.getBars(BarSize._5_secs);
        }

        TDoubleArrayList close = bars.getClose();
        int size = close.size();
        if (size < period) {
            return false;
        }

        double acc = 0;
        for (int i = 0; i < period; i++) {
            acc = acc + close.get(size - i - 1);
        }
        double saValue = acc / period;

        //log.info("MA {}, diff {}", saValue, saValue - marketData.getLastPrice());

        return saValue - marketData.getLastPrice() > delta;
    }

    @Override
    public String getName() {
        return "Below indicator";
    }

}
