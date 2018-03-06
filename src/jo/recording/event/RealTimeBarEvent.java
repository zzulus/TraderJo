package jo.recording.event;

import jo.model.Bar;

public class RealTimeBarEvent extends BaseEvent {
    private Bar bar;

    public RealTimeBarEvent(Bar bar) {
        super("RealTimeBar");
        this.bar = bar;
    }

    public Bar getBar() {
        return bar;
    }

    public void setBar(Bar bar) {
        this.bar = bar;
    }
}
