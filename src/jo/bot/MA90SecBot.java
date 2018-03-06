package jo.bot;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.app.App;
import jo.controller.IBService;
import jo.model.Bars;
import jo.signal.AllSignals;
import jo.signal.BelowSimpleAverageSignal;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.Signal;

public class MA90SecBot extends BaseBot {
    public MA90SecBot(Contract contract, int totalQuantity) {
        super(contract, totalQuantity);

        List<Signal> signals = new ArrayList<>();
        signals.add(new HasAtLeastNBarsSignal(90 / 5)); // 90 seconds
        signals.add(new NotCloseToDailyHighRestriction(0.2d));
        //signals.add(new BelowSimpleAverageSignal(90 / 5, 0.03d));

        positionSignal = new AllSignals(signals);
    }

    @Override
    public void start(IBService ib, App app) {
        log.info("Start bot for {}", contract.symbol());
        marketData = app.getStockMarketData(contract.symbol());
        Bars bars = marketData.getBars(BarSize._5_secs);

        new Thread("Bot 1#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        if (takeProfitOrderIsActive) {
                            continue;
                        }

                        if (positionSignal.isActive(app, contract, marketData)) {
                            log.info("Signal is active " + marketData.getLastPrice());

                            Double currentTarget = bars.getAverageClose(90 / 5);
                            if (currentTarget == null) {
                                continue;
                            }

                            final double lastPrice = marketData.getLastPrice();
                            final double openPrice = lastPrice - 0.04d;
                            final double profitPrice = lastPrice + 0.10d;

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
        }.start();
    }
}
