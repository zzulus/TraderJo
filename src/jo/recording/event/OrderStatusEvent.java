package jo.recording.event;

import com.ib.client.OrderStatus;

import jo.model.OrderStatusInput;

public class OrderStatusEvent extends AbstractEvent {
    public static final String TYPE = "OrderStatus";
    private int orderId;
    private OrderStatus status;
    private double filled;
    private double remaining;
    private double avgFillPrice;
    private long permId;
    private int parentId;
    private double lastFillPrice;
    private int clientId;
    private String whyHeld;

    public OrderStatusEvent() {
        super(TYPE);
    }

    public OrderStatusEvent(OrderStatusInput input) {
        super(TYPE);
        this.orderId = input.getOrderId();
        this.status = input.getStatus();
        this.filled = input.getFilled();
        this.remaining = input.getRemaining();
        this.avgFillPrice = input.getAvgFillPrice();
        this.permId = input.getPermId();
        this.parentId = input.getParentId();
        this.lastFillPrice = input.getLastFillPrice();
        this.clientId = input.getClientId();
        this.whyHeld = input.getWhyHeld();
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public double getFilled() {
        return filled;
    }

    public void setFilled(double filled) {
        this.filled = filled;
    }

    public double getRemaining() {
        return remaining;
    }

    public void setRemaining(double remaining) {
        this.remaining = remaining;
    }

    public double getAvgFillPrice() {
        return avgFillPrice;
    }

    public void setAvgFillPrice(double avgFillPrice) {
        this.avgFillPrice = avgFillPrice;
    }

    public long getPermId() {
        return permId;
    }

    public void setPermId(long permId) {
        this.permId = permId;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public double getLastFillPrice() {
        return lastFillPrice;
    }

    public void setLastFillPrice(double lastFillPrice) {
        this.lastFillPrice = lastFillPrice;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getWhyHeld() {
        return whyHeld;
    }

    public void setWhyHeld(String whyHeld) {
        this.whyHeld = whyHeld;
    }

}
