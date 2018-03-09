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
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.Signal;
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
        signals.add(new HasAtLeastNBarsSignal(90 / 5)); // 90 seconds
        signals.add(new NotCloseToDailyHighRestriction(0.2d));
        // signals.add(new BelowSimpleAverageSignal(90 / 5, 0.03d));

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
                        double lastPrice = marketData.getLastPrice();
//                        if (!openOrderIsActive && takeProfitOrderIsActive && openOrder.lmtPrice() - lastPrice > out*2) {
//                            // openOrder.lmtPrice(openPrice);
//                            takeProfitOrder.lmtPrice(lastPrice);
//
//                            modifyOrders(ib);
//                        }

                        if (takeProfitOrderIsActive) {
                            continue;
                        }

                        if (positionSignal.isActive(app, contract, marketData)) {
                            log.info("Signal is active " + marketData.getLastPrice());

                            Double currentTarget = bars.getAverageClose(90 / 5);
                            if (currentTarget == null) {
                                continue;
                            }

                            // final double lastPrice = marketData.getLastPrice();
                            final double openPrice = lastPrice + in;
                            final double profitPrice = openPrice + out;

                            if (profitPrice - openPrice < 0.05) {
                                throw new RuntimeException("Wtf");
                            }

                            openOrder = new Order();
                            openOrder.orderRef("ave90");
                            openOrder.orderId(ib.getNextOrderId());
                            openOrder.action(Action.BUY);
                            openOrder.orderType(OrderType.LMT);
                            openOrder.totalQuantity(totalQuantity);
                            openOrder.lmtPrice(openPrice);
                            openOrder.transmit(false);

                            takeProfitOrder = new Order();
                            takeProfitOrder.orderRef("ave90");
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
