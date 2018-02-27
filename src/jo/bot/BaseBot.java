package jo.bot;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;

import jo.controller.IBService;
import jo.handler.IOrderHandler;
import jo.model.MarketData;
import jo.signal.Signal;

public abstract class BaseBot implements Bot {
    protected final Logger log = LogManager.getLogger(this.getClass());
    protected final Contract contract;
    protected final int totalQuantity;
    protected MarketData marketData;

    protected Signal signal;
    protected volatile boolean openOrderIsActive = false;
    protected volatile boolean takeProfitOrderIsActive = false;

    protected Order openOrder;
    protected Order takeProfitOrder;

    public BaseBot(Contract contract, int totalQuantity) {
        checkArgument(totalQuantity > 0);
        checkNotNull(contract);

        this.contract = contract;
        this.totalQuantity = totalQuantity;
    }

    protected void placeOrders(IBService ib) {
        log.info("Placing order: Open at {}, close at {}, last {}", openOrder.lmtPrice(), takeProfitOrder.lmtPrice());

        if (openOrderIsActive) {
            throw new IllegalStateException("openOrder is active");
        }
        if (takeProfitOrderIsActive) {
            throw new IllegalStateException("takeProfitOrder is active");
        }

        openOrderIsActive = true;
        takeProfitOrderIsActive = true;

        ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
    }

    protected void modifyOrders(IBService ib) {
        log.info("Modify orders: Open at {}, close at {}, last {}", openOrder.lmtPrice(), takeProfitOrder.lmtPrice());

        if (!openOrderIsActive) {
            throw new IllegalStateException("openOrder is not active");
        }

        if (takeProfitOrderIsActive) {
            throw new IllegalStateException("takeProfitOrder is not active");
        }

        ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
        ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
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
                openOrderIsActive = false;
            }
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.error("orderStatus: status {}, filled {}, remaining {}", status, filled, remaining);
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
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
                takeProfitOrderIsActive = false;
            }
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.error("orderStatus: status {}, filled {}, remaining {}", status, filled, remaining);
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
            log.error("Error: {} - {}", errorCode, errorMsg);
        }
    }
}
