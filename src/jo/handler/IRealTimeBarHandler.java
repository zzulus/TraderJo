package jo.handler;

import jo.model.Bar;

// ----------------------------------------- Real-time bars --------------------------------------
public interface IRealTimeBarHandler {
    void realtimeBar(Bar bar); // time is in seconds since epoch
}