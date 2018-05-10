package jo.trade;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Objects;
import com.ib.client.Contract;
import com.ib.client.Order;

import jo.util.Formats;
import jo.util.LongShort;

public class TradeBook {
    private static final int HALT_STOP_LOSS = -50;
    public static final Logger log = LogManager.getLogger("PNL");
    public static final Logger csv = LogManager.getLogger("PNLCSV");
    static {
        csv.info("Date Trade Action Symbol Qty OpenPrice ClosePrice OpenTime CloseTime P&L ContractP&L Total");
    }

    private volatile double grandTotalPnL = 0;
    private Map<String, MutableDouble> pnlByContract = new ConcurrentHashMap<>();
    private Map<Integer, Trade> trades = new ConcurrentHashMap<>();

    public synchronized void addOrder(Contract contract, Order order) {
        int orderId = order.orderId();

        if (trades.containsKey(orderId)) {
            log.warn("addOrder: Already have order {} for {}", orderId, contract.symbol());
            return;
        }

        Trade trade = Trade.of(contract, order);
        trades.put(orderId, trade);
    }

    public synchronized void addExecution(int orderId, double avgFillPrice) {
        Trade trade = trades.get(orderId);

        // sanity check
        if (trade == null) {
            log.warn("addTrade: unknown order {}", orderId);
            throw new IllegalStateException("addTrade: unknown order " + orderId);
        }

        if (trade.hasAvgFillPrice()) {
            log.warn("addTrade: duplication of order execution {}", orderId);
            return;
        }

        trade.setAvgFillPrice(avgFillPrice);

        int parentOrderId = trade.getParentOrderId();
        if (parentOrderId == 0) {
            return;
        }

        Trade openTrade = trades.get(parentOrderId);
        Trade closeTrade = trade; // just for convenience
        if (openTrade == null) {
            log.warn("Cannot find open (parent) order {} for order {}", parentOrderId, orderId);
            return;
        }

        calculatePnL(openTrade, closeTrade);

        if (grandTotalPnL < HALT_STOP_LOSS) {
            System.err.println("Grand total loss " + grandTotalPnL + " > $50, halt");
            System.exit(-1);
        }
    }

    private void calculatePnL(Trade openTrade, Trade closeTrade) {
        String symbol = closeTrade.getContract().symbol();

        // sanity check
        if (!Objects.equal(symbol, openTrade.getContract().symbol())) {
            throw new IllegalArgumentException("Different symbols for open and close trades: "
                    + "open " + openTrade.getContract().symbol() + " and close " + symbol);
        }

        double openPrice = openTrade.getAvgFillPrice();
        double closePrice = closeTrade.getAvgFillPrice();
        int totalQuantity = closeTrade.getTotalQuantity();
        boolean isLongTrade = LongShort.isLongByOpen(openTrade.getAction());

        double diff = totalQuantity * (closePrice - openPrice);
        double pnl = isLongTrade ? diff : -diff;

        // TODO Partial execution
        MutableDouble contractPnl = pnlByContract.computeIfAbsent(symbol, ignore -> new MutableDouble());

        grandTotalPnL += pnl;
        contractPnl.add(pnl);

        String oldThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(openTrade.getTradeRef());

        // TODO use MDC
        log.info("Trade {}. {} {}, qty {}, open {}, close {}, open time {}, close time {}. P&L {}. Contract P&L {}. Total P&L {}",
                openTrade.getTradeRef(),
                isLongTrade ? "Long" : "Short",
                symbol,
                totalQuantity,
                Formats.fmt(openTrade.getAvgFillPrice()),
                Formats.fmt(closePrice),
                Formats.fmtTime(openTrade.getFillTime()),
                Formats.fmtTime(closeTrade.getFillTime()),
                Formats.fmt(pnl),
                Formats.fmt(contractPnl.doubleValue()),
                Formats.fmt(grandTotalPnL));

        csv.info("{} {} {} {} {} {} {} {} {} {} {}",
                openTrade.getTradeRef(),
                isLongTrade ? "Long" : "Short",
                symbol,
                totalQuantity,
                Formats.fmt(openTrade.getAvgFillPrice()),
                Formats.fmt(closePrice),
                Formats.fmtTime(openTrade.getFillTime()),
                Formats.fmtTime(closeTrade.getFillTime()),
                Formats.fmt(pnl),
                Formats.fmt(contractPnl.doubleValue()),
                Formats.fmt(grandTotalPnL));

        Thread.currentThread().setName(oldThreadName);
    }
}
