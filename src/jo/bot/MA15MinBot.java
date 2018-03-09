package jo.bot;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;

import jo.app.IApp;
import jo.controller.IBroker;
import jo.signal.AllSignals;
import jo.signal.BelowSimpleAverageSignal;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.Signal;

public class MA15MinBot extends BaseBot {
    public MA15MinBot(Contract contract, int totalQuantity) {
        super(contract, totalQuantity);

        List<Signal> signals = new ArrayList<>();
        signals.add(new HasAtLeastNBarsSignal(180));
        signals.add(new NotCloseToDailyHighRestriction(0.2d));
        signals.add(new BelowSimpleAverageSignal(900 / 5, 0.20d)); // 15 min

        positionSignal = new AllSignals(signals);
    }

    @Override
    public void start(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        marketData = app.getMarketData(contract.symbol());

        new Thread("Bot 2#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(200);
                        if (takeProfitOrderIsActive) {
                            continue;
                        }

                        if (positionSignal.isActive(app, contract, marketData)) {
                            log.info("Signal is active " + marketData.getLastPrice());

                            final double lastPrice = marketData.getLastPrice();
                            final double openPrice = lastPrice - 0.04d;
                            final double profitPrice = lastPrice + 0.30d;

                            openOrder = new Order();
                            openOrder.orderRef("ave15");
                            openOrder.orderId(ib.getNextOrderId());
                            openOrder.action(Action.BUY);
                            openOrder.orderType(OrderType.LMT);
                            openOrder.totalQuantity(totalQuantity);
                            openOrder.lmtPrice(openPrice);
                            openOrder.transmit(false);

                            takeProfitOrder = new Order();
                            takeProfitOrder.orderRef("ave15");
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
