package jo.position;

import jo.model.Bars;
import jo.model.MarketData;
import jo.tech.ATR;

public class ATRTrailAmountStrategy implements TrailAmountStrategy {
    private final double multiplier;
    private final Bars bars;
    private int prevBarsSize = -1;
    private Double value;
    private ATR atr;

    public ATRTrailAmountStrategy(Bars bars, double multiplier, int period, int offset) {
        this.atr = new ATR(bars, period, offset);
        this.multiplier = multiplier;
        this.bars = bars;
    }

    @Override
    public Double getTrailAmount(MarketData md) {
        int barsSize = bars.getSize();

        if (barsSize == prevBarsSize) {
            return value;
        }

        prevBarsSize = barsSize;
        value = atr.get();

        if (value != null) {
            value = value * multiplier;
        }

        return value;
    }
}
