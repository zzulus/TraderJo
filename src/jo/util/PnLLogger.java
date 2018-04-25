package jo.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Types.Action;

public class PnLLogger {
    public final static Logger pnlLog = LogManager.getLogger("PNL");
    private static double grandTotalPnL = 0;
    private static Map<String, MutableDouble> pnlByContract = new HashMap<>();

    public static synchronized void log(Contract contract, Action action, double totalQuantity, double openAvgFillPrice, double closeAvgFillPrice) {
        String symbol = contract.symbol();
        double priceDiff = closeAvgFillPrice - openAvgFillPrice;
        double longPnL = totalQuantity * priceDiff;
        boolean longTrade = action == Action.BUY;
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
    }

}
