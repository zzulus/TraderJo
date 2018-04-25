package jo.position;

import jo.model.MarketData;

public interface PositionSizeStrategy {
    int getPositionSize(MarketData md);
}
