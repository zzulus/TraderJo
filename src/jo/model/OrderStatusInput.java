package jo.model;

import com.ib.client.OrderStatus;

public class OrderStatusInput {
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

    @Override
    public String toString() {
        return "OrderStatusInput [orderId=" + orderId + ", status=" + status + ", filled=" + filled + ", remaining=" + remaining + ", avgFillPrice=" + avgFillPrice + ", permId=" + permId + ", parentId="
                + parentId + ", lastFillPrice=" + lastFillPrice + ", clientId=" + clientId + ", whyHeld=" + whyHeld + "]";
    }

}
