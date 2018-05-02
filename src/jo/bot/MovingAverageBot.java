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
import jo.tech.ChangeList;
import jo.tech.EMA;
import jo.tech.PctChange;
import jo.tech.StopTrail;
import jo.util.AsyncExec;
import jo.util.NullUtils;
import jo.util.Orders;
import jo.util.SyncSignal;

public class MovingAverageBot extends BaseBot {
    private boolean whatIf = false;
    private SyncSignal signal;
    private SyncSignal priceSignal;
    private Bars maBars;

    private EMA maEdge0;
    private EMA maEdge1;
    private EMA maEdge2;

    public int period = 18;
    private TrailAmountStrategy trailAmountStrategy;

    private IBroker ib;
    private IApp app;
    private Filter nasdaqIsOpen = new NasdaqRegularHoursFilter(1);
    private BotState botStatePrev;
    private StopTrail stopTrail;
    private int skipBarIdx;
    private int openPositionBarIdx;

    private long cancelOpenOrderWaitInterval = TimeUnit.SECONDS.toMillis(10);
    private long cancelOpenOrderAfter;

    private OpenPositionOrderHandler openPositionOrderHandler = new OpenPositionOrderHandler();
    private ClosePositionOrderHandler closePositionOrderHandler = new ClosePositionOrderHandler();

    private PctChange changeO0;
    private PctChange changeO1;
    private PctChange changeO2;
    private PctChange changeC0;
    private PctChange changeC1;
    private PctChange changeC2;
    private ChangeList ocChanges;
    private Bars realitimeBars;
    private double smallTrailAmount;
    private double largeTrailAmount;

    public MovingAverageBot(Contract contract, PositionSizeStrategy positionSize, TrailAmountStrategy trailAmountStrategy) {
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
        this.realitimeBars = md.getBars(BarSize._5_secs);

        this.maEdge0 = new EMA(maBars, BarType.CLOSE, period, 0);
        this.maEdge1 = new EMA(maBars, BarType.CLOSE, period, 1);
        this.maEdge2 = new EMA(maBars, BarType.CLOSE, period, 2);

        this.changeO0 = new PctChange(maBars, BarType.OPEN, 0);
        this.changeO1 = new PctChange(maBars, BarType.OPEN, 1);
        this.changeO2 = new PctChange(maBars, BarType.OPEN, 2);
        this.changeC0 = new PctChange(maBars, BarType.CLOSE, 0);
        this.changeC1 = new PctChange(maBars, BarType.CLOSE, 1);
        this.changeC2 = new PctChange(maBars, BarType.CLOSE, 2);

        this.ocChanges = ChangeList.of(changeO0, changeO1, changeO2, changeC0, changeC1, changeC2);

        Stats hs = Stats.tryLoad(contract.symbol());
        if (hs != null) {
            this.trailAmountStrategy = new DollarValueTrailAmountStrategy(hs.getHiLo().getAvg());
        } else {
            this.trailAmountStrategy = new HighLowAvgTrailAmountStrategy(maBars, 180, 0);
        }

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

        double barLow0 = maBars.getLastBar(BarType.LOW, 0);
        double barLow1 = maBars.getLastBar(BarType.LOW, 1);
        double barLow2 = maBars.getLastBar(BarType.LOW, 2);
        double barHigh0 = maBars.getLastBar(BarType.HIGH, 0);
        double barHigh1 = maBars.getLastBar(BarType.HIGH, 1);
        double barHigh2 = maBars.getLastBar(BarType.HIGH, 2);

        if (NullUtils.anyNull(maEdgeVal0, maEdgeVal1, maEdgeVal2))
            return;

        double maEdgeValChange1 = PctChange.of(maEdgeVal2, maEdgeVal1);
        double maEdgeValChange0 = PctChange.of(maEdgeVal1, maEdgeVal0);

        boolean maEdgeGoingUp = maEdgeValChange1 > 0.0005 && maEdgeValChange0 > 0.0005;
        boolean maEdgeGoingDown = maEdgeValChange1 < -0.0005 && maEdgeValChange0 < -0.0005;

        boolean placeOrders = false;

        boolean openLong = maEdgeGoingUp
                && barLow0 - maEdgeVal0 > 0.02
                && lastPrice > maEdgeVal0
                && bidPrice > maEdgeVal0
                && askPrice > maEdgeVal0
        //&& ocChanges.allPositive()
        ;

        boolean openShort = maEdgeGoingDown
                && barHigh0 - maEdgeVal0 < -0.02
                && lastPrice < maEdgeVal0
                && bidPrice < maEdgeVal0
                && askPrice < maEdgeVal0
        //&& ocChanges.allNegative()
        ;

        this.smallTrailAmount = trailAmountStrategy.getTrailAmount(md);
        this.largeTrailAmount = trailAmountStrategy.getTrailAmount(md) * 1.6;

        double strategyTrailAmount = smallTrailAmount;

        if (openLong) {
            String ref = updateTradeRef();

            final double openPrice = realitimeBars.getLastBar().getWap();
            final double stopLossTrail = openPrice - strategyTrailAmount;
            final double stopLossEdge = maEdgeVal0;
            final double stopLossPrice = fixPriceVariance(Math.max(stopLossTrail, stopLossEdge));
            final int totalQuantity = positionSize.getPositionSize(md);

            log.info("Bars HL: {}, {}, {} <- last ", fmt(barHigh2 - barLow2), fmt(barHigh1 - barLow1), fmt(barHigh0 - barLow0));
            log.info("Bar Distance barLow - edge: {}, {}, {} <- last ", fmt(barLow2 - maEdgeVal2), fmt(barLow1 - maEdgeVal1), fmt(barLow0 - maEdgeVal0));
            log.info("Change L%: {}, {}, {} <- last ", fmt(changeC2.getChange() * 100), fmt(changeC1.getChange() * 100), fmt(changeC0.getChange() * 100));
            log.info("Change H%: {}, {}, {} <- last ", fmt(changeO2.getChange() * 100), fmt(changeO1.getChange() * 100), fmt(changeO0.getChange() * 100));
            log.info("Price: last {}, bid {}, ask {}, ask-bid {}", fmt(md.getLastPrice()), fmt(bidPrice), fmt(askPrice), fmt(askPrice - bidPrice));
            log.info("Go Long: open {}, stop loss {}, trail amount {}, edge {}", fmt(openPrice), fmt(stopLossPrice), fmt(strategyTrailAmount), fmt(maEdgeVal0));

            openOrder = Orders.newLimitBuyOrder(ib, totalQuantity, openPrice);
            closeOrder = Orders.newStopSellOrder(ib, totalQuantity, stopLossPrice, openOrder.orderId());
            openOrder.orderRef(ref);
            closeOrder.orderRef(ref);

            placeOrders = true;
        }

        if (openShort) {
            String ref = updateTradeRef();

            //final double openPrice = Math.max(lastPrice, bidPrice + 0.01); //lastPrice;
            final double openPrice = realitimeBars.getLastBar().getWap();
            final double stopLossTrail = openPrice + strategyTrailAmount;
            final double stopLossEdge = maEdgeVal0;
            final double stopLossPrice = fixPriceVariance(Math.min(stopLossTrail, stopLossEdge));
            final int totalQuantity = positionSize.getPositionSize(md);

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

            ib.placeOrModifyOrder(contract, openOrder, openPositionOrderHandler);
            ib.placeOrModifyOrder(contract, closeOrder, closePositionOrderHandler);

            openOrder.transmit(true);

            stopTrail = new StopTrail(ib, contract, closeOrder, md, strategyTrailAmount);
            cancelOpenOrderAfter = System.currentTimeMillis() + cancelOpenOrderWaitInterval;
            openPositionBarIdx = barSize;

        } else {
            skipBarIdx = barSize;
        }
    }

    private void mayBeUpdateProfitTaker() {
        // TODO If started going against us - switch to shorter EMA
        double edgeVal = fixPriceVariance(maEdge0.get());
        boolean longPosition = (closeOrder.action() == Action.SELL);

        int barIdx = maBars.getSize();
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
        //skipBarIdx = maBars.getSize();

        if (barIdx > openPositionBarIdx && currentTrailAmount == smallTrailAmount) {
            boolean updateToLargeTrail = false;
            if (longPosition && currentPrice > openPrice + 0.02) {
                updateToLargeTrail = true;
            }

            if (!longPosition && currentPrice < openPrice - 0.02) {
                updateToLargeTrail = true;
            }

            if (updateToLargeTrail) {
                log.info("mayBeUpdateProfitTaker {}: updating trail amount from {} to {}",
                        longPosition ? "long" : "short",
                        fmt(smallTrailAmount), fmt(largeTrailAmount));
                stopTrail.setTrailAmount(largeTrailAmount);
            }
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
