package jo.bot;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;

import jo.app.IApp;
import jo.controller.IBroker;
import jo.signal.AllSignals;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.LastTradesNotNegativeRestriction;
import jo.signal.NasdaqRegularHoursRestriction;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.RandomSignal;
import jo.signal.Signal;
import jo.util.SyncSignal;

public class ChaseTrendBot extends BaseBot {
    private double profitTarget = 0.30d;
    private Signal startSignal;

    public ChaseTrendBot(Contract contract, int totalQuantity, double profitTarget) {
        super(contract, totalQuantity);
        this.profitTarget = profitTarget;

        List<Signal> positionSignals = new ArrayList<>();
        positionSignals.add(new NasdaqRegularHoursRestriction(15));

        positionSignal = new AllSignals(positionSignals);
        startSignal = new HasAtLeastNBarsSignal(6);
    }

    @Override
    public void start(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        md = app.getMarketData(contract.symbol());
        SyncSignal marketDataSignal = md.getUpdateSignal();

        new Thread("Bot Chase#" + contract.symbol()) {
            @Override
            public void run() {
                while (!startSignal.isActive(app, contract, md)) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }

                while (true) {
                    try {
                        marketDataSignal.waitForSignal();

                        final double lastPrice = md.getLastPrice();
                        double openPrice = lastPrice - 0.04d;
                        double profitPrice = openPrice + profitTarget;
                        boolean updateOrders = (openOrder != null && abs(openOrder.lmtPrice() - openPrice) > 0.02);

                        openPrice = fixPriceVariance(openPrice);
                        profitPrice = fixPriceVariance(profitPrice);

                        if (positionSignal.isActive(app, contract, md)) {
                            if (!takeProfitOrderIsActive) {
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
                        } else {
                            if (openOrderIsActive && takeProfitOrderIsActive && updateOrders) {
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

    protected void takeProfitOrderFilled() {
        super.takeProfitOrderFilled();
    }
}
