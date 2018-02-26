package jo.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Async {
    private static final long TIMEOUT = 60L;
    private static final int CORE_THREADS = 5;
    private static final ExecutorService POOL = new ThreadPoolExecutor(CORE_THREADS, Integer.MAX_VALUE, TIMEOUT, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

    public static void execute(Runnable r) {
        POOL.execute(r);
    }

    public static Future<?> submit(Runnable r) {
        return POOL.submit(r);
    }

    public static <T> Future<T> submit(Callable<T> c) {
        return POOL.submit(c);
    }
}
