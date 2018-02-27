package jo.bot;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;

import jo.app.App;
import jo.controller.IBService;
import jo.signal.AllSignals;
import jo.signal.NotCloseToDailyHighSignal;
import jo.signal.RandomSignal;
import jo.signal.Signal;

public class RandomBot extends BaseBot {
    private double profitTarget = 0.30d;

    public RandomBot(Contract contract, int totalQuantity, double profitTarget) {
        super(contract, totalQuantity);
        this.profitTarget = profitTarget;

        List<Signal> signals = new ArrayList<>();
        signals.add(new NotCloseToDailyHighSignal(0.2d));
        signals.add(new RandomSignal(0.5d));

        signal = new AllSignals(signals);
    }

    @Override
    public void start(IBService ib, App app) {
        log.info("Start bot for {}", contract.symbol());
        marketData = app.getStockMarketData(contract.symbol());

        new Thread("Bot Rnd#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);

                        if (signal.isActive(app, contract, marketData)) {
                            // log.info("Signal is active " + marketData.getLastPrice());

                            final double lastPrice = marketData.getLastPrice();
                            double openPrice = lastPrice - 0.04d;
                            double profitPrice = lastPrice + profitTarget;
                            boolean needModify = (openOrder != null && abs(openOrder.lmtPrice() - openPrice) > 0.02);

                            openPrice = fixPriceVariance(openPrice);
                            profitPrice = fixPriceVariance(profitPrice);

                            if (!takeProfitOrderIsActive) {
                                openOrder = new Order();
                                openOrder.orderRef("rnd" + profitTarget);
                                openOrder.orderId(ib.getNextOrderId());
                                openOrder.action(Action.BUY);
                                openOrder.orderType(OrderType.LMT);
                                openOrder.totalQuantity(totalQuantity);
                                openOrder.lmtPrice(openPrice);
                                openOrder.transmit(false);

                                takeProfitOrder = new Order();
                                takeProfitOrder.orderRef("rnd" + profitTarget);
                                takeProfitOrder.orderId(ib.getNextOrderId());
                                takeProfitOrder.action(Action.SELL);
                                takeProfitOrder.orderType(OrderType.LMT);
                                takeProfitOrder.totalQuantity(totalQuantity);
                                takeProfitOrder.lmtPrice(profitPrice);
                                takeProfitOrder.parentId(openOrder.orderId());
                                takeProfitOrder.transmit(true);

                                placeOrders(ib);
                            } else if (openOrderIsActive && takeProfitOrderIsActive && needModify) {
                                openOrder.lmtPrice(openPrice);
                                takeProfitOrder.lmtPrice(profitPrice);

                                modifyOrders(ib);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }
            }
        }.start();
    }
}
