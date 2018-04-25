package jo.bot;

import static jo.util.PriceUtils.fixPriceVariance;

import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.controller.IBroker;
import jo.filter.Filter;
import jo.filter.NasdaqRegularHoursFilter;
import jo.model.BarType;
import jo.model.Bars;
import jo.model.IApp;
import jo.position.PositionSizeStrategy;
import jo.position.TrailAmountStrategy;
import jo.tech.ChangeList;
import jo.tech.EMA;
import jo.tech.PctChange;
import jo.tech.StopTrail;
import jo.util.AsyncExec;
import jo.util.NullUtils;
import jo.util.PriceUtils;
import jo.util.SyncSignal;

public class MovingAverageHLBot extends BaseBot {
    private final boolean whatIf = true;
    private SyncSignal signal;
    private SyncSignal bar5SecSignal;
    private SyncSignal bar1MinSignal;
    private SyncSignal priceSignal;
    private Bars bars5sec;
    private Bars bars1min;
    private Bars maBars;

    private EMA maH0;
    private EMA maH1;
    private EMA maH2;

    private EMA maL0;
    private EMA maL1;
    private EMA maL2;
    private EMA maEdge;

    public int period = 6;
    private TrailAmountStrategy trailAmountStrategy;

    private IBroker ib;
    private IApp app;
    private Filter nasdaqIsOpen = new NasdaqRegularHoursFilter(1);
    private BotState botStatePrev;
    private StopTrail stopTrail;

    private long cancelOpenOrderWaitInterval = TimeUnit.SECONDS.toMillis(10);
    private long cancelOpenOrderAfter;

    private OpenPositionOrderHandler openPositionOrderHandler = new OpenPositionOrderHandler();
    private MocOrderHandler mocOrderHandler = new MocOrderHandler();
    private ClosePositionOrderHandler closePositionOrderHandler = new ClosePositionOrderHandler();

    private PctChange changeH0;
    private PctChange changeH1;
    private PctChange changeH2;
    private PctChange changeL0;
    private PctChange changeL1;
    private PctChange changeL2;
    private ChangeList hlChanges;

    public MovingAverageHLBot(Contract contract, PositionSizeStrategy positionSize, TrailAmountStrategy trailAmountStrategy) {
        super(contract, positionSize);
        this.trailAmountStrategy = trailAmountStrategy;
    }

    @Override
    public void init(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());

        this.ib = ib;
        this.app = app;

        this.app.initMarketData(contract);

        this.md = app.getMarketData(contract.symbol());
        this.bars5sec = md.getBars(BarSize._5_secs);
        this.bars1min = md.getBars(BarSize._1_min);
        this.maBars = this.bars1min;

        this.bar5SecSignal = bars5sec.getSignal();
        this.bar1MinSignal = bars1min.getSignal();
        this.priceSignal = md.getSignal();
        this.signal = priceSignal;

        this.maBars = bars1min;

        this.maEdge = new EMA(maBars, BarType.CLOSE, period * 3, 0);

        this.maH0 = new EMA(maBars, BarType.HIGH, period, 0);
        this.maH1 = new EMA(maBars, BarType.HIGH, period, 1);
        this.maH2 = new EMA(maBars, BarType.HIGH, period, 2);
        this.maL0 = new EMA(maBars, BarType.LOW, period, 0);
        this.maL1 = new EMA(maBars, BarType.LOW, period, 1);
        this.maL2 = new EMA(maBars, BarType.LOW, period, 2);

        this.changeH0 = new PctChange(maBars, BarType.HIGH, 0);
        this.changeH1 = new PctChange(maBars, BarType.HIGH, 1);
        this.changeH2 = new PctChange(maBars, BarType.HIGH, 2);
        this.changeL0 = new PctChange(maBars, BarType.LOW, 0);
        this.changeL1 = new PctChange(maBars, BarType.LOW, 1);
        this.changeL2 = new PctChange(maBars, BarType.LOW, 2);

        this.hlChanges = ChangeList.of(changeH0, changeH1, changeH2, changeL0, changeL1, changeL2);
        
        this.trailAmountStrategy.init(ib, app);
    }

    @Override
    public void start() {
        String threadName = "MABot#" + contract.symbol();
        this.thread = AsyncExec.startThread(threadName, this::run);
    }

    @Override
    public void runLoop() {
        BotState botState = getBotState();
        if (botStatePrev != botState) {
            log.info("BotState: " + botState);
            botStatePrev = botState;
        }

        if (botState == BotState.PENDING) {
            return;
        }

        if (botState == BotState.OPENNING_POSITION && System.currentTimeMillis() > cancelOpenOrderAfter) {
            log.info("Too slow, cancelling open order");
            ib.cancelOrder(openOrder.orderId());
            signal = priceSignal;
            return;
        }

        if (botState == BotState.READY_TO_OPEN) {
            signal = priceSignal;
            mayBeOpenPosition();
        }

        if (botState == BotState.PROFIT_WAITING) {
            signal = priceSignal;
            mayBeUpdateProfitTaker();
        }
    }

    private void mayBeOpenPosition() {
        if (maBars.getSize() < period + 3)
            return;

        double lastPrice = md.getLastPrice();
        Double maEdgeVal = maEdge.get();

        // going up && lows > MA HIGH && last > maEdge
        boolean goingUp = hlChanges.allPositive();
        double barLow0 = maBars.getLastBar(BarType.LOW, 0);
        double barLow1 = maBars.getLastBar(BarType.LOW, 1);
        double barLow2 = maBars.getLastBar(BarType.LOW, 2);
        Double maValH0 = maH0.get();
        Double maValH1 = maH1.get();
        Double maValH2 = maH2.get();

        // going down && highs < MA lows && last > maEdge
        boolean goingDown = hlChanges.allNegative();
        double barHigh0 = maBars.getLastBar(BarType.HIGH, 0);
        double barHigh1 = maBars.getLastBar(BarType.HIGH, 1);
        double barHigh2 = maBars.getLastBar(BarType.HIGH, 2);
        Double maValL0 = maL0.get();
        Double maValL1 = maL1.get();
        Double maValL2 = maL2.get();

        if (NullUtils.anyNull(maEdgeVal, maValH0, maValH1, maValH2, maValL0, maValL1, maValL2))
            return;

        //log.info(String.format("Last price: %.2f, smaVal: %.2f, barOpen: %.2f, barClose: %.2f", lastPrice, smaVal, barOpen, barClose));
        //System.out.println();

        maEdgeVal = PriceUtils.fixPriceVariance(maEdgeVal);

        boolean placeOrders = false;

        boolean openLong = //goingUp
                // lows > MA HIGH
                barLow0 > maValH0
                        && barLow1 > maValH1
                        && barLow2 > maValH2
                        // last > maEdge
                        && lastPrice > maEdgeVal;

        boolean openShort = //goingDown
                // highs < MA lows
                barHigh0 < maValL0
                        && barHigh1 < maValL1
                        && barHigh2 < maValL2
                        // last < maEdge
                        && lastPrice < maEdgeVal;

        final double trailAmount = trailAmountStrategy.getTrailAmount(md);

        if (openLong) {
            final double openPrice = lastPrice;
            final double stopLossTrail = openPrice - trailAmount;
            final double stopLossEdge = maEdgeVal;
            final double stopLossPrice = fixPriceVariance(Math.max(stopLossTrail, stopLossEdge));
            final double totalQuantity = positionSize.getPositionSize(md);

            log.info("Go Long: open " + String.format("%.2f", openPrice) + ", stop loss " + String.format("%.2f", stopLossPrice));

            openOrder = new Order();
            openOrder.orderId(ib.getNextOrderId());
            openOrder.action(Action.BUY);
            openOrder.orderType(OrderType.LMT);
            openOrder.totalQuantity(totalQuantity);
            openOrder.lmtPrice(openPrice);
            openOrder.transmit(false);

            mocOrder = new Order();
            mocOrder.orderId(ib.getNextOrderId());
            mocOrder.action(Action.SELL);
            mocOrder.orderType(OrderType.MOC);
            mocOrder.totalQuantity(totalQuantity);
            mocOrder.parentId(openOrder.orderId());
            mocOrder.transmit(false);

            closeOrder = new Order();
            closeOrder.orderId(ib.getNextOrderId());
            closeOrder.action(Action.SELL);
            closeOrder.totalQuantity(totalQuantity);
            closeOrder.auxPrice(stopLossPrice);
            closeOrder.parentId(openOrder.orderId());
            closeOrder.orderType(OrderType.STP);
            closeOrder.transmit(true);

            placeOrders = true;
        }

        if (openShort) {
            final double openPrice = lastPrice;
            final double stopLossTrail = openPrice + trailAmount;
            final double stopLossEdge = maEdgeVal;
            final double stopLossPrice = fixPriceVariance(Math.min(stopLossTrail, stopLossEdge));
            final double totalQuantity = positionSize.getPositionSize(md);

            log.info("Go Short: open " + String.format("%.2f", openPrice) + ", stop loss " + String.format("%.2f", stopLossPrice));

            openOrder = new Order();
            openOrder.orderId(ib.getNextOrderId());
            openOrder.action(Action.SELL);
            openOrder.orderType(OrderType.LMT);
            openOrder.totalQuantity(totalQuantity);
            openOrder.lmtPrice(openPrice);
            openOrder.transmit(false);

            mocOrder = new Order();
            mocOrder.orderId(ib.getNextOrderId());
            mocOrder.action(Action.BUY);
            mocOrder.orderType(OrderType.MOC);
            mocOrder.totalQuantity(totalQuantity);
            mocOrder.parentId(openOrder.orderId());
            mocOrder.transmit(false);

            closeOrder = new Order();
            closeOrder.orderId(ib.getNextOrderId());
            closeOrder.action(Action.BUY);
            closeOrder.totalQuantity(totalQuantity);
            closeOrder.auxPrice(stopLossPrice);
            closeOrder.parentId(openOrder.orderId());
            closeOrder.orderType(OrderType.STP);
            closeOrder.transmit(true);

            placeOrders = true;
        }

        if (placeOrders) {
            openOrderStatus = OrderStatus.PendingSubmit;
            closeOrderStatus = OrderStatus.PendingSubmit;

            if (whatIf) {
                openOrder.whatIf(true);
                mocOrder.whatIf(true);
                closeOrder.whatIf(true);

                openOrder.transmit(true);
                mocOrder.transmit(true);
                closeOrder.transmit(true);
            }

            ib.placeOrModifyOrder(contract, openOrder, openPositionOrderHandler);
            ib.placeOrModifyOrder(contract, mocOrder, mocOrderHandler);
            ib.placeOrModifyOrder(contract, closeOrder, closePositionOrderHandler);

            openOrder.transmit(true);
            mocOrder.transmit(true);
            closeOrder.transmit(true);

            stopTrail = new StopTrail(ib, contract, closeOrder, md, trailAmount);
            cancelOpenOrderAfter = System.currentTimeMillis() + cancelOpenOrderWaitInterval;
        }
    }

    private void mayBeUpdateProfitTaker() {
        double edgeVal = fixPriceVariance(maEdge.get());
        double oldPrice = closeOrder.auxPrice();
        boolean update = false;

        // long
        if (closeOrder.action() == Action.SELL && oldPrice < edgeVal) {
            log.info("mayBeUpdateProfitTaker: we are long, updating to edge");
            update = true;
        }

        // short
        if (closeOrder.action() == Action.BUY && oldPrice > edgeVal) {
            log.info("mayBeUpdateProfitTaker: we are short, updating to edge");
            update = true;
        }

        if (update) {
            closeOrder.auxPrice(edgeVal);
            log.info("mayBeUpdateProfitTaker: adjusting from {} to {}", oldPrice, edgeVal);
            ib.placeOrModifyOrder(contract, closeOrder, null);
        }
    }

    @Override
    protected void openPositionOrderFilled(double avgFillPrice) {
        super.openPositionOrderFilled(avgFillPrice);
        stopTrail.start();
    }

    @Override
    protected void closePositionOrderFilled(double avgFillPrice) {
        super.closePositionOrderFilled(avgFillPrice);
        if (stopTrail != null) {
            stopTrail.stop();
        }
    }

    @Override
    protected void closePositionOrderCancelled() {
        super.closePositionOrderCancelled();
        if (stopTrail != null) {
            stopTrail.stop();
        }
    }

    public void run() {
        try {
            while (signal.waitForSignal()) {
                if (Thread.interrupted()) {
                    log.info("Thread Interrupted EXIT");
                    return;
                }

                if (nasdaqIsOpen.isActive(app, contract, md)) {
                    runLoop();
                }
            }
            log.info("Exit");
        } catch (Exception e) {
            log.error("Error in bot", e);
        }
    }
}
