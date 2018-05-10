package jo.replay;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Order;

import jo.controller.BrokerAdapter;
import jo.handler.IOrderHandler;
import jo.recording.event.AbstractEvent;

public class ReplayBroker extends BrokerAdapter {
    private static final Logger log = LogManager.getLogger(ReplayBroker.class);
    private final ReplayContext ctx;

    public ReplayBroker(ReplayContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void placeOrModifyOrder(Contract contract, Order order, IOrderHandler handler) {
        if (order.orderId() == 0) {
            order.orderId(getNextOrderId());
        }

        ctx.getOrderManager().placeOrModifyOrder(contract, order, handler);

        log.info("placeOrModifyOrder: {}: {} {} {} for {}",
                contract.symbol(), order.action(), order.totalQuantity(), order.orderType(), order.lmtPrice());
    }

    @Override
    public void cancelAllOrders() {
        ctx.getOrderManager().cancelAllOrders();
    }

    @Override
    public void cancelOrder(int orderId) {
        ctx.getOrderManager().cancelOrder(orderId);
    }

    public void handleReplayEvent(Contract contract, AbstractEvent event) {
        ctx.getOrderManager().tick(contract, event);
    }
}
