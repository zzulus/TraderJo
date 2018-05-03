package jo.position;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DollarValuePositionSizeStrategy implements PositionSizeStrategy {
    protected final Logger log = LogManager.getLogger(this.getClass());
    private double maxDollarAmount;
    private double maxRiskDollarAmount;

    public DollarValuePositionSizeStrategy(double maxDollarAmount, double maxRiskDollarAmount) {
        this.maxDollarAmount = maxDollarAmount;
        this.maxRiskDollarAmount = maxRiskDollarAmount;
    }

    @Override
    public int getPositionSize(double openPrice, double riskPerShare) {
        int maxPositionSize = (int) (maxDollarAmount / openPrice);
        int maxRiskShares = (int) (maxRiskDollarAmount / riskPerShare);
        int positionSize = Math.min(maxPositionSize, maxRiskShares);

        log.info("maxPositionSize {}, maxRiskShares {} => positionSize {}    [openPrice {}, maxRiskDollarAmount {}]",
                maxPositionSize, maxRiskShares, positionSize, openPrice, maxRiskDollarAmount);

        return positionSize;
    }
}
