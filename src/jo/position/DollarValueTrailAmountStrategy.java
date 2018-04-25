package jo.position;

import jo.controller.IBroker;
import jo.model.IApp;
import jo.model.MarketData;

public class DollarValueTrailAmountStrategy implements TrailAmountStrategy {
    private final double value;

    public DollarValueTrailAmountStrategy(double value) {
        this.value = value;
    }

    @Override
    public Double getTrailAmount(MarketData md) {
        return value;
    }

    public static DollarValueTrailAmountStrategy of(double value) {
        return new DollarValueTrailAmountStrategy(value);
    }

    @Override
    public void init(IBroker ib, IApp app) {
    }
}
