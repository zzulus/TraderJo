package jo.recording.event;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;

public class OpenOrderEvent extends BaseEvent {
    private Contract contract;
    private Order order;
    private OrderState orderState;

    public OpenOrderEvent(Contract contract, Order order, OrderState orderState) {
        super("OpenOrder");
        this.contract = contract;
        this.order = order;
        this.orderState = orderState;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderState getOrderState() {
        return orderState;
    }

    public void setOrderState(OrderState orderState) {
        this.orderState = orderState;
    }

}
