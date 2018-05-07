package jo.util;

public class AsyncExec {
    private AsyncExec() {

    }

    public static Thread startThread(String name, Runnable target) {
        Thread t = new Thread(target, name);
        t.start();
        return t;
    }

    public static Thread startDaemonThread(String name, Runnable target) {
        Thread t = new Thread(target, name);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public static Thread startDaemonThread(Runnable target) {
        Thread t = new Thread(target);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public static Thread execute(Runnable target) {
        Thread t = new Thread(target);
        t.start();
        return t;
    }
}
