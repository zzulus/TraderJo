package jo.recording;

import jo.controller.IBService;

public interface Recorder {
    void start(IBService ib);

    void stop();

}
