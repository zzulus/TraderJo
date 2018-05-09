package jo.position;

import java.util.ArrayList;
import java.util.List;

import jo.model.Bars;
import jo.model.MarketData;

public class ATRMaxOfTrailAmountStrategy implements TrailAmountStrategy {
    private List<TrailAmountStrategy> strategies;

    public ATRMaxOfTrailAmountStrategy(Bars bars, double multiplier, int period, int maxOfSize) {
        strategies = new ArrayList<>();

        for (int offset = 0; offset < maxOfSize; offset++) {
            strategies.add(new ATRTrailAmountStrategy(bars, multiplier, period, offset));
        }
    }

    @Override
    public Double getTrailAmount(MarketData md) {
        Double maxValue = null;

        for (TrailAmountStrategy strategy : strategies) {
            Double value = strategy.getTrailAmount(md);
            if (value != null) {
                maxValue = (maxValue == null) ? value : Math.max(maxValue, value);
            }
        }

        return maxValue;
    }
}
