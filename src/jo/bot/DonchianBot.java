package jo.bot;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.app.IApp;
import jo.controller.IBroker;
import jo.filter.Filter;
import jo.filter.NasdaqRegularHoursFilter;
import jo.model.Bars;
import jo.tech.Channel;
import jo.tech.DonchianChannel;
import jo.util.AsyncExec;
import jo.util.SyncSignal;

public class DonchianBot extends BaseBot {
    private SyncSignal sig;
    private DonchianChannel donchian;
    public int lowerPeriod = 51;
    public int upperPeriod = 51;
    private double grabProfit = 0.5;
    private IBroker ib;
    private IApp app;
    private Filter nasdaqIsOpen = new NasdaqRegularHoursFilter(1);    

    public DonchianBot(Contract contract, int totalQuantity) {
        super(contract, totalQuantity);
    }

    @Override
    public void init(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        this.ib = ib;
        this.app = app;

        this.md = app.getMarketData(contract.symbol());
        this.sig = md.getUpdateSignal();        
        Bars bars = md.getBars(BarSize._5_secs);
        this.donchian = new DonchianChannel(bars, lowerPeriod, upperPeriod);
    }

    @Override
    public void start() {
        String threadName = "DonchianBot#" + contract.symbol();
        this.thread = AsyncExec.startThread(threadName, this::run);
    }

    @Override
    public void runLoop() {
        if (takeProfitOrderIsActive || nasdaqIsOpen.isActive(app, contract, md))
            return;

        Channel ch = donchian.get();
        if (ch == null)
            return;

        double lastPrice = md.getLastPrice();

        if (lastPrice > ch.getUpper()) {
            log.info(String.format("Last %.2f, Bid/Ask %.2f / %.2f", lastPrice, md.getBidPrice(), md.getAskPrice()));
            log.info("Ch:" + String.format("%.2f / %.2f / %.2f", ch.getLower(), ch.getMiddle(), ch.getUpper()));
            final double openPrice = lastPrice;
            final double profitPrice = openPrice + grabProfit;
            final double stopLossPrice = openPrice - grabProfit;

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

            // stopLossOrder = new Order();
            // stopLossOrder.orderId(ib.getNextOrderId());
            // stopLossOrder.action(Action.SELL);
            // stopLossOrder.orderType(OrderType.STP);
            // stopLossOrder.totalQuantity(totalQuantity);
            // stopLossOrder.auxPrice(stopLossPrice);
            // stopLossOrder.parentId(openOrder.orderId());
            // stopLossOrder.transmit(true);

            mocOrder = new Order();
            mocOrder.orderId(ib.getNextOrderId());
            mocOrder.action(Action.SELL);
            mocOrder.orderType(OrderType.MOC);
            mocOrder.totalQuantity(totalQuantity);
            mocOrder.parentId(openOrder.orderId());
            mocOrder.transmit(true);

            openOrderIsActive = true;
            takeProfitOrderIsActive = true;

            ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
            ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
            ib.placeOrModifyOrder(contract, mocOrder, new TakeProfitOrderHandler());

        } else if (lastPrice < ch.getLower()) {
            log.info(String.format("Last %.2f, Bid/Ask %.2f / %.2f", lastPrice, md.getBidPrice(), md.getAskPrice()));
            log.info("Ch:" + String.format("%.2f / %.2f / %.2f", ch.getLower(), ch.getMiddle(), ch.getUpper()));
            final double openPrice = lastPrice;
            final double profitPrice = openPrice - grabProfit;
            final double stopLossPrice = openPrice + grabProfit;

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

            // stopLossOrder = new Order();
            // stopLossOrder.orderId(ib.getNextOrderId());
            // stopLossOrder.action(Action.BUY);
            // stopLossOrder.orderType(OrderType.STP);
            // stopLossOrder.totalQuantity(totalQuantity);
            // stopLossOrder.auxPrice(stopLossPrice);
            // stopLossOrder.parentId(openOrder.orderId());
            // stopLossOrder.transmit(true);

            mocOrder = new Order();
            mocOrder.orderId(ib.getNextOrderId());
            mocOrder.action(Action.BUY);
            mocOrder.orderType(OrderType.MOC);
            mocOrder.totalQuantity(totalQuantity);
            mocOrder.parentId(openOrder.orderId());
            mocOrder.transmit(true);

            openOrderIsActive = true;
            takeProfitOrderIsActive = true;

            ib.placeOrModifyOrder(contract, openOrder, new OpenPositionOrderHandler());
            ib.placeOrModifyOrder(contract, takeProfitOrder, new TakeProfitOrderHandler());
            ib.placeOrModifyOrder(contract, mocOrder, new TakeProfitOrderHandler());
        }

    }

    public void run() {
        try {
            while (sig.waitForSignal()) {

                if (Thread.interrupted()) {
                    return;
                }

                runLoop();
            }
        } catch (Exception e) {
            log.error("Error in bot", e);
        }
    }
}
