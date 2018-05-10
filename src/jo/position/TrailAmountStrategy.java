package jo.position;

import jo.model.Context;
import jo.model.MarketData;

public interface TrailAmountStrategy {
    Double getTrailAmount(MarketData md);

    default void init(Context ctx) {
    }
}
