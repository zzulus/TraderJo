package jo.command;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jo.controller.IBroker;
import jo.model.Context;
import jo.util.AsyncExec;

public class GetNextOrderIdCommand implements AppCommand {
    private static final Logger log = LogManager.getLogger(GetNextOrderIdCommand.class);

    @Override
    public void execute(Context ctx) {
        log.info("Start order id updater");
        AsyncExec.startDaemonThread(() -> requestNextOrderIdLoop(ctx.getIb()));
    }

    public void requestNextOrderIdLoop(IBroker ib) {
        while (true) {
            try {
                ib.reqNextOrderId();
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
