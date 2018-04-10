package jo.bot;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.Types.Action;

import jo.filter.Filter;
import jo.handler.IOrderHandler;
import jo.model.MarketData;

public abstract class BaseBot implements Bot {
    protected final Logger log = LogManager.getLogger(this.getClass());
    protected final Contract contract;
    protected final int totalQuantity;
    protected MarketData md;

    protected Filter positionFilter;

    protected Order openOrder;
    protected Order takeProfitOrder;
    protected Order stopLossOrder;
    protected Order mocOrder;

    protected OrderStatus openOrderStatus = null;
    protected OrderStatus takeProfitOrderStatus = null;

    protected Thread thread;

    public BaseBot(Contract contract, int totalQuantity) {
        checkArgument(totalQuantity > 0);
        checkNotNull(contract);

        this.contract = contract;
        this.totalQuantity = totalQuantity;
    }

    // TODO Move out
    protected static double fixPriceVariance(double price) {
        double minTick = 0.01;
        int d = (int) (price / minTick);
        return d * minTick;
    }

    protected void openPositionOrderSubmitted() {

    }

    protected void openPositionOrderFilled(double avgFillPrice) {

    }

    protected void openPositionOrderCancelled() {
    }

    protected void takeProfitOrderSubmitted() {

    }

    protected void takeProfitOrderFilled(double takeProfitOrderAvgFillPrice) {
        double priceDiff = takeProfitOrderAvgFillPrice - openOrderAvgFillPrice;
        double longPnL = openOrder.totalQuantity() * priceDiff;
        if (openOrder.action() == Action.BUY) {
            log.info(String.format("P&L %.2f", longPnL));
        } else {
            log.info(String.format("P&L %.2f", -longPnL));
        }
    }

    protected void takeProfitOrderCancelled() {
    }

    /*
     * Actinable states:
     * Open Order: 
     *  Unknown, Canceled -> Can open position
     *  Filled -> Check TakeProfit order 
     *  PreSubmitted -> Waiting 
     *  Submitted -> Wait or Cancel&Return
     * 
     * Take Profit Order:
     *  Unknown, Filled, Canceled -> Can open position 
     *  PreSubmitted -> Do Nothing 
     *  Submitted -> Wait or cancel 
     *  Filled, Canceled -> Can open position
     * 
     */
    protected BotState getBotState() {
        if (openOrderStatus == null)
            return BotState.READY_TO_OPEN;

        if (openOrderStatus == OrderStatus.PendingSubmit)
            return BotState.PENDING;

        if (openOrderStatus == OrderStatus.PreSubmitted || openOrderStatus == OrderStatus.Submitted)
            return BotState.OPENNING_POSITION;

        if (openOrderStatus == OrderStatus.Cancelled)
            return BotState.READY_TO_OPEN;

        if (openOrderStatus == OrderStatus.Filled && (takeProfitOrderStatus == OrderStatus.PreSubmitted || takeProfitOrderStatus == OrderStatus.Submitted))
            return BotState.PROFIT_WAITING;

        if (openOrderStatus == OrderStatus.Filled && takeProfitOrderStatus == OrderStatus.Filled)
            return BotState.READY_TO_OPEN;

        if (openOrderStatus == OrderStatus.Filled && takeProfitOrderStatus == OrderStatus.Cancelled)
            return BotState.READY_TO_OPEN;

        throw new IllegalStateException("Unsupported combo of states: openOrderStatus=" + openOrderStatus + ", takeProfitOrderStatus=" + takeProfitOrderStatus);
    }

    public void shutdown() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    protected double openOrderAvgFillPrice;
    protected double takeProfitOrderAvgFillPrice;

    protected class OpenPositionOrderHandler implements IOrderHandler {
        private final Logger log = LogManager.getLogger(OpenPositionOrderHandler.class);

        @Override
        public void orderState(OrderState orderState) {
            String status = orderState.getStatus();
            log.info("OpenPosition: OrderState: {}", status);
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("OpenPosition: OderStatus: status {}, filled {}, remaining {}, whyHeld {}", status, filled, remaining, whyHeld);

            if (openOrderStatus == status) {
                return;
            }

            if (status == OrderStatus.Submitted) {
                openPositionOrderSubmitted();
            }

            if (status == OrderStatus.Filled && remaining < 0.01) {
                openOrderAvgFillPrice = avgFillPrice;
                openPositionOrderFilled(avgFillPrice);
            }

            // remap ApiCancelled to Canceled
            if (status == OrderStatus.ApiCancelled) {
                status = OrderStatus.Cancelled;
            }

            if (status == OrderStatus.Cancelled) {
                openPositionOrderCancelled();
            }

            // finally assign status
            if (status == OrderStatus.Filled
                    || status == OrderStatus.Cancelled
                    || status == OrderStatus.PreSubmitted
                    || status == OrderStatus.Submitted) {
                openOrderStatus = status;
            }
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
            log.error("Error: {} - {}", errorCode, errorMsg);
        }

    }

    protected class TakeProfitOrderHandler implements IOrderHandler {
        private final Logger log = LogManager.getLogger(TakeProfitOrderHandler.class);

        @Override
        public void orderState(OrderState orderState) {
            String status = orderState.getStatus();
            log.info("TakeProfit: OrderState: {}", status);
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("TakeProfit: OderStatus: status {}, filled {}, remaining {}, whyHeld {}", status, filled, remaining, whyHeld);

            if (takeProfitOrderStatus == status) {
                return;
            }

            if (status == OrderStatus.Submitted) {
                takeProfitOrderSubmitted();
            }

            if (status == OrderStatus.Filled && remaining < 0.01) {
                takeProfitOrderAvgFillPrice = avgFillPrice;
                takeProfitOrderFilled(avgFillPrice);
            }

            // remap ApiCancelled to Canceled
            if (status == OrderStatus.ApiCancelled) {
                status = OrderStatus.Cancelled;
            }

            if (status == OrderStatus.Cancelled) {
                takeProfitOrderCancelled();
            }

            // finally assign status
            if (status == OrderStatus.Filled
                    || status == OrderStatus.Cancelled
                    || status == OrderStatus.PreSubmitted
                    || status == OrderStatus.Submitted) {
                takeProfitOrderStatus = status;
            }
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
            log.error("Error: {} - {}", errorCode, errorMsg);
        }
    }

    protected class MocOrderHandler implements IOrderHandler {
        @Override
        public void orderState(OrderState orderState) {
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            if (status == OrderStatus.Filled && remaining < 0.01) {
                takeProfitOrderAvgFillPrice = avgFillPrice;
                takeProfitOrderFilled(avgFillPrice);
            }
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
        }
    }
}
