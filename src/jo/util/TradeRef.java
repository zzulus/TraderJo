package jo.util;

import java.util.concurrent.atomic.AtomicLong;

public class TradeRef {
    private static final AtomicLong ref = new AtomicLong(0);

    public static String create(String prefix) {
        return prefix + "-" + ref.getAndIncrement() + "-";
    }
}
