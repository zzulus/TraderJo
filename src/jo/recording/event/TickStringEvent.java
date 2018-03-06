package jo.recording.event;

import com.ib.client.TickType;

public class TickStringEvent extends BaseEvent {
    private TickType tickType;
    private String value;

    public TickStringEvent(TickType tickType, String value) {
        super("TickString");
        this.tickType = tickType;
        this.value = value;
    }

    public TickType getTickType() {
        return tickType;
    }

    public void setTickType(TickType tickType) {
        this.tickType = tickType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
