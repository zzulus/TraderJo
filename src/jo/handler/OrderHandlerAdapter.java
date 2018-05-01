package jo.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Order;
import com.ib.client.OrderState;

import jo.model.OrderStatusInput;

public class OrderHandlerAdapter implements IOrderHandler {
    private final Logger log = LogManager.getLogger(this.getClass());

    @Override
    public void orderState(Order order, OrderState orderState) {
    }

    @Override
    public void orderStatus(OrderStatusInput input) {
    }

    @Override
    public void handle(int errorCode, String errorMsg) {
        log.error("Error: {} - {}", errorCode, errorMsg);
    }

}
