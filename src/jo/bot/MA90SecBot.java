package jo.bot;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.app.IApp;
import jo.controller.IBroker;
import jo.model.Bars;
import jo.signal.AllSignals;
import jo.signal.BelowSimpleAverageSignal;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.NasdaqRegularHoursRestriction;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.Signal;
import jo.signal.TwoMASignal;
import jo.util.SyncSignal;

public class MA90SecBot extends BaseBot {
    private double in = -0.04d;
    private double out = +0.04d;

    public MA90SecBot(Contract contract, int totalQuantity, double inDelta, double outDelta) {
        this(contract, totalQuantity);
        this.in = inDelta;
        this.out = outDelta;
    }

    public MA90SecBot(Contract contract, int totalQuantity) {
        super(contract, totalQuantity);

        List<Signal> signals = new ArrayList<>();
        //signals.add(new HasAtLeastNBarsSignal(90 / 5)); // 90 seconds
        //signals.add(new NasdaqRegularHoursRestriction(0));
        //signals.add(new NotCloseToDailyHighRestriction(0.2d));
        // signals.add(new BelowSimpleAverageSignal(90 / 5, 0.03d));
        // signals.add(new BelowSimpleAverageSignal((5 * 60) / 5, 0.03d));
        
        int n = 5;
        signals.add(new TwoMASignal(n, 2 * n + 1));

        positionSignal = new AllSignals(signals);
    }

    @Override
    public void start(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        marketData = app.getMarketData(contract.symbol());
        Bars bars = marketData.getBars(BarSize._5_secs);
        SyncSignal marketDataSignal = marketData.getUpdateSignal();

        this.thread = new Thread("Bot 1#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    if (Thread.interrupted()) {
                        return;
                    }

                    try {
                        marketDataSignal.waitForSignal();

                        if (takeProfitOrderIsActive) {
                            continue;
                        }
                        //double lastPrice = marketData.getLastPrice();
                        double basePrice = marketData.getAskPrice();

                        if (positionSignal.isActive(app, contract, marketData)) {
                            // log.info("Signal is active " + marketData.getLastPrice());

                            // final double lastPrice = marketData.getLastPrice();
                            final double openPrice = basePrice + in;
                            final double profitPrice = openPrice + out;

                            if (profitPrice - openPrice < 0.05) {
                                throw new RuntimeException("Wtf");
                            }

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
                            takeProfitOrder.orderType(OrderType.LMT);
                            takeProfitOrder.totalQuantity(totalQuantity);
                            takeProfitOrder.lmtPrice(profitPrice);
                            takeProfitOrder.parentId(openOrder.orderId());
                            takeProfitOrder.transmit(true);

                            placeOrders(ib);
                        }
                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }
            }
        };
        thread.start();
    }
}