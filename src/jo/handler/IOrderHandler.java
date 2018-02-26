package jo.handler;

import com.ib.client.OrderState;
import com.ib.client.OrderStatus;

// ---------------------------------------- Trading and Option Exercise ----------------------------------------
/**
 * This interface is for receiving events for a specific order placed from the API. Compare to ILiveOrderHandler.
 */
public interface IOrderHandler {
    void orderState(OrderState orderState);

    void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld);

    void handle(int errorCode, String errorMsg);
}