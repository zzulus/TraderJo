package jo.bot;

import jo.app.IApp;
import jo.controller.IBroker;

public interface Bot {
    void init(IBroker ib, IApp app);

    void runLoop();

    default void start() {

    }
}
