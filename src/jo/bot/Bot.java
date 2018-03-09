package jo.bot;

import jo.app.IApp;
import jo.controller.IBroker;

public interface Bot {
    void start(IBroker ib, IApp app);
}
