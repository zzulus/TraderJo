package jo.command;

import jo.app.TraderApp;
import jo.controller.IBService;

public interface AppCommand {
    void execute(IBService ib, TraderApp app);
}
