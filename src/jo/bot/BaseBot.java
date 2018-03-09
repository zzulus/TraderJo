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
import jo.handler.IOrderHandler;
import jo.model.MarketData;
import jo.signal.Signal;

public abstract class BaseBot implements Bot {
    protected final Logger log = LogManager.getLogger(this.getClass());
    protected final Contract contract;
    protected final int totalQuantity;
    protected MarketData marketData;

    protected Signal positionSignal;
    protected volatile boolean openOrderIsActive = false;
    protected volatile boolean takeProfitOrderIsActive = false;

    protected Order openOrder;
    protected Order takeProfitOrder;

    protected Thread thread;

    public BaseBot(Contract contract, int totalQuantity) {
        checkArgument(totalQuantity > 0);
        checkNotNull(contract);

        this.contract = contract;
        this.totalQuantity = totalQuantity;
    }

    protected void placeOrders(IBroker ib) {
        log.info("Placing order: Open at {}, close at {}", openOrder.lmtPrice(), takeProfitOrder.lmtPrice());

        if (openOrderIsActive) {
            throw new IllegalStateException("openOrder is active");
        }

        if (takeProfitOrderIsActive) {
            throw new IllegalStateException("takeProfitOrder is active");
        }

        if (takeProfitOrder.lmtPrice() < 130 || openOrder.lmtPrice() < 130) {
            throw new IllegalStateException("open/close price doesn't conform to min 0.10 price rule");
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
        log.info("Modify orders: Open at {}, close at {}", openOrder.lmtPrice(), takeProfitOrder.lmtPrice());

        if (!openOrderIsActive) {
            // throw new IllegalStateException("openOrder is not active");
            return;
        }

        if (takeProfitOrderIsActive) {
            // throw new IllegalStateException("takeProfitOrder is not active");
            return;
        }

        if (takeProfitOrder.lmtPrice() < 130 || openOrder.lmtPrice() < 130) {
            throw new IllegalStateException("open/close price doesn't conform to min 0.10 price rule");
        }

        if (takeProfitOrder.lmtPrice() - openOrder.lmtPrice() < 0.05) {
            throw new IllegalStateException("open/close price doesn't conform to min 0.05 diff rule");
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

    public void shutdown() {
        thread.interrupt();
    }

    protected class OpenPositionOrderHandler implements IOrderHandler {
        private final Logger log = LogManager.getLogger(OpenPositionOrderHandler.class);
        private boolean isActive = true;

        @Override
        public void orderState(OrderState orderState) {
            String status = orderState.getStatus();
            log.info("orderState: {}", status);
            if (isActive && "Filled".equals(status)) {
                isActive = false;
                openPositionOrderFilled();
            }
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("orderStatus: status {}, filled {}, remaining {}", status, filled, remaining);
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
            log.info("orderState: {}", status);
            if (isActive && "Filled".equals(status)) {
                isActive = false;
                takeProfitOrderFilled();
            }
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("orderStatus: status {}, filled {}, remaining {}", status, filled, remaining);
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
            log.error("Error: {} - {}", errorCode, errorMsg);
        }
    }
}
