package jo.bot;

import jo.controller.IBroker;
import jo.model.IApp;

public interface Bot {
    void init(IBroker ib, IApp app);

    void runLoop();

    default void start() {

    }
}
