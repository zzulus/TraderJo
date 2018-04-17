package jo.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;

public class OrderHandlerAdapter implements IOrderHandler {
    private final Logger log = LogManager.getLogger(this.getClass());

    @Override
    public void orderState(Order order, OrderState orderState) {
    }

    @Override
    public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
    }

    @Override
    public void handle(int errorCode, String errorMsg) {
        log.error("Error: {} - {}", errorCode, errorMsg);
    }

}
