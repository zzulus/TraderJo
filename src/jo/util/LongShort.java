package jo.util;

import com.ib.client.Order;
import com.ib.client.Types.Action;

public enum LongShort {
    LONG, SHORT;

    public static LongShort byOpenOrder(Order order) {
        return byOpenOrder(order.action());
    }

    public static LongShort byCloseOrder(Order order) {
        return byCloseOrder(order.action());
    }

    public static LongShort byOpenOrder(Action action) {
        if (action == Action.BUY)
            return LONG;

        if (action == Action.SELL)
            return SHORT;

        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    public static LongShort byCloseOrder(Action action) {
        if (action == Action.SELL)
            return LONG;

        if (action == Action.BUY)
            return SHORT;

        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    public static boolean isLongByOpen(Action action) {
        return byOpenOrder(action) == LONG;
    }

    public static boolean isShortByOpen(Action action) {
        return byOpenOrder(action) == SHORT;
    }

    public static boolean isLongByClose(Action action) {
        return byCloseOrder(action) == LONG;
    }

    public static boolean isShortByClose(Action action) {
        return byCloseOrder(action) == LONG;
    }
}
