package jo.command;

import jo.app.App;
import jo.controller.IBService;

public interface AppCommand {
    void execute(IBService ib, App app);
}
