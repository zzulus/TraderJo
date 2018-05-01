package jo.command;

import jo.controller.IApp;
import jo.controller.IBroker;

public interface AppCommand {
    void execute(IBroker ib, IApp app);
}
