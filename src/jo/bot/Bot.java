package jo.bot;

import jo.model.Context;

public interface Bot {
    void init(Context ctx);

    void runLoop();

    void start();
}
