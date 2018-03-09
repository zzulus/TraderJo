package jo.recording;

import jo.controller.IBroker;

public interface Recorder {
    void start(IBroker ib);

    void stop();

}
