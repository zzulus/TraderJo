package jo.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AsyncVal<V> extends CompletableFuture<V> {
    public static <V> AsyncVal<V> create() {
        return new AsyncVal<>();
    }

    @Override
    public V get() {
        try {
            return super.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    public void set(V value) {
        complete(value);
    }
}
