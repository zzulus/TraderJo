package jo.command;

import jo.model.Context;

public interface AppCommand {
    void execute(Context ctx);
}
