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
import jo.signal.AskIsGreaterThanLastRestriction;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.LastIsGreaterThanCloseRestriction;
import jo.signal.NasdaqRegularHoursRestriction;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.NotCloseToHourHighRestriction;
import jo.signal.RandomSignal;
import jo.signal.Signal;
import jo.util.SyncSignal;

public class RandomBot extends BaseBot {
    private double in = -0.01d;
    private double out = 0.30d;
    private Signal startSignal;

    public RandomBot(Contract contract, int totalQuantity, double in, double out) {
        super(contract, totalQuantity);
        this.in = in;
        this.out = out;

        List<Signal> positionSignals = new ArrayList<>();
        // positionSignals.add(openAfterTimeRestriction); // TODO bullshit, add trend + support/resistance
        // positionSignals.add(new BarShapeHLHRestriction());
        positionSignals.add(new NotCloseToDailyHighRestriction(0.3d));
        positionSignals.add(new NotCloseToHourHighRestriction(0.3d)); // TODO this is bullshit too

        positionSignals.add(new RandomSignal(0.05d));
        //positionSignals.add(new LastIsGreaterThanCloseRestriction());
        //positionSignals.add(new AskIsGreaterThanLastRestriction());

        // positionSignals.add(new NasdaqRegularHoursRestriction(15));

        positionSignal = new AllSignals(positionSignals);
        startSignal = new HasAtLeastNBarsSignal(6);
    }

    @Override
    public void start(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        marketData = app.getMarketData(contract.symbol());
        SyncSignal marketDataSignal = marketData.getUpdateSignal();

        this.thread = new Thread("Bot Rnd#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    if (Thread.interrupted()) {
                        return;
                    }

                    marketDataSignal.waitForSignal();
                    if (startSignal.isActive(app, contract, marketData)) {
                        break;
                    }
                }
                log.info("Bot activated");

                while (true) {
                    if (Thread.interrupted()) {
                        return;
                    }

                    try {
                        marketDataSignal.waitForSignal();

                        final double lastPrice = marketData.getLastPrice();
                        double openPrice = lastPrice + in;
                        double profitPrice = openPrice + out;
                        boolean updateOrders = (openOrder != null && abs(openOrder.lmtPrice() - openPrice) > 0.02);

                        if (profitPrice - openPrice < 0.05) {
                            throw new RuntimeException("Wtf");
                        }

                        openPrice = fixPriceVariance(openPrice);
                        profitPrice = fixPriceVariance(profitPrice);

                        if (positionSignal.isActive(app, contract, marketData)) {
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
                        } else if (openOrderIsActive && takeProfitOrderIsActive && updateOrders) {
                            openOrder.lmtPrice(openPrice);
                            takeProfitOrder.lmtPrice(profitPrice);

                            modifyOrders(ib);
                        } /* 
                        // DO NOT DO
                        else if (!openOrderIsActive && takeProfitOrderIsActive && openOrder.lmtPrice() - lastPrice > out) {
                            //openOrder.lmtPrice(openPrice);
                            takeProfitOrder.lmtPrice(lastPrice);

                            modifyOrders(ib);
                        }*/


                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }
            }
        };
        thread.start();
    }

    protected void takeProfitOrderFilled() {
        super.takeProfitOrderFilled();
    }
}