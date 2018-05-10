package jo.position;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jo.model.Bars;
import jo.model.MarketData;

public class ATRPercentileOfTrailAmountStrategy implements TrailAmountStrategy {
    private List<TrailAmountStrategy> strategies;
    private double percentile;

    public ATRPercentileOfTrailAmountStrategy(Bars bars, double multiplier, int period, int size, double percentile) {
        this.percentile = percentile;
        this.strategies = new ArrayList<>();

        for (int offset = 0; offset < size; offset++) {
            strategies.add(new ATRTrailAmountStrategy(bars, multiplier, period, offset));
        }
    }

    @Override
    public Double getTrailAmount(MarketData md) {
        TDoubleList list = new TDoubleArrayList();

        for (TrailAmountStrategy strategy : strategies) {
            Double value = strategy.getTrailAmount(md);
            if (value != null) {
                list.add(value);
            }
        }

        if (list.isEmpty()) {
            return null;
        }

        list.sort();

        int position = (int) (list.size() * percentile);
        if (position > list.size()) {
            position = list.size() - 1;
        }

        return list.get(position);
    }

}
