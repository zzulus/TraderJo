package jo.recording.event;

import com.ib.client.TickType;

public class TickPriceEvent extends AbstractEvent {
    public static final String TYPE = "TickPrice";
    private TickType tickType;
    private double price;

    public TickPriceEvent() {
        super(TYPE);
    }

    public TickPriceEvent(TickType tickType, double price) {
        super(TYPE);
        this.tickType = tickType;
        this.price = price;
    }

    public TickType getTickType() {
        return tickType;
    }

    public void setTickType(TickType tickType) {
        this.tickType = tickType;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
