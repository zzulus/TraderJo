package jo.bot;

import jo.app.TraderApp;
import jo.controller.IBService;

public interface Bot {
    void start(IBService ib, TraderApp app);
}
