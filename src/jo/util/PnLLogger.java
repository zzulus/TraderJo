package jo.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Types.Action;

public class PnLLogger {
    public final static Logger pnlLog = LogManager.getLogger("PNL");
    private static double totalPnL = 0;

    public static synchronized void log(Contract contract, Action action, double totalQuantity, double openAvgFillPrice, double closeAvgFillPrice) {
        double priceDiff = closeAvgFillPrice - openAvgFillPrice;
        double longPnL = totalQuantity * priceDiff;
        boolean longTrade = action == Action.BUY;
        double pnl = longTrade ? longPnL : -longPnL;

        totalPnL += pnl;

        pnlLog.info("{} {}, qty {}, open {}, close {}, P&L {}. Total P&L {}",
                longTrade ? "Long" : "Short",
                contract.symbol(),
                (int) totalQuantity,
                Formats.fmt(openAvgFillPrice),
                Formats.fmt(closeAvgFillPrice),
                Formats.fmt(pnl),
                Formats.fmt(totalPnL));
    }

}
