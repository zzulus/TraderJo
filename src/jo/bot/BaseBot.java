package jo.bot;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderStatus;

import jo.controller.IBroker;
import jo.handler.OrderHandlerAdapter;
import jo.model.Context;
import jo.model.MarketData;
import jo.model.OrderStatusInput;
import jo.position.PositionSizeStrategy;
import jo.trade.TradeRef;

public abstract class BaseBot implements Bot {
    protected Logger log = LogManager.getLogger(this.getClass());
    protected Contract contract;
    protected PositionSizeStrategy positionSize;
    protected MarketData md;
    protected Context ctx;
    protected IBroker ib;

    protected Order openOrder;
    protected Order closeOrder;

    protected OrderStatus openOrderStatus = null;
    protected OrderStatus closeOrderStatus = null;

    protected Thread thread;

    public BaseBot(Contract contract, PositionSizeStrategy positionSize) {
        checkNotNull(contract);
        checkNotNull(positionSize);
        this.contract = contract;
        this.positionSize = positionSize;
    }

    protected void openPositionOrderSubmitted() {

    }

    protected void openPositionOrderFilled(int orderId, double avgFillPrice) {
        ctx.getTradeBook().addExecution(orderId, avgFillPrice);
    }

    protected void openPositionOrderCancelled() {
    }

    protected void takeProfitOrderSubmitted() {

    }

    protected void closePositionOrderFilled(int orderId, double avgFillPrice) {
        ctx.getTradeBook().addExecution(orderId, avgFillPrice);
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

    protected String updateTradeRef() {
        String tradeRef = TradeRef.create(contract.symbol());
        Thread.currentThread().setName(tradeRef);
        return tradeRef;
    }

    protected class OpenPositionOrderHandler extends OrderHandlerAdapter {
        @Override
        public void orderStatus(OrderStatusInput input) {
            if (openOrder.orderId() != input.getOrderId()) {
                log.warn("OpenPositionOrderHandler: reported order {} != current open order {}",
                        input.getOrderId(), openOrder.orderId());
                return;
            }

            OrderStatus status = input.getStatus();
            final double filled = input.getFilled();
            final double remaining = input.getRemaining();
            final String whyHeld = input.getWhyHeld();
            final double avgFillPrice = input.getAvgFillPrice();

            log.info("OpenPosition: OderStatus: status {}, filled {}, remaining {}, avgFillPrice {}, whyHeld {}",
                    status, filled, remaining, avgFillPrice, whyHeld);

            if (openOrderStatus == status) {
                return;
            }

            if (status == OrderStatus.Submitted) {
                openPositionOrderSubmitted();
            }

            if (status == OrderStatus.Filled && remaining < 0.01) {
                openPositionOrderFilled(input.getOrderId(), avgFillPrice);
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
        @Override
        public void orderStatus(OrderStatusInput input) {
            if (closeOrder.orderId() != input.getOrderId()) {
                log.warn("ClosePositionOrderHandler: reported order {} != current close order {}",
                        input.getOrderId(), closeOrder.orderId());
                return;
            }

            OrderStatus status = input.getStatus();
            final double filled = input.getFilled();
            final double remaining = input.getRemaining();
            final String whyHeld = input.getWhyHeld();
            final double avgFillPrice = input.getAvgFillPrice();

            log.info("ClosePosition: OderStatus: status {}, filled {}, remaining {}, avgFillPrice {}, whyHeld {}",
                    status, filled, remaining, avgFillPrice, whyHeld);

            if (closeOrderStatus == status) {
                return;
            }

            if (status == OrderStatus.Submitted) {
                takeProfitOrderSubmitted();
            }

            if (status == OrderStatus.Filled && remaining < 0.01) {
                closePositionOrderFilled(input.getOrderId(), avgFillPrice);
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

    protected class MocOrderHandler extends OrderHandlerAdapter {
        @Override
        public void orderStatus(OrderStatusInput input) {
            OrderStatus status = input.getStatus();
            final double filled = input.getFilled();
            final double remaining = input.getRemaining();
            final double avgFillPrice = input.getAvgFillPrice();

            if (status == OrderStatus.Filled && remaining < 0.01) {
                closePositionOrderFilled(input.getOrderId(), avgFillPrice);
            }
        }
    }
}
