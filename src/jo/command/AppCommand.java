package jo.command;

import jo.app.IApp;
import jo.controller.IBroker;

public interface AppCommand {
    void execute(IBroker ib, IApp app);
}
