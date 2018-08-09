package jo.position;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FixedDollarValuePositionSizeStrategy implements PositionSizeStrategy {
    protected final Logger log = LogManager.getLogger(this.getClass());
    private double maxDollarAmount;

    public FixedDollarValuePositionSizeStrategy(double maxDollarAmount) {
        this.maxDollarAmount = maxDollarAmount;
    }

    @Override
    public int getPositionSize(double openPrice, double riskPerShare) {
        return (int) (maxDollarAmount / openPrice);
    }
}
