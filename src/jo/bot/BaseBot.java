package jo.bot;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderStatus;

import jo.filter.Filter;
import jo.handler.OrderHandlerAdapter;
import jo.model.MarketData;
import jo.position.PositionSizeStrategy;
import jo.util.PnLLogger;

public abstract class BaseBot implements Bot {
    protected final Logger log = LogManager.getLogger(this.getClass());
    protected final Logger pnlLog = LogManager.getLogger("PNL");
    protected final Contract contract;
    protected final PositionSizeStrategy positionSize;
    protected MarketData md;

    protected Filter positionFilter;

    protected double openAvgFillPrice;
    protected double takeProfitOrderAvgFillPrice;
    protected int currentPosition = 0;

    protected Order openOrder;
    protected Order closeOrder;
    protected Order mocOrder;

    protected OrderStatus openOrderStatus = null;
    protected OrderStatus closeOrderStatus = null;
    protected OrderStatus stopLossOrderStatus = null;

    protected Thread thread;

    public BaseBot(Contract contract, PositionSizeStrategy positionSize) {
        checkNotNull(contract);
        checkNotNull(positionSize);
        this.contract = contract;
        this.positionSize = positionSize;
    }

    protected void openPositionOrderSubmitted() {

    }

    protected void openPositionOrderFilled(double avgFillPrice) {

    }

    protected void openPositionOrderCancelled() {
    }

    protected void takeProfitOrderSubmitted() {

    }

    protected void closePositionOrderFilled(double closeAvgFillPrice) {
        PnLLogger.log(contract, openOrder.action(), openOrder.totalQuantity(), openAvgFillPrice, closeAvgFillPrice);
    }

    protected void closePositionOrderCancelled() {
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

        if (openOrderStatus == OrderStatus.Filled && (closeOrderStatus == OrderStatus.PreSubmitted || closeOrderStatus == OrderStatus.Submitted))
            //if (openOrderStatus == OrderStatus.Filled && currentPosition > 0)
            return BotState.PROFIT_WAITING;

        if (openOrderStatus == OrderStatus.Filled && closeOrderStatus == OrderStatus.Filled)
            //if (openOrderStatus == OrderStatus.Filled && currentPosition == 0)
            return BotState.READY_TO_OPEN;

        if (openOrderStatus == OrderStatus.Filled && closeOrderStatus == OrderStatus.Cancelled)
            return BotState.READY_TO_OPEN;

        throw new IllegalStateException("Unsupported combo of states: openOrderStatus=" + openOrderStatus + ", takeProfitOrderStatus=" + closeOrderStatus);
    }

    public void shutdown() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    protected class OpenPositionOrderHandler extends OrderHandlerAdapter {
        private final Logger log = LogManager.getLogger(OpenPositionOrderHandler.class);

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("OpenPosition: OderStatus: status {}, filled {}, remaining {}, avgFillPrice {}, whyHeld {}",
                    status, filled, remaining, avgFillPrice, whyHeld);

            if (openOrderStatus == status) {
                return;
            }

            if (status == OrderStatus.Submitted) {
                openPositionOrderSubmitted();
            }

            if (status == OrderStatus.Filled && remaining < 0.01) {
                openAvgFillPrice = avgFillPrice;
                currentPosition = (int) filled; // TODO Deal with partially filled orders 
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

    }

    protected class ClosePositionOrderHandler extends OrderHandlerAdapter {
        private final Logger log = LogManager.getLogger(ClosePositionOrderHandler.class);

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("ClosePosition: OderStatus: status {}, filled {}, remaining {}, avgFillPrice {}, whyHeld {}",
                    status, filled, remaining, avgFillPrice, whyHeld);

            if (closeOrderStatus == status) {
                return;
            }

            if (status == OrderStatus.Submitted) {
                takeProfitOrderSubmitted();
            }

            if (status == OrderStatus.Filled && remaining < 0.01) {
                takeProfitOrderAvgFillPrice = avgFillPrice;
                currentPosition -= filled;
                closePositionOrderFilled(avgFillPrice);
            }

            // remap ApiCancelled to Canceled
            if (status == OrderStatus.ApiCancelled) {
                status = OrderStatus.Cancelled;
            }

            if (status == OrderStatus.Cancelled) {
                closePositionOrderCancelled();
            }

            // finally assign status
            if (status == OrderStatus.Filled
                    || status == OrderStatus.Cancelled
                    || status == OrderStatus.PreSubmitted
                    || status == OrderStatus.Submitted) {
                closeOrderStatus = status;
            }
        }
    }

    protected class StopLossOrderHandler extends OrderHandlerAdapter {
        private final Logger log = LogManager.getLogger(StopLossOrderHandler.class);

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.info("StopLoss: OderStatus: status {}, filled {}, remaining {}, whyHeld {}", status, filled, remaining, whyHeld);

            if (stopLossOrderStatus == status) {
                return;
            }

            if (status == OrderStatus.Submitted) {
                //takeProfitOrderSubmitted();
            }

            if (status == OrderStatus.Filled && remaining < 0.01) {
                takeProfitOrderAvgFillPrice = avgFillPrice;
                currentPosition -= filled;
                closePositionOrderFilled(avgFillPrice);
            }

            // remap ApiCancelled to Canceled
            if (status == OrderStatus.ApiCancelled) {
                status = OrderStatus.Cancelled;
            }

            if (status == OrderStatus.Cancelled) {
                closePositionOrderCancelled();
            }

            // finally assign status
            if (status == OrderStatus.Filled
                    || status == OrderStatus.Cancelled
                    || status == OrderStatus.PreSubmitted
                    || status == OrderStatus.Submitted) {
                stopLossOrderStatus = status;
            }
        }
    }

    protected class MocOrderHandler extends OrderHandlerAdapter {
        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            if (status == OrderStatus.Filled && remaining < 0.01) {
                takeProfitOrderAvgFillPrice = avgFillPrice;
                currentPosition -= filled;
                closePositionOrderFilled(avgFillPrice);
            }
        }
    }
}
