package jo.trade;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

public class TradeRef {
    private static final AtomicLong ref = new AtomicLong(0);

    public static String create(String symbol) {
        String tradeNum = Objects.toString(ref.getAndIncrement());
        String tradeNumPad = StringUtils.leftPad(tradeNum, 4, '0');

        return tradeNumPad + '.' + symbol;
    }
}
