package jo.position;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jo.model.MarketData;

public class DollarValuePositionSizeStrategy implements PositionSizeStrategy {
    protected final Logger log = LogManager.getLogger(this.getClass());
    private double dollarAmount;

    public DollarValuePositionSizeStrategy(double dollarAmount) {
        this.dollarAmount = dollarAmount;
    }

    @Override
    public int getPositionSize(MarketData md) {
        double lastPrice;
        while ((lastPrice = md.getLastPrice()) < 1) {
            log.info("Waiting for price data");
            md.getSignal().waitForSignal();
        }

        int positionSize = (int) (dollarAmount / lastPrice);
        log.info("Position size " + positionSize);

        return positionSize;
    }
}
