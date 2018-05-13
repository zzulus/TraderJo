package jo.tech;

import static com.google.common.base.Preconditions.checkArgument;
import static jo.util.Formats.fmt;
import static jo.util.PriceUtils.fixPriceVariance;

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
    private boolean longPosition;
    private final MarketData md;
    private double trailAmount;

    private volatile boolean stop;
    private Thread thread;
    private long startTimeMs;

    public StopTrail(IBroker ib, Contract contract, Order order, MarketData md, double trailAmount) {
        this.ib = ib;
        this.contract = contract;
        this.order = order;
        this.md = md;
        this.trailAmount = fixPriceVariance(trailAmount);
        this.longPosition = (order.action() == Action.SELL);
    }

    public void start() {
        log.info("Start");
        this.startTimeMs = System.currentTimeMillis();
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
        SyncSignal signal = md.getSignal();
        double prevPrice = 0;

        while (!stop && signal.waitForSignal() && !stop) {
            double price = md.getLastPrice();
            if (prevPrice == price)
                continue;

            double newStopPrice = calculateTrailStopPrice(price);
            maybeUpdateStopPrice(newStopPrice);

            prevPrice = price;
        }

        log.info("Exit");
    }

    public synchronized void maybeUpdateStopPrice(double proposedStopPrice) {
        double lastPrice = md.getLastPrice();
        double orderPrice = fixPriceVariance(order.auxPrice());
        proposedStopPrice = fixPriceVariance(proposedStopPrice);
        boolean update = false;

        //        log.info("Check: {} order stop price {}, proposed stop price {}, trail amount {}, last price {}",
        //                longPosition ? "long" : "short",
        //                fmt(orderPrice), fmt(proposedStopPrice), fmt(trailAmount), fmt(lastPrice));

        if (longPosition && proposedStopPrice > orderPrice) {
            update = true;
        }

        if (!longPosition && proposedStopPrice < orderPrice) {
            update = true;
        }

        if (update) {
            log.info("Adjusting {} stop price from {} to {} using trail amount {} and last price {}",
                    longPosition ? "long" : "short",
                    fmt(orderPrice), fmt(proposedStopPrice), fmt(trailAmount), fmt(lastPrice));

            order.auxPrice(proposedStopPrice);
            ib.placeOrModifyOrder(contract, order, null);
        }
    }

    private double calculateTrailStopPrice(double lastPrice) {
        double trailStop;
        if (longPosition) {
            trailStop = lastPrice - trailAmount;
        } else {
            trailStop = lastPrice + trailAmount;
        }
        return fixPriceVariance(trailStop);
    }

    public synchronized void setTrailAmount(double newTrailAmount) {
        newTrailAmount = fixPriceVariance(newTrailAmount);

        if (trailAmount == this.trailAmount) {
            return;
        }

        log.info("Updating trailAmount: from {} to {}",
                fmt(this.trailAmount), fmt(newTrailAmount));

        checkArgument(newTrailAmount > 0, "trailAmount cannot be less zero: %s", newTrailAmount);
        this.trailAmount = newTrailAmount;

        double price = md.getLastPrice();
        double newStopPrice = calculateTrailStopPrice(price);
        maybeUpdateStopPrice(newStopPrice);
    }

    public double getTrailAmount() {
        return trailAmount;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }
}
