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
    private ReplayApp app;
    private ReplayOrderManager orderManager = new ReplayOrderManager();

    @Override
    public void placeOrModifyOrder(Contract contract, Order order, IOrderHandler handler) {
        if (order.orderId() == 0) {
            order.orderId(getNextOrderId());
        }

        orderManager.placeOrModifyOrder(contract, order, handler);

        log.info("placeOrModifyOrder: {}: {} {} {} for {}",
                contract.symbol(), order.action(), order.totalQuantity(), order.orderType(), order.lmtPrice());
    }

    @Override
    public void cancelAllOrders() {
        orderManager.cancelAllOrders();
    }

    @Override
    public void cancelOrder(int orderId) {
        orderManager.cancelOrder(orderId);
    }

    public void setApp(ReplayApp app) {
        this.app = app;
        orderManager.setApp(app);
    }

    // event bus?
    public ReplayOrderManager getOrderManager() {
        return orderManager;
    }

    public void handleReplayEvent(Contract contract, AbstractEvent event) {
        orderManager.tick(contract, event);
    }
}
