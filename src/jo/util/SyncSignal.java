package jo.util;

public class SyncSignal {
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
                    stop();
                }
            }
        }
        return active;
    }

    public void stop() {
        active = false;
        signalAll();
    }
}
