package jo.bot;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.app.IApp;
import jo.controller.IBroker;
import jo.filter.Filter;
import jo.filter.NasdaqRegularHoursFilter;
import jo.model.BarType;
import jo.model.Bars;
import jo.tech.Channel;
import jo.tech.DonchianChannel;
import jo.tech.SMA;
import jo.util.AsyncExec;
import jo.util.SyncSignal;

public class DonchianBot extends BaseBot {
    private SyncSignal sig;
    private DonchianChannel donchian;
    private DonchianChannel donchianPrev;
    private SMA fastMovAvg;
    public int lowerPeriod = 90;
    public int upperPeriod = 90;
    public int fastMovAvgPeriod = 12;
    private double grabProfit = 0.25;
    private IBroker ib;
    private IApp app;
    private Filter nasdaqIsOpen = new NasdaqRegularHoursFilter(1);
    private Bars bars;

    public DonchianBot(Contract contract, int totalQuantity) {
        super(contract, totalQuantity);
    }

    @Override
    public void init(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        this.ib = ib;
        this.app = app;

        this.md = app.getMarketData(contract.symbol());
        this.sig = md.getBarSignal();
        bars = md.getBars(BarSize._5_secs);
        this.donchian = new DonchianChannel(bars, BarType.WAP, BarType.WAP, lowerPeriod, upperPeriod);
        this.donchianPrev = new DonchianChannel(bars, BarType.WAP, BarType.WAP, lowerPeriod, upperPeriod);
        this.donchianPrev.setOffset(1);
        this.fastMovAvg = new SMA(bars, BarType.CLOSE, fastMovAvgPeriod, 0);
    }

    @Override
    public void start() {
        String threadName = "DonchianBot#" + contract.symbol();
        this.thread = AsyncExec.startThread(threadName, this::run);
    }

    @Override
    public void runLoop() {
        BotState botState = getBotState();
        log.info("BotState: " + botState);

        if (botState == BotState.PENDING) {
            return;
        }

        if (botState == BotState.OPENNING_POSITION) {
            log.info("Too slow, cancelling open order");
            ib.cancelOrder(openOrder.orderId());
            return;
        }

        Channel ch = donchian.get();
        Channel prevCh = donchianPrev.get();
        if (ch == null || prevCh == null)
            return;

        log.info("Ch - Prev: " + String.format("%.2f / %.2f / %.2f", prevCh.getLower(), prevCh.getMiddle(), prevCh.getUpper()));
        log.info("Ch - Curr: " + String.format("%.2f / %.2f / %.2f", ch.getLower(), ch.getMiddle(), ch.getUpper()));
        log.info("");

        // TODO
        // Cancel order if not filled and timing or trend changed
        // Update stop loss point until get positive
        // Add confirmation of price penetration
        // Tight stop loss on entry, normal after time or positive

        if (botState == BotState.READY_TO_OPEN) {
            mayBeOpenPosition(ch, prevCh);
        }
        
        if (botState == BotState.PROFIT_WAITING) {
            mayBeUpdateProfitTaker();
        }
    }

    private void mayBeUpdateProfitTaker() {
        
    }

    private void mayBeOpenPosition(Channel ch, Channel prevCh) {
        if (ch.getUpper() > prevCh.getUpper()) {
            final double openPrice = bars.getLastBar(BarType.WAP);
            final double stopLossMax = openPrice - grabProfit;
            final double stopLossPrevLow = bars.getLastBar(BarType.LOW);
            final double stopLossMin = openPrice - 0.1;
            final double stopLossPrice = Math.max(stopLossMax, Math.min(stopLossMin, stopLossPrevLow));

            log.info("Go Long: open " + String.format("%.2f", openPrice) + ", stop loss " + String.format("%.2f", stopLossPrice));

            openOrder = new Order();
            openOrder.orderId(ib.getNextOrderId());
            openOrder.action(Action.BUY);
            openOrder.orderType(OrderType.LMT);
            openOrder.totalQuantity(totalQuantity);
            openOrder.lmtPrice(openPrice);
            openOrder.transmit(false);

            takeProfitOrder = new Order();
            takeProfitOrder.orderId(ib.getNextOrderId());
            takeProfitOrder.action(Action.SELL);
            takeProfitOrder.totalQuantity(totalQuantity);
            takeProfitOrder.parentId(openOrder.orderId());
            takeProfitOrder.orderType(OrderType.TRAIL);
            takeProfitOrder.auxPrice(grabProfit);
            takeProfitOrder.trailStopPrice(stopLossPrice);
            takeProfitOrder.transmit(false);

            mocOrder = new Order();
            mocOrder.orderId(ib.getNextOrderId());
            mocOrder.action(Action.SELL);
            mocOrder.orderType(OrderType.MOC);
            mocOrder.totalQuantity(totalQuantity);
            mocOrder.parentId(openOrder.orderId());
            mocOrder.transmit(true);

            openOrderStatus = OrderStatus.PendingSubmit;
            takeProfitOrderStatus = OrderStatus.PendingSubmit;

            ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
            ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
            ib.placeOrModifyOrder(contract, mocOrder, new MocOrderHandler());

        } else if (ch.getLower() < prevCh.getLower()) {
            final double openPrice = bars.getLastBar(BarType.WAP);
            final double stopLossMax = openPrice + grabProfit;
            final double stopLossPrevHigh = bars.getLastBar(BarType.HIGH);
            final double stopLossMin = openPrice + 0.1;
            final double stopLossPrice = Math.min(stopLossMax, Math.max(stopLossMin, stopLossPrevHigh));

            log.info("Go Short: open " + String.format("%.2f", openPrice) + ", stop loss " + String.format("%.2f", stopLossPrice));

            openOrder = new Order();
            openOrder.orderId(ib.getNextOrderId());
            openOrder.action(Action.SELL);
            openOrder.orderType(OrderType.LMT);
            openOrder.totalQuantity(totalQuantity);
            openOrder.lmtPrice(openPrice);
            openOrder.transmit(false);

            takeProfitOrder = new Order();
            takeProfitOrder.orderId(ib.getNextOrderId());
            takeProfitOrder.action(Action.BUY);
            takeProfitOrder.totalQuantity(totalQuantity);
            takeProfitOrder.parentId(openOrder.orderId());
            takeProfitOrder.orderType(OrderType.TRAIL);
            takeProfitOrder.auxPrice(grabProfit);
            takeProfitOrder.trailStopPrice(stopLossPrice);
            takeProfitOrder.transmit(false);

            mocOrder = new Order();
            mocOrder.orderId(ib.getNextOrderId());
            mocOrder.action(Action.BUY);
            mocOrder.orderType(OrderType.MOC);
            mocOrder.totalQuantity(totalQuantity);
            mocOrder.parentId(openOrder.orderId());
            mocOrder.transmit(true);

            openOrderStatus = OrderStatus.PendingSubmit;
            takeProfitOrderStatus = OrderStatus.PendingSubmit;

            ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
            ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
            ib.placeOrModifyOrder(contract, mocOrder, new MocOrderHandler());
        }
    }

    public void run() {
        try {
            while (sig.waitForSignal()) {

                if (Thread.interrupted()) {
                    return;
                }

                if (nasdaqIsOpen.isActive(app, contract, md)) {
                    runLoop();
                }
            }
        } catch (Exception e) {
            log.error("Error in bot", e);
        }
    }
}
