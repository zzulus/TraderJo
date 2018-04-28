package jo.handler;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

// ---------------------------------------- Live order handling ----------------------------------------
/**
 * This interface is for downloading and receiving events for all live orders. Compare to IOrderHandler.
 */
public interface ILiveOrderHandler {
    void openOrder(Contract contract, Order order, OrderState orderState);

    void openOrderEnd();

    void orderStatus(OrderStatusInput input);

    void handle(int orderId, int errorCode, String errorMsg);
}