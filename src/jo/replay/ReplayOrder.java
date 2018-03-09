package jo.replay;

import com.ib.client.Contract;
import com.ib.client.Order;

import jo.handler.IOrderHandler;

public class ReplayOrder {
    private Contract contract;
    private Order order;
    private IOrderHandler handler;

    public ReplayOrder(Contract contract, Order order, IOrderHandler handler) {
        this.contract = contract;
        this.order = order;
        this.handler = handler;
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

    public IOrderHandler getHandler() {
        return handler;
    }

    public void setHandler(IOrderHandler handler) {
        this.handler = handler;
    }

}
