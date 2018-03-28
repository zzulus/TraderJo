package jo.bot;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;

import jo.controller.IBroker;
import jo.filter.Filter;
import jo.handler.IOrderHandler;
import jo.model.MarketData;

public abstract class BaseBot implements Bot {
    protected final Logger log = LogManager.getLogger(this.getClass());
    protected final Contract contract;
    protected final int totalQuantity;
    protected MarketData md;

    protected Filter positionFilter;
    protected volatile boolean openOrderIsActive = false;
    protected volatile boolean takeProfitOrderIsActive = false;
    protected volatile boolean stopLossOrderIsActive = false; // TODO make this shit better

    protected Order openOrder;
    protected Order takeProfitOrder;
    protected Order stopLossOrder;
    protected Order mocOrder;

    protected Thread thread;

    public BaseBot(Contract contract, int totalQuantity) {
        checkArgument(totalQuantity > 0);
        checkNotNull(contract);

        this.contract = contract;
        this.totalQuantity = totalQuantity;
    }

    protected void placeOrders(IBroker ib, Order... orders) {
        for (Order order : orders) {
            log.info("Placing order: {} {}", order.action(), order.lmtPrice());

            ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
            ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
            ib.placeOrModifyOrder(contract, stopLossOrder, new StopLossOrderHandler());

            openOrder.transmit(true);
            takeProfitOrder.transmit(true);
            stopLossOrder.transmit(true);
        }
    }

    protected void placeTrioBracketOrder(IBroker ib) {
        log.info("Placing order: Open at {} {}, close at {} {}, stop loss ",
                openOrder.action(), openOrder.lmtPrice(),
                takeProfitOrder.action(), takeProfitOrder.lmtPrice(),
                stopLossOrder.action(), stopLossOrder.auxPrice());

        openOrderIsActive = true;
        takeProfitOrderIsActive = true;
        stopLossOrderIsActive = true;

        ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
        ib.placeOrModifyOrder(contract, stopLossOrder, new StopLossOrderHandler());

        openOrder.transmit(true);
        takeProfitOrder.transmit(true);
        stopLossOrder.transmit(true);
    }
    
    protected void placeDuoBracketOrder(IBroker ib) {
        log.info("Placing order: Open at {} {}, close at {} {}, stop loss ",
                openOrder.action(), openOrder.lmtPrice(),
                takeProfitOrder.action(), takeProfitOrder.lmtPrice());

        openOrderIsActive = true;
        takeProfitOrderIsActive = true;

        ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());

        openOrder.transmit(true);
        takeProfitOrder.transmit(true);
    }

    protected void placeOrders(IBroker ib) {
        log.info("Placing long order: Open at {} {}, close at {} {}",
                openOrder.action(), openOrder.lmtPrice(), takeProfitOrder.action(), takeProfitOrder.lmtPrice());

        if (openOrderIsActive) {
            throw new IllegalStateException("openOrder is active");
        }

        if (takeProfitOrderIsActive) {
            throw new IllegalStateException("takeProfitOrder is active");
        }

        if (takeProfitOrder.lmtPrice() < 5 || openOrder.lmtPrice() < 5) {
            throw new IllegalStateException("open/close price doesn't conform to min $5 price rule, "
                    + "open " + openOrder.lmtPrice() + ", take profit " + takeProfitOrder.lmtPrice());
        }

        if (takeProfitOrder.lmtPrice() - openOrder.lmtPrice() < 0.05) {
            String msg = String.format("open/close price doesn't conform to min 0.05 diff rule, open %.2f, close %.2f", openOrder.lmtPrice(), takeProfitOrder.lmtPrice());
            throw new IllegalStateException(msg);
        }
        openOrderIsActive = true;
        takeProfitOrderIsActive = true;

        ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
    }

    protected void modifyOrders(IBroker ib) {
        log.info("Modify long order: Open at {} {}, close at {} {}",
                openOrder.action(), openOrder.lmtPrice(), takeProfitOrder.action(), takeProfitOrder.lmtPrice());

        if (!openOrderIsActive) {
            // throw new IllegalStateException("openOrder is not active");
            return;
        }

        if (takeProfitOrderIsActive) {
            // throw new IllegalStateException("takeProfitOrder is not active");
            return;
        }

        if (takeProfitOrder.lmtPrice() < 5 || openOrder.lmtPrice() < 5) {
            throw new IllegalStateException("open/close price doesn't conform to min $5 price rule");
        }

        if (takeProfitOrder.lmtPrice() - openOrder.lmtPrice() < 0.05) {
            throw new IllegalStateException("open/close price doesn't conform to min 0.05 diff rule");
        }

        // TODO Use existing handlers?
        ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
    }

    protected void placeShortOrders(IBroker ib) {
        log.info("Placing short order: Open at {} {}, close at {} {}",
                openOrder.action(), openOrder.lmtPrice(), takeProfitOrder.action(), takeProfitOrder.lmtPrice());

        if (openOrderIsActive) {
            throw new IllegalStateException("openOrder is active");
        }

        if (takeProfitOrderIsActive) {
            throw new IllegalStateException("takeProfitOrder is active");
        }

        if (takeProfitOrder.lmtPrice() < 5 || openOrder.lmtPrice() < 5) {
            throw new IllegalStateException("open/close price doesn't conform to min $5 price rule, "
                    + "open " + openOrder.lmtPrice() + ", take profit " + takeProfitOrder.lmtPrice());
        }

        if (openOrder.lmtPrice() - takeProfitOrder.lmtPrice() < 0.02) {
            String msg = String.format("open/close price doesn't conform to min 0.04 diff rule, open %.2f, close %.2f", openOrder.lmtPrice(), takeProfitOrder.lmtPrice());
            throw new IllegalStateException(msg);
        }
        openOrderIsActive = true;
        takeProfitOrderIsActive = true;

        ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
    }

    protected void modifyShortOrders(IBroker ib) {
        log.info("Modify short order: Open at {} {}, close at {} {}",
                openOrder.action(), openOrder.lmtPrice(), takeProfitOrder.action(), takeProfitOrder.lmtPrice());

        if (!openOrderIsActive) {
            // throw new IllegalStateException("openOrder is not active");
            return;
        }

        if (takeProfitOrderIsActive) {
            // throw new IllegalStateException("takeProfitOrder is not active");
            return;
        }

        if (takeProfitOrder.lmtPrice() < 5 || openOrder.lmtPrice() < 5) {
            throw new IllegalStateException("open/close price doesn't conform to min $5 price rule");
        }

        if (openOrder.lmtPrice() - takeProfitOrder.lmtPrice() < 0.02) {
            throw new IllegalStateException("open/close price doesn't conform to min 0.04 diff rule");
        }

        // TODO Use existing handlers?
        ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
    }

    protected void stopLossOrder(IBroker ib) {
        log.info("Stop loss order  at {}", takeProfitOrder.lmtPrice());

        if (!takeProfitOrderIsActive) {
            // throw new IllegalStateException("takeProfitOrder is not active");
            return;
        }

        // TODO Use existing handlers?
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
    }

    protected static double fixPriceVariance(double price) {
        double minTick = 0.01;
        int d = (int) (price / minTick);
        return d * minTick;
    }

    protected void openPositionOrderFilled() {
        openOrderIsActive = false;
    }

    protected void takeProfitOrderFilled() {
        takeProfitOrderIsActive = false;
    }

    protected void takeProfitOrderCancelled() {
        takeProfitOrderIsActive = false;
    }

    protected void stopLossOrderFilled() {
        stopLossOrderIsActive = false;
    }

    protected void stopLossOrderCancelled() {
        stopLossOrderIsActive = false;
    }

    public void shutdown() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    protected class OpenPositionOrderHandler implements IOrderHandler {
        private final Logger log = LogManager.getLogger(OpenPositionOrderHandler.class);
        private boolean isActive = true;

        @Override
        public void orderState(OrderState orderState) {
            String status = orderState.getStatus();
            log.info("OpenPosition: OrderState: {}", status);
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("OpenPosition: OderStatus: status {}, filled {}, remaining {}", status, filled, remaining);

            if (isActive && OrderStatus.Filled == status && remaining < 0.1) {
                isActive = false;
                openPositionOrderFilled();
            }

        }

        @Override
        public void handle(int errorCode, String errorMsg) {
            log.error("Error: {} - {}", errorCode, errorMsg);
        }

    }

    protected class TakeProfitOrderHandler implements IOrderHandler {
        private final Logger log = LogManager.getLogger(TakeProfitOrderHandler.class);
        private boolean isActive = true;

        @Override
        public void orderState(OrderState orderState) {
            String status = orderState.getStatus();
            log.info("TakeProfit: OrderState: {}", status);
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("TakeProfit: OderStatus: status {}, filled {}, remaining {}", status, filled, remaining);

            if (isActive && OrderStatus.Filled == status && remaining < 0.1) {
                isActive = false;
                takeProfitOrderFilled();
            }

            if (isActive && OrderStatus.Cancelled == status && remaining < 0.1) {
                isActive = false;
                takeProfitOrderCancelled();
            }
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
            log.error("Error: {} - {}", errorCode, errorMsg);
        }
    }

    protected class StopLossOrderHandler implements IOrderHandler {
        private final Logger log = LogManager.getLogger(StopLossOrderHandler.class);
        private boolean isActive = true;

        @Override
        public void orderState(OrderState orderState) {
            String status = orderState.getStatus();
            log.info("StopLoss: OrderState: {}", status);

        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("StopLoss: OderStatus: status {}, filled {}, remaining {}", status, filled, remaining);

            if (isActive && OrderStatus.Filled == status && remaining < 0.1) {
                isActive = false;
                stopLossOrderFilled();
            }

            if (isActive && OrderStatus.Cancelled == status && remaining < 0.1) {
                isActive = false;
                stopLossOrderCancelled();
            }
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
            log.error("Error: {} - {}", errorCode, errorMsg);
        }
    }
}
