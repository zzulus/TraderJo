package jo.trade;

import java.util.Date;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.Types.Action;

public class Trade {
    private Contract contract;
    private int orderId;
    private int parentOrderId;
    private Action action;
    private int totalQuantity;
    private String tradeRef;
    private double avgFillPrice = Double.MIN_VALUE;
    private Date fillTime;

    public static Trade of(Contract contract, Order order) {
        Trade trade = new Trade();
        trade.setContract(contract);
        trade.setTradeRef(order.orderRef());
        trade.setOrderId(order.orderId());
        trade.setParentOrderId(order.parentId());
        trade.setAction(order.action());
        trade.setTotalQuantity((int) order.totalQuantity());
        return trade;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getParentOrderId() {
        return parentOrderId;
    }

    public void setParentOrderId(int parentOrderId) {
        this.parentOrderId = parentOrderId;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public double getAvgFillPrice() {
        return avgFillPrice;
    }

    public void setAvgFillPrice(double avgFillPrice) {
        this.avgFillPrice = avgFillPrice;
        this.fillTime = new Date();
    }

    public boolean hasAvgFillPrice() {
        return avgFillPrice != Double.MIN_VALUE;
    }

    public String getTradeRef() {
        return tradeRef;
    }

    public void setTradeRef(String tradeRef) {
        this.tradeRef = tradeRef;
    }

    public Date getFillTime() {
        return fillTime;
    }

    @Override
    public String toString() {
        return "Trade [symbol=" + contract.symbol()
                + ", tradeRef=" + tradeRef
                + ", orderId=" + orderId
                + ", parentOrderId=" + parentOrderId
                + ", action=" + action
                + ", totalQuantity=" + totalQuantity
                + ", avgFillPrice=" + avgFillPrice
                + "]";
    }

}
