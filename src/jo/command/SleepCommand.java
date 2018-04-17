package jo.command;

import jo.controller.IBroker;
import jo.model.IApp;

public class SleepCommand implements AppCommand {
    private long sleepMs;

    public SleepCommand(long sleepMs) {
        this.sleepMs = sleepMs;
    }

    @Override
    public void execute(IBroker ib, IApp app) {
        try {
            Thread.currentThread().sleep(sleepMs);
        } catch (InterruptedException e) {

        }
    }

}
