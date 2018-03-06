package jo.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// a bit better copy of EJavaSignal
public class SyncSignal {
    private static final Logger log = LogManager.getLogger(SyncSignal.class);
    private Object monitor = new Object();
    private boolean open = false;

    public void signalAll() {
        synchronized (monitor) {
            open = true;
            monitor.notifyAll();
        }
    }

    public void waitForSignal() {
        synchronized (monitor) {
            while (!open) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    log.error(e, e);
                }
            }

            open = false;
        }
    }
}
