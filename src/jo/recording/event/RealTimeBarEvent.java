package jo.recording.event;

import jo.model.Bar;

public class RealTimeBarEvent extends AbstractEvent {
    public static final String TYPE = "RealTimeBar";
    private Bar bar;

    public RealTimeBarEvent() {
        super(TYPE);
    }

    public RealTimeBarEvent(Bar bar) {
        super(TYPE);
        this.bar = bar;
    }

    public Bar getBar() {
        return bar;
    }

    public void setBar(Bar bar) {
        this.bar = bar;
    }
}
