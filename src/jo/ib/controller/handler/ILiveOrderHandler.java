package jo.ib.controller.handler;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;

// ---------------------------------------- Live order handling ----------------------------------------
/**
 * This interface is for downloading and receiving events for all live orders. Compare to IOrderHandler.
 */
public interface ILiveOrderHandler {
    void openOrder(Contract contract, Order order, OrderState orderState);

    void openOrderEnd();

    void orderStatus(int orderId, OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld);

    void handle(int orderId, int errorCode, String errorMsg); // add permId?
}