package jo.util;

// a bit better copy of EJavaSignal
public class SyncSignal {
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
                    // ignore
                }
            }

            open = false;
        }
    }
}
