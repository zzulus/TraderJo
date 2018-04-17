package jo.filter;

import com.google.common.base.Preconditions;
import com.ib.client.Contract;

import jo.model.IApp;
import jo.model.MarketData;

public class RandomFilter implements Filter {
    private double weight;

    public RandomFilter(double weight) {
        Preconditions.checkArgument(weight > 0.0d && weight < 1.0d, "weight value should be in (0, 1): %s", weight);
        this.weight = weight;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        return Math.random() <= weight;
    }

    public String getName() {
        return "RandomSignal";
    }
}
