package jo.bot;

import static jo.util.Formats.fmt;
import static jo.util.PriceUtils.fixPriceVariance;

import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;
import com.ib.client.OrderStatus;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.controller.IApp;
import jo.controller.IBroker;
import jo.filter.Filter;
import jo.filter.NasdaqRegularHoursFilter;
import jo.model.BarType;
import jo.model.Bars;
import jo.model.Stats;
import jo.position.DollarValueTrailAmountStrategy;
import jo.position.HighLowAvgTrailAmountStrategy;
import jo.position.PositionSizeStrategy;
import jo.position.TrailAmountStrategy;
import jo.tech.BarsPctChange;
import jo.tech.ChangeList;
import jo.tech.EMA;
import jo.tech.StopTrail;
import jo.trade.TradeSummary;
import jo.util.AsyncExec;
import jo.util.NullUtils;
import jo.util.Orders;
import jo.util.SyncSignal;

public class MovingAverageBot extends BaseBot {
    private boolean whatIf = false;
    private SyncSignal signal;
    private SyncSignal priceSignal;
    private Bars maBars;
    private Bars rtBars;

    private EMA maEdge0;
    private EMA maEdge1;
    private EMA maEdge2;

    private EMA maRt0;
    private EMA maRt1;
    private EMA maRt2;

    private int rtPeriod = 5; // last 25 seconds
    public int period = 18;
    private TrailAmountStrategy trailAmountStrategy;

    private IBroker ib;
    private IApp app;
    private Filter nasdaqIsOpen = new NasdaqRegularHoursFilter(1);
    private BotState botStatePrev;
    private StopTrail stopTrail;
    private int skipBarIdx;
    private String defaultThreadName;

    private long cancelOpenOrderWaitInterval = TimeUnit.SECONDS.toMillis(45);
    private long cancelOpenOrderAfter;

    private OpenPositionOrderHandler openPositionOrderHandler = new OpenPositionOrderHandler();
    private ClosePositionOrderHandler closePositionOrderHandler = new ClosePositionOrderHandler();

    private BarsPctChange changeO0;
    private BarsPctChange changeO1;
    private BarsPctChange changeO2;
    private BarsPctChange changeC0;
    private BarsPctChange changeC1;
    private BarsPctChange changeC2;
    private ChangeList ocChanges;

    public MovingAverageBot(Contract contract, PositionSizeStrategy positionSize) {
        super(contract, positionSize);
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

        this.rtBars = md.getBars(BarSize._5_secs);
        this.maRt0 = new EMA(rtBars, BarType.CLOSE, rtPeriod, 0);
        this.maRt1 = new EMA(rtBars, BarType.CLOSE, rtPeriod, 1);
        this.maRt2 = new EMA(rtBars, BarType.CLOSE, rtPeriod, 2);

        this.maBars = md.getBars(BarSize._1_min);
        this.maEdge0 = new EMA(maBars, BarType.CLOSE, period, 0);
        this.maEdge1 = new EMA(maBars, BarType.CLOSE, period, 1);
        this.maEdge2 = new EMA(maBars, BarType.CLOSE, period, 2);

        this.changeO0 = new BarsPctChange(maBars, BarType.OPEN, 0);
        this.changeO1 = new BarsPctChange(maBars, BarType.OPEN, 1);
        this.changeO2 = new BarsPctChange(maBars, BarType.OPEN, 2);
        this.changeC0 = new BarsPctChange(maBars, BarType.CLOSE, 0);
        this.changeC1 = new BarsPctChange(maBars, BarType.CLOSE, 1);
        this.changeC2 = new BarsPctChange(maBars, BarType.CLOSE, 2);

        this.ocChanges = ChangeList.of(changeO0, changeO1, changeO2, changeC0, changeC1, changeC2);

        Stats hs = Stats.tryLoad(contract.symbol());
        if (hs != null) {
            double trailAmount = Math.max(0.05, hs.getHiLo().getP99());
            log.info("Using {} trail stop for {}", trailAmount, contract.symbol());
            this.trailAmountStrategy = new DollarValueTrailAmountStrategy(trailAmount);

        } else {
            this.trailAmountStrategy = new HighLowAvgTrailAmountStrategy(maBars, 180, 0);
        }

        this.trailAmountStrategy.init(ib, app);
    }

    @Override
    public void start() {
        defaultThreadName = "MABot#" + contract.symbol();
        this.thread = AsyncExec.startThread(defaultThreadName, this::run);
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
            // TODO Liquidate position or update price
            return;
        }

        if (botState == BotState.READY_TO_OPEN) {
            if (!Thread.currentThread().getName().equals(defaultThreadName)) {
                Thread.currentThread().setName(defaultThreadName);
            }
            mayBeOpenPosition();
        }

        if (botState == BotState.PROFIT_WAITING) {
            mayBeUpdateProfitTaker();
        }
    }

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

        Double maRtVal0 = fixPriceVariance(maRt0.get());
        Double maRtVal1 = fixPriceVariance(maRt1.get());
        Double maRtVal2 = fixPriceVariance(maRt2.get());

        double barLow0 = maBars.getLastBar(BarType.LOW, 0);
        double barLow1 = maBars.getLastBar(BarType.LOW, 1);
        double barLow2 = maBars.getLastBar(BarType.LOW, 2);
        double barHigh0 = maBars.getLastBar(BarType.HIGH, 0);
        double barHigh1 = maBars.getLastBar(BarType.HIGH, 1);
        double barHigh2 = maBars.getLastBar(BarType.HIGH, 2);
        double barOpen0 = maBars.getLastBar(BarType.OPEN, 0);
        double barOpen1 = maBars.getLastBar(BarType.OPEN, 1);
        double barOpen2 = maBars.getLastBar(BarType.OPEN, 2);
        double barClose0 = maBars.getLastBar(BarType.CLOSE, 0);
        double barClose1 = maBars.getLastBar(BarType.CLOSE, 1);
        double barClose2 = maBars.getLastBar(BarType.CLOSE, 2);

        if (NullUtils.anyNull(maEdgeVal0, maEdgeVal1, maEdgeVal2))
            return;

        double maEdgeValChange1 = BarsPctChange.of(maEdgeVal2, maEdgeVal1);
        double maEdgeValChange0 = BarsPctChange.of(maEdgeVal1, maEdgeVal0);

        double maRtValChange1 = BarsPctChange.of(maRtVal2, maRtVal1);
        double maRtValChange0 = BarsPctChange.of(maRtVal1, maRtVal0);

        boolean maEdgeGoingUp = maEdgeValChange1 > 0 && maEdgeValChange0 > 0;
        boolean maEdgeGoingDown = maEdgeValChange1 < 0 && maEdgeValChange0 < 0;

        boolean maRtGoingUp = maRtValChange1 > 0 && maRtValChange0 > 0;
        boolean maRtGoingDown = maRtValChange1 < 0 && maRtValChange0 < 0;

        boolean placeOrders = false;

        boolean openLong = maEdgeGoingUp
                && maRtGoingUp
                && barOpen0 < barClose0
                && barOpen1 < barClose1
                && barLow0 - maEdgeVal0 > 0.02
                && lastPrice > maEdgeVal0
                && bidPrice > maEdgeVal0
                && askPrice > maEdgeVal0
        //&& ocChanges.allPositive()
        ;

        boolean openShort = maEdgeGoingDown
                && maRtGoingDown
                && barOpen0 > barClose0
                && barOpen1 > barClose1
                && barHigh0 - maEdgeVal0 < -0.02
                && lastPrice < maEdgeVal0
                && bidPrice < maEdgeVal0
                && askPrice < maEdgeVal0
        //&& ocChanges.allNegative()
        ;

        double strategyTrailAmount = trailAmountStrategy.getTrailAmount(md);

        if (openLong) {
            String tradeRef = updateTradeRef();

            final double openPrice = rtBars.getLastBar().getWap();
            final double stopLossTrail = openPrice - strategyTrailAmount;
            final double stopLossEdge = maEdgeVal0;
            final double stopLossPrice = fixPriceVariance(Math.max(stopLossTrail, stopLossEdge));
            final int totalQuantity = positionSize.getPositionSize(openPrice, strategyTrailAmount);

            log.info("Bars HL: {}, {}, {} <- last ", fmt(barHigh2 - barLow2), fmt(barHigh1 - barLow1), fmt(barHigh0 - barLow0));
            log.info("Bar Distance barLow - edge: {}, {}, {} <- last ", fmt(barLow2 - maEdgeVal2), fmt(barLow1 - maEdgeVal1), fmt(barLow0 - maEdgeVal0));
            log.info("Change L%: {}, {}, {} <- last ", fmt(changeC2.getChange() * 100), fmt(changeC1.getChange() * 100), fmt(changeC0.getChange() * 100));
            log.info("Change H%: {}, {}, {} <- last ", fmt(changeO2.getChange() * 100), fmt(changeO1.getChange() * 100), fmt(changeO0.getChange() * 100));
            log.info("Price: last {}, bid {}, ask {}, ask-bid {}", fmt(md.getLastPrice()), fmt(bidPrice), fmt(askPrice), fmt(askPrice - bidPrice));
            log.info("Go Long: open {}, stop loss {}, trail amount {}, edge {}", fmt(openPrice), fmt(stopLossPrice), fmt(strategyTrailAmount), fmt(maEdgeVal0));

            openOrder = Orders.newLimitBuyOrder(ib, totalQuantity, openPrice);
            closeOrder = Orders.newStopSellOrder(ib, totalQuantity, stopLossPrice, openOrder.orderId());
            openOrder.orderRef(tradeRef);
            closeOrder.orderRef(tradeRef);

            placeOrders = true;
        }

        if (openShort) {
            String ref = updateTradeRef();

            //final double openPrice = Math.max(lastPrice, bidPrice + 0.01); //lastPrice;
            final double openPrice = rtBars.getLastBar().getWap();
            final double stopLossTrail = openPrice + strategyTrailAmount;
            final double stopLossEdge = maEdgeVal0;
            final double stopLossPrice = fixPriceVariance(Math.min(stopLossTrail, stopLossEdge));
            final int totalQuantity = positionSize.getPositionSize(openPrice, strategyTrailAmount);

            log.info("Bar Distance edge - barHigh: {}, {}, {} <- last ", fmt(maEdgeVal2 - barHigh2), fmt(maEdgeVal1 - barHigh1), fmt(maEdgeVal0 - barHigh0));
            log.info("Bars HL: {}, {}, {} <- last ", fmt(barHigh2 - barLow2), fmt(barHigh1 - barLow1), fmt(barHigh0 - barLow0));
            log.info("Change L%: {}, {}, {} <- last ", fmt(changeC2.getChange() * 100), fmt(changeC1.getChange() * 100), fmt(changeC0.getChange() * 100));
            log.info("Change H%: {}, {}, {} <- last ", fmt(changeO2.getChange() * 100), fmt(changeO1.getChange() * 100), fmt(changeO0.getChange() * 100));
            log.info("Price: last {}, bid {}, ask {}, ask-bid {}", fmt(md.getLastPrice()), fmt(bidPrice), fmt(askPrice), fmt(askPrice - bidPrice));
            log.info("Go Short: open {}, stop loss {}, trail amount {}, edge {}", fmt(openPrice), fmt(stopLossPrice), fmt(strategyTrailAmount), fmt(maEdgeVal0));

            openOrder = Orders.newLimitSellOrder(ib, totalQuantity, openPrice);
            closeOrder = Orders.newStopBuyOrder(ib, totalQuantity, stopLossPrice, openOrder.orderId());
            openOrder.orderRef(ref);
            closeOrder.orderRef(ref);

            placeOrders = true;
        }

        if (placeOrders) {
            openOrderStatus = OrderStatus.PendingSubmit;
            closeOrderStatus = OrderStatus.PendingSubmit;

            openOrder.transmit(false);
            closeOrder.transmit(true);

            if (whatIf) {
                openOrder.whatIf(true);
                closeOrder.whatIf(true);

                openOrder.transmit(true);
                closeOrder.transmit(true);
            }

            TradeSummary.addOrder(contract, openOrder);
            TradeSummary.addOrder(contract, closeOrder);

            ib.placeOrModifyOrder(contract, openOrder, openPositionOrderHandler);
            ib.placeOrModifyOrder(contract, closeOrder, closePositionOrderHandler);

            openOrder.transmit(true);

            stopTrail = new StopTrail(ib, contract, closeOrder, md, strategyTrailAmount);
            cancelOpenOrderAfter = System.currentTimeMillis() + cancelOpenOrderWaitInterval;
        } else {
            markBarUsed();
        }
    }

    private void mayBeUpdateProfitTaker() {
        markBarUsed();

        // TODO If started going against us - switch to shorter EMA
        double edgeVal = fixPriceVariance(maEdge0.get());
        boolean longPosition = (closeOrder.action() == Action.SELL);

        double openPrice = openOrder.lmtPrice();
        double currentPrice = md.getLastPrice();
        double currentTrailAmount = stopTrail.getTrailAmount();

        log.info("mayBeUpdateProfitTaker {}: check if should update from {} to new edge {}",
                longPosition ? "long" : "short",
                fmt(closeOrder.auxPrice()),
                fmt(edgeVal));

        //        // force stop loss to positive zone
        //        if (longPosition && openPrice < stopLossPrice && currentPrice > lastOpen) {
        //            stopTrail.maybeUpdateStopPrice(lastOpen);
        //        }
        //
        //        if (!longPosition && openPrice > stopLossPrice && currentPrice < lastOpen) {
        //            stopTrail.maybeUpdateStopPrice(lastOpen);
        //        }

        stopTrail.maybeUpdateStopPrice(edgeVal);
    }

    @Override
    protected void openPositionOrderFilled(int orderId, double avgFillPrice) {
        super.openPositionOrderFilled(orderId, avgFillPrice);
        stopTrail.start();
    }

    @Override
    protected void closePositionOrderFilled(int orderId, double avgFillPrice) {
        super.closePositionOrderFilled(orderId, avgFillPrice);
        markBarUsed();

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

    private void markBarUsed() {
        skipBarIdx = maBars.getSize();
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
            throw new RuntimeException(e);
        }
    }
}
