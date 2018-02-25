package jo.ib.controller.handler;

import jo.ib.controller.model.Bar;

// ----------------------------------------- Real-time bars --------------------------------------
public interface IRealTimeBarHandler {
    void realtimeBar(Bar bar); // time is in seconds since epoch
}