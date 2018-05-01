package jo.bot;

import jo.controller.IApp;
import jo.controller.IBroker;

public interface Bot {
    void init(IBroker ib, IApp app);

    void runLoop();

    void start();
}
