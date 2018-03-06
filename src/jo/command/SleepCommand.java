package jo.command;

import jo.app.TraderApp;
import jo.controller.IBService;

public class SleepCommand implements AppCommand {
    private long sleepMs;

    public SleepCommand(long sleepMs) {
        this.sleepMs = sleepMs;
    }

    @Override
    public void execute(IBService ib, TraderApp app) {
        try {
            Thread.currentThread().sleep(sleepMs);
        } catch (InterruptedException e) {

        }
    }

}
