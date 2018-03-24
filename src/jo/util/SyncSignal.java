package jo.util;

import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.Uninterruptibles;

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

    public static void main(String[] args) {
        SyncSignal s = new SyncSignal();

        for (int k = 0; k < 10; k++) {
            AsyncExec.startThread("" + k, () -> {
                while (s.waitForSignal()) {
                    System.out.println(Thread.currentThread().getName());
                }
            });
        }

        for (int i = 0; i < 5; i++) {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            System.out.println("Bom");
            s.signalAll();
        }

        s.stop();
    }
}
