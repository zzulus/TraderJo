package jo.bot;

import static jo.util.Formats.fmt;
import static jo.util.PriceUtils.fixPriceVariance;

import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;
import com.ib.client.OrderStatus;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.controller.IBroker;
import jo.filter.Filter;
import jo.filter.NasdaqRegularHoursFilter;
import jo.model.BarType;
import jo.model.Bars;
import jo.model.IApp;
import jo.position.HighLowAvgTrailAmountStrategy;
import jo.position.PositionSizeStrategy;
import jo.position.TrailAmountStrategy;
import jo.tech.ChangeList;
import jo.tech.EMA;
import jo.tech.PctChange;
import jo.tech.StopTrail;
import jo.util.AsyncExec;
import jo.util.NullUtils;
import jo.util.Orders;
import jo.util.SyncSignal;

public class MovingAverageHLBot extends BaseBot {
    private boolean whatIf = false;
    private SyncSignal signal;
    private SyncSignal priceSignal;
    private Bars maBars;

    private EMA maH0;
    private EMA maH1;
    private EMA maH2;

    private EMA maL0;
    private EMA maL1;
    private EMA maL2;
    private EMA maEdge0;
    private EMA maEdge1;
    private EMA maEdge2;

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
        this.priceSignal = md.getSignal();
        this.signal = priceSignal;

        this.maBars = md.getBars(BarSize._1_min);

        this.maEdge0 = new EMA(maBars, BarType.CLOSE, period * 3, 0);
        this.maEdge1 = new EMA(maBars, BarType.CLOSE, period * 3, 1);
        this.maEdge2 = new EMA(maBars, BarType.CLOSE, period * 3, 2);

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

        this.trailAmountStrategy = new HighLowAvgTrailAmountStrategy(maBars, 180, 0);
        //this.trailAmountStrategy = new HistoricalHighLowAvgTrailAmountStrategy(BarSize._1_min, 1, 0, contract);
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

        if (botState == BotState.PENDING && whatIf && System.currentTimeMillis() > cancelOpenOrderAfter + TimeUnit.MINUTES.toMillis(2)) {
            log.info("WhatIf, cancelling open order");
            openOrderStatus = null;
            return;
        }

        if (botState == BotState.PENDING) {
            return;
        }

        if (botState == BotState.OPENNING_POSITION && System.currentTimeMillis() > cancelOpenOrderAfter) {
            log.info("Too slow, cancelling open order");
            ib.cancelOrder(openOrder.orderId());
            return;
        }

        if (botState == BotState.READY_TO_OPEN) {
            mayBeOpenPosition();
        }

        if (botState == BotState.PROFIT_WAITING) {
            mayBeUpdateProfitTaker();
        }
    }

    private int skipBarIdx = 0;

    private void mayBeOpenPosition() {
        int barSize = maBars.getSize();
        if (barSize < period + 3 || barSize == skipBarIdx)
            return;

        double lastPrice = md.getLastPrice();
        double bidPrice = md.getBidPrice(); // buy
        double askPrice = md.getAskPrice(); // sell

        Double maEdgeVal0 = fixPriceVariance(maEdge0.get());
        Double maEdgeVal1 = fixPriceVariance(maEdge1.get());
        Double maEdgeVal2 = fixPriceVariance(maEdge2.get());

        // going up && lows > MA HIGH && last > maEdge
        boolean goingUp = hlChanges.allPositive();
        double barLow0 = maBars.getLastBar(BarType.LOW, 0);
        double barLow1 = maBars.getLastBar(BarType.LOW, 1);
        double barLow2 = maBars.getLastBar(BarType.LOW, 2);
        Double maValH0 = fixPriceVariance(maH0.get());
        Double maValH1 = fixPriceVariance(maH1.get());
        Double maValH2 = fixPriceVariance(maH2.get());

        // going down && highs < MA lows && last > maEdge
        boolean goingDown = hlChanges.allNegative();
        double barHigh0 = maBars.getLastBar(BarType.HIGH, 0);
        double barHigh1 = maBars.getLastBar(BarType.HIGH, 1);
        double barHigh2 = maBars.getLastBar(BarType.HIGH, 2);
        Double maValL0 = fixPriceVariance(maL0.get());
        Double maValL1 = fixPriceVariance(maL1.get());
        Double maValL2 = fixPriceVariance(maL2.get());

        if (NullUtils.anyNull(maEdgeVal0, maEdgeVal1, maEdgeVal2, maValH0, maValH1, maValH2, maValL0, maValL1, maValL2))
            return;

        boolean placeOrders = false;

        boolean openLong = goingUp
                // lows > MA HIGH
                && barLow0 > maValH0
                && barLow1 > maValH1
                && barLow2 > maValH2
                // last > maEdge
                && lastPrice > maEdgeVal0
                && bidPrice > maEdgeVal0
                && askPrice > maEdgeVal0;

        boolean openShort = goingDown
                // highs < MA lows
                && barHigh0 < maValL0
                && barHigh1 < maValL1
                && barHigh2 < maValL2
                // last < maEdge
                && lastPrice < maEdgeVal0
                && bidPrice < maEdgeVal0
                && askPrice < maEdgeVal0;

        final double trailAmount = fixPriceVariance(trailAmountStrategy.getTrailAmount(md) * 1.7);

        if (openLong) {
            updateTradeRef();

            final double openPrice = Math.min(lastPrice, askPrice - 0.01); //lastPrice;
            final double stopLossTrail = openPrice - trailAmount;
            final double stopLossEdge = maEdgeVal0;
            final double stopLossPrice = fixPriceVariance(Math.max(stopLossTrail, stopLossEdge));
            final int totalQuantity = positionSize.getPositionSize(md);

            log.info("Bars HL: {}, {}, {} <- last ", fmt(barHigh2 - barLow2), fmt(barHigh1 - barLow1), fmt(barHigh0 - barLow0));
            log.info("Bar Distance barLow - maH: {}, {}, {} <- last ", fmt(barLow2 - maValH2), fmt(barLow1 - maValH1), fmt(barLow0 - maValH0));
            log.info("Bar Distance barLow - edge: {}, {}, {} <- last ", fmt(barLow2 - maEdgeVal2), fmt(barLow1 - maEdgeVal1), fmt(barLow0 - maEdgeVal0));
            log.info("Change L%: {}, {}, {} <- last ", fmt(changeL2.getChange() * 100), fmt(changeL1.getChange() * 100), fmt(changeL0.getChange() * 100));
            log.info("Change H%: {}, {}, {} <- last ", fmt(changeH2.getChange() * 100), fmt(changeH1.getChange() * 100), fmt(changeH0.getChange() * 100));
            log.info("Price: last {}, bid {}, ask {}, ask-bid {}", fmt(md.getLastPrice()), fmt(bidPrice), fmt(askPrice), fmt(askPrice - bidPrice));
            log.info("Go Long: open {}, stop loss {}, trail amount {}, edge {}", fmt(openPrice), fmt(stopLossPrice), fmt(trailAmount), fmt(maEdgeVal0));

            openOrder = Orders.newLimitBuyOrder(ib, totalQuantity, openPrice);
            mocOrder = Orders.newMocSellOrder(ib, totalQuantity, openOrder.orderId());
            closeOrder = Orders.newStopSellOrder(ib, totalQuantity, stopLossPrice, openOrder.orderId());

            placeOrders = true;
        }

        if (openShort) {
            updateTradeRef();

            final double openPrice = Math.max(lastPrice, bidPrice + 0.01); //lastPrice;
            final double stopLossTrail = openPrice + trailAmount;
            final double stopLossEdge = maEdgeVal0;
            final double stopLossPrice = fixPriceVariance(Math.min(stopLossTrail, stopLossEdge));
            final int totalQuantity = positionSize.getPositionSize(md);

            log.info("Bar Distance maL - barHigh: {}, {}, {} <- last ", fmt(maValL2 - barHigh2), fmt(maValL1 - barHigh1), fmt(maValL0 - barHigh0));
            log.info("Bar Distance edge - barHigh: {}, {}, {} <- last ", fmt(maEdgeVal2 - barHigh2), fmt(maEdgeVal1 - barHigh1), fmt(maEdgeVal0 - barHigh0));
            log.info("Bars HL: {}, {}, {} <- last ", fmt(barHigh2 - barLow2), fmt(barHigh1 - barLow1), fmt(barHigh0 - barLow0));
            log.info("Change L%: {}, {}, {} <- last ", fmt(changeL2.getChange() * 100), fmt(changeL1.getChange() * 100), fmt(changeL0.getChange() * 100));
            log.info("Change H%: {}, {}, {} <- last ", fmt(changeH2.getChange() * 100), fmt(changeH1.getChange() * 100), fmt(changeH0.getChange() * 100));
            log.info("Price: last {}, bid {}, ask {}, ask-bid {}", fmt(md.getLastPrice()), fmt(bidPrice), fmt(askPrice), fmt(askPrice - bidPrice));
            log.info("Go Short: open {}, stop loss {}, trail amount {}, edge {}", fmt(openPrice), fmt(stopLossPrice), fmt(trailAmount), fmt(maEdgeVal0));

            openOrder = Orders.newLimitSellOrder(ib, totalQuantity, openPrice);
            mocOrder = Orders.newMocBuyOrder(ib, totalQuantity, openOrder.orderId());
            closeOrder = Orders.newStopBuyOrder(ib, totalQuantity, stopLossPrice, openOrder.orderId());

            placeOrders = true;
        }

        if (placeOrders) {
            openOrderStatus = OrderStatus.PendingSubmit;
            closeOrderStatus = OrderStatus.PendingSubmit;

            openOrder.transmit(false);
            mocOrder.transmit(false);
            closeOrder.transmit(true);

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

            stopTrail = new StopTrail(ib, contract, closeOrder, md, trailAmount);
            cancelOpenOrderAfter = System.currentTimeMillis() + cancelOpenOrderWaitInterval;
        } else {
            skipBarIdx = barSize;
        }
    }

    private void mayBeUpdateProfitTaker() {
        double edgeVal = fixPriceVariance(maEdge0.get());

        boolean longPosition = closeOrder.action() == Action.SELL;
        log.info("mayBeUpdateProfitTaker {}: check if can update from {} to new edge {}",
                longPosition ? "long" : "short",
                fmt(closeOrder.auxPrice()),
                fmt(edgeVal));
        stopTrail.maybeUpdateStopPrice(edgeVal);
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
