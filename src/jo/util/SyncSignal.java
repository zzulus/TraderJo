package jo.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SyncSignal {
    private final Logger log = LogManager.getLogger(SyncSignal.class);

    private final Object monitor = new Object();
    private boolean active = true;

    public void signalAll() {
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    public boolean waitForSignal() {
        if (active) {
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
        }
        return active;
    }

    public void stop() {
        log.error("Stop Called");
        active = false;
        signalAll();
    }
}
