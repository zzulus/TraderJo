package jo.bot;

import static jo.util.PriceUtils.fixPriceVariance;

import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.filter.NasdaqRegularHours;
import jo.model.BarType;
import jo.model.Bars;
import jo.model.Context;
import jo.position.PositionSizeStrategy;
import jo.tech.Channel;
import jo.tech.DonchianChannel;
import jo.tech.SMA;
import jo.tech.StopTrail;
import jo.util.AsyncExec;
import jo.util.SyncSignal;

public class DonchianBot extends BaseBot {
    private SyncSignal signal;
    private SyncSignal barSignal;
    private SyncSignal priceSignal;

    private DonchianChannel donchian;
    private SMA fastSMA;
    public int fastSMAPeriod = 9;
    public int lowerPeriod = 120;
    public int upperPeriod = 120;
    private double trailAmount;
    private Bars bars;
    private BotState botStatePrev;
    private StopTrail stopTrail;
    private long cancelOpenOrderWaitInterval = TimeUnit.SECONDS.toMillis(10);
    private long cancelOpenOrderAfter;

    private OpenPositionOrderHandler openPositionOrderHandler = new OpenPositionOrderHandler();
    private ClosePositionOrderHandler closePositionOrderHandler = new ClosePositionOrderHandler();

    public DonchianBot(Contract contract, PositionSizeStrategy positionSize, double trailAmount) {
        super(contract, positionSize);
        this.trailAmount = trailAmount;
    }

    @Override
    public void init(Context ctx) {
        log.info("Start bot for {}", contract.symbol());

        this.ctx = ctx;
        this.ib = ctx.getIb();

        this.md = ctx.initMarketData(contract);
        this.bars = md.getBars(BarSize._5_secs);

        this.barSignal = bars.getSignal();
        this.priceSignal = md.getSignal();
        this.signal = priceSignal;

        this.donchian = new DonchianChannel(bars, BarType.LOW, BarType.HIGH, lowerPeriod, upperPeriod);
        this.fastSMA = new SMA(bars, BarType.CLOSE, fastSMAPeriod, 0);
    }

    @Override
    public void start() {
        String threadName = "DonchianBot#" + contract.symbol();
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

        // TODO
        // Cancel order if not filled and timing or trend changed - done
        // Update stop loss point until get positive
        // Add confirmation of price penetration
        // Tight stop loss on entry, normal after time or positive

        if (botState == BotState.READY_TO_OPEN) {
            signal = priceSignal;
            mayBeOpenPosition();
        }

        if (botState == BotState.PROFIT_WAITING) {
            signal = barSignal;
            mayBeUpdateProfitTaker();
        }
    }

    private void mayBeUpdateProfitTaker() {
        double barOpen0 = bars.getLastBar(BarType.OPEN, 0);
        double barClose0 = bars.getLastBar(BarType.CLOSE, 0);

        double barOpen1 = bars.getLastBar(BarType.OPEN, 1);
        double barClose1 = bars.getLastBar(BarType.CLOSE, 1);

        double barOpen2 = bars.getLastBar(BarType.OPEN, 2);
        double barClose2 = bars.getLastBar(BarType.CLOSE, 2);

        boolean goingDown = (barClose0 < barOpen0 && (barClose1 < barOpen1 || barClose2 < barOpen2));
        boolean goingUp = (barClose0 > barOpen0 && (barClose1 > barOpen1 || barClose2 > barOpen2));

        double smaVal = fixPriceVariance(fastSMA.get());
        boolean update = false;

        double oldPrice = closeOrder.auxPrice();

        // long
        if (closeOrder.action() == Action.SELL && goingDown && oldPrice < smaVal) {
            log.info("mayBeUpdateProfitTaker: we are long, price going down, updating...");
            update = true;
        }

        // short
        if (closeOrder.action() == Action.BUY && goingUp && oldPrice > smaVal) {
            log.info("mayBeUpdateProfitTaker: we are short, price going up, updating...");
            update = true;
        }

        if (update) {
            closeOrder.auxPrice(smaVal);
            log.info("mayBeUpdateProfitTaker: adjusting from {} to {}", oldPrice, smaVal);
            ib.placeOrModifyOrder(contract, closeOrder, null);
        }
    }

    private void mayBeOpenPosition() {
        Channel ch = donchian.get();
        if (ch == null)
            return;

        double barOpen = bars.getLastBar(BarType.OPEN);
        double barClose = bars.getLastBar(BarType.CLOSE);
        double lastPrice = md.getLastPrice();

        Double smaVal = fastSMA.get();
        if (smaVal == null) {
            return;
        }

        log.info(String.format("Channel: L: %.2f, M: %.2f, U: %.2f", ch.getLower(), ch.getMiddle(), ch.getUpper()));
        log.info(String.format("Last price: %.2f, smaVal: %.2f, barOpen: %.2f, barClose: %.2f", lastPrice, smaVal, barOpen, barClose));

        boolean placeOrders = false;

        if (lastPrice > ch.getUpper() && barClose > smaVal) {
            final double openPrice = lastPrice;// bars.getLastBar(BarType.WAP);
            final double stopLossMax = openPrice - trailAmount;
            final double prevBarClose = bars.getLastBar(BarType.CLOSE);
            final double stopLossMin = openPrice - 0.15; //TODO % of GrabProfit
            final double stopLossPrice = fixPriceVariance(stopLossMin); //Math.max(stopLossMin, prevBarClose);
            final double totalQuantity = positionSize.getPositionSize(openPrice, 0.15);

            log.info(String.format("Channel: L: %.2f, M: %.2f, U: %.2f", ch.getLower(), ch.getMiddle(), ch.getUpper()));
            log.info(String.format("Last price: %.2f, smaVal: %.2f, barOpen: %.2f, barClose: %.2f", lastPrice, smaVal, barOpen, barClose));
            log.info("Go Long: open " + String.format("%.2f", openPrice) + ", stop loss " + String.format("%.2f", stopLossPrice));

            openOrder = new Order();
            openOrder.orderId(ib.getNextOrderId());
            openOrder.action(Action.BUY);
            openOrder.orderType(OrderType.LMT);
            openOrder.totalQuantity(totalQuantity);
            openOrder.lmtPrice(openPrice);
            openOrder.transmit(false);

            closeOrder = new Order();
            closeOrder.orderId(ib.getNextOrderId());
            closeOrder.action(Action.SELL);
            closeOrder.totalQuantity(totalQuantity);
            closeOrder.auxPrice(stopLossPrice);
            closeOrder.parentId(openOrder.orderId());
            closeOrder.orderType(OrderType.STP);
            closeOrder.transmit(true);

            placeOrders = true;

        } else if (lastPrice < ch.getLower() && barClose < smaVal) {
            final double openPrice = lastPrice;//bars.getLastBar(BarType.WAP);
            final double stopLossMax = openPrice + trailAmount;
            final double prevBar = bars.getLastBar(BarType.CLOSE);
            final double stopLossMin = openPrice + 0.15; //TODO % of GrabProfit
            final double stopLossPrice = fixPriceVariance(stopLossMin); // Math.min(stopLossMax, Math.max(stopLossMin, stopLossPrevHigh));
            final double totalQuantity = positionSize.getPositionSize(openPrice, 0.15);

            log.info(String.format("Channel: L: %.2f, M: %.2f, U: %.2f", ch.getLower(), ch.getMiddle(), ch.getUpper()));
            log.info(String.format("Last price: %.2f, smaVal: %.2f, barOpen: %.2f, barClose: %.2f", lastPrice, smaVal, barOpen, barClose));
            log.info("Go Short: open " + String.format("%.2f", openPrice) + ", stop loss " + String.format("%.2f", stopLossPrice));

            openOrder = new Order();
            openOrder.orderId(ib.getNextOrderId());
            openOrder.action(Action.SELL);
            openOrder.orderType(OrderType.LMT);
            openOrder.totalQuantity(totalQuantity);
            openOrder.lmtPrice(openPrice);
            openOrder.transmit(false);

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

            ctx.getTradeBook().addOrder(contract, openOrder);
            ctx.getTradeBook().addOrder(contract, closeOrder);

            ib.placeOrModifyOrder(contract, openOrder, openPositionOrderHandler);
            ib.placeOrModifyOrder(contract, closeOrder, closePositionOrderHandler);

            openOrder.transmit(true);
            closeOrder.transmit(true);

            stopTrail = new StopTrail(ib, contract, closeOrder, md, trailAmount);
            cancelOpenOrderAfter = System.currentTimeMillis() + cancelOpenOrderWaitInterval;
        }
    }

    @Override
    protected void openPositionOrderFilled(int orderId, double avgFillPrice) {
        super.openPositionOrderFilled(orderId, avgFillPrice);
        stopTrail.start();
    }

    @Override
    protected void closePositionOrderFilled(int orderId, double avgFillPrice) {
        super.closePositionOrderFilled(orderId, avgFillPrice);
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

                if (NasdaqRegularHours.INSTANCE.isMarketOpen()) {
                    runLoop();
                }
            }
            log.info("Exit");
        } catch (Exception e) {
            log.error("Error in bot", e);
        }
    }
}
