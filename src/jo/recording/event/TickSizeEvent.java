package jo.recording.event;

import com.ib.client.TickType;

public class TickSizeEvent extends BaseEvent {
    private TickType tickType;
    private int size;

    public TickSizeEvent(TickType tickType, int size) {
        super("TickSize");
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
