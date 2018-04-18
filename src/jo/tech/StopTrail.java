package jo.tech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.Types.Action;

import jo.controller.IBroker;
import jo.model.MarketData;
import jo.util.AsyncExec;
import jo.util.SyncSignal;

public class StopTrail {
    private static final Logger log = LogManager.getLogger(StopTrail.class);

    private final IBroker ib;
    private final Contract contract;
    private final Order order;
    private final MarketData md;
    private final double trailAmount;

    private boolean stop;
    private Thread thread;

    public StopTrail(IBroker ib, Contract contract, Order order, MarketData md, double trailAmount) {
        this.ib = ib;
        this.contract = contract;
        this.order = order;
        this.md = md;
        this.trailAmount = trailAmount;
    }

    public void start() {
        log.info("Start");
        thread = AsyncExec.startThread("StopTrail#" + contract.symbol(), this::run);
    }

    public void stop() {
        log.info("Stop");
        stop = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void run() {
        log.info("Run");
        SyncSignal signal = md.getUpdateSignal();
        boolean isLongPosition = (order.action() == Action.SELL);

        while (!stop && signal.waitForSignal() && !stop) {
            double lastPrice = md.getLastPrice();
            double orderPrice = order.auxPrice();
            double adjustedPrice = lastPrice - trailAmount;

            log.info("Check: stop price {}, adjusted price {}, trail amount {}, last price {}",
                    orderPrice, adjustedPrice, trailAmount, lastPrice);

            if (isLongPosition && adjustedPrice > orderPrice) {
                log.info("Adjusting long stop price from {} to {} using trail amount {} and last price {}",
                        orderPrice, adjustedPrice, trailAmount, lastPrice);

                order.auxPrice(adjustedPrice);
                ib.placeOrModifyOrder(contract, order, null);
            }

            if (!isLongPosition && adjustedPrice < orderPrice) {
                log.info("Adjusting short stop price from {} to {} using trail amount {} and last price {}",
                        orderPrice, adjustedPrice, trailAmount, lastPrice);

                order.auxPrice(adjustedPrice);
                ib.placeOrModifyOrder(contract, order, null);
            }
        }

        log.info("Exit");
    }

}
