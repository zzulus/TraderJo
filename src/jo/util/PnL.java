package jo.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Types.Action;

public class PnL {
    public final static Logger pnlLog = LogManager.getLogger("PNL");
    private static double grandTotalPnL = 0;
    private static Map<String, MutableDouble> pnlByContract = new ConcurrentHashMap<>();
    private static Set<Integer> reportedOrders = new ConcurrentHashSet<>();

    public static synchronized void log(Contract contract, int openOrderId, Action action, double totalQuantity, double openAvgFillPrice, double closeAvgFillPrice) {
        String symbol = contract.symbol();
        boolean longTrade = action == Action.BUY;

        if (!reportedOrders.add(openOrderId)) {
            pnlLog.info("Missinform for {} {}. Open orderId {}", longTrade ? "Long" : "Short", symbol, openOrderId);
        }

        double priceDiff = closeAvgFillPrice - openAvgFillPrice;
        double longPnL = totalQuantity * priceDiff;
        double pnl = longTrade ? longPnL : -longPnL;

        MutableDouble contractPnl = pnlByContract.computeIfAbsent(symbol, ignore -> new MutableDouble());

        grandTotalPnL += pnl;
        contractPnl.add(pnl);

        pnlLog.info("{} {}, qty {}, open {}, close {}, P&L {}. Contract P&L {}. Grand total P&L {}",
                longTrade ? "Long" : "Short",
                symbol,
                (int) totalQuantity,
                Formats.fmt(openAvgFillPrice),
                Formats.fmt(closeAvgFillPrice),
                Formats.fmt(pnl),
                Formats.fmt(contractPnl.doubleValue()),
                Formats.fmt(grandTotalPnL));

        if (grandTotalPnL < -100) {
            System.err.println("Grand total loss > $100, halt");
            System.exit(-1);
        }
    }
}
