package jo.recording.event;

import com.ib.client.TickType;

public class TickSizeEvent extends AbstractEvent {
    public static final String TYPE = "TickSize";
    private TickType tickType;
    private int size;

    public TickSizeEvent() {
        super(TYPE);
    }

    public TickSizeEvent(TickType tickType, int size) {
        super(TYPE);
        this.tickType = tickType;
        this.size = size;
    }

    public TickType getTickType() {
        return tickType;
    }

    public void setTickType(TickType tickType) {
        this.tickType = tickType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
