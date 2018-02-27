package jo.signal;

import com.google.common.base.Preconditions;
import com.ib.client.Contract;

import jo.app.App;
import jo.model.MarketData;

public class RandomSignal implements Signal {
    private double weight;

    public RandomSignal(double weight) {
        Preconditions.checkArgument(weight > 0.0d && weight < 1.0d, "weight value should be in (0, 1): %s", weight);
        this.weight = weight;
    }

    @Override
    public boolean isActive(App app, Contract contract, MarketData marketData) {
        return Math.random() <= weight;
    }
    
    public String getName() {
        return "RandomSignal";
    }
}
