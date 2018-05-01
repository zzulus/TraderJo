package jo.handler;

import com.ib.client.Order;
import com.ib.client.OrderState;

import jo.model.OrderStatusInput;

// ---------------------------------------- Trading and Option Exercise ----------------------------------------
/**
 * This interface is for receiving events for a specific order placed from the API. Compare to ILiveOrderHandler.
 */
public interface IOrderHandler {
    void orderState(Order order, OrderState orderState);

    void orderStatus(OrderStatusInput input);

    void handle(int errorCode, String errorMsg);
}