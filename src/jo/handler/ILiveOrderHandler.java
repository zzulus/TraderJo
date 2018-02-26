package jo.handler;

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

    /**
     * Order status.
     *
     * @param orderId
     *            the order id
     * @param status
     *            the status
     * @param filled
     *            number of stocks filled
     * @param remaining
     *            number of stocks remaining to be filled
     * @param avgFillPrice
     *            the avg fill price
     * @param permId
     *            the perm id
     * @param parentId
     *            the parent id
     * @param lastFillPrice
     *            the last fill price
     * @param clientId
     *            the client id
     * @param whyHeld
     *            the why held
     */
    void orderStatus(int orderId, OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld);

    void handle(int orderId, int errorCode, String errorMsg); // add permId?
}