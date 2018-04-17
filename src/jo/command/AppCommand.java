package jo.command;

import jo.controller.IBroker;
import jo.model.IApp;

public interface AppCommand {
    void execute(IBroker ib, IApp app);
}
