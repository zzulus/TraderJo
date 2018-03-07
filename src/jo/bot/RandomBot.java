package jo.bot;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;

import jo.app.TraderApp;
import jo.controller.IBService;
import jo.signal.AllSignals;
import jo.signal.AskIsGreaterThanLastRestriction;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.LastIsGreaterThanCloseRestriction;
import jo.signal.NasdaqRegularHoursRestriction;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.NotCloseToHourHighRestriction;
import jo.signal.Signal;
import jo.util.SyncSignal;

public class RandomBot extends BaseBot {
    private double profitTarget = 0.30d;
    private Signal startSignal;

    public RandomBot(Contract contract, int totalQuantity, double profitTarget) {
        super(contract, totalQuantity);
        this.profitTarget = profitTarget;

        List<Signal> positionSignals = new ArrayList<>();
        // positionSignals.add(openAfterTimeRestriction); // TODO bullshit, add trend + support/resistance
        //positionSignals.add(new BarShapeHLHRestriction());
        positionSignals.add(new NotCloseToDailyHighRestriction(0.3d));
        positionSignals.add(new NotCloseToHourHighRestriction(0.3d)); // TODO this is bullshit too

        // positionSignals.add(new RandomSignal(0.5d));
        positionSignals.add(new LastIsGreaterThanCloseRestriction());
        positionSignals.add(new AskIsGreaterThanLastRestriction());
        
        positionSignals.add(new NasdaqRegularHoursRestriction(15));

        positionSignal = new AllSignals(positionSignals);
        startSignal = new HasAtLeastNBarsSignal(6);
    }

    @Override
    public void start(IBService ib, TraderApp app) {
        log.info("Start bot for {}", contract.symbol());
        marketData = app.getStockMarketData(contract.symbol());
        SyncSignal marketDataSignal = marketData.getUpdateSignal();

        new Thread("Bot Rnd#" + contract.symbol()) {
            @Override
            public void run() {
                while (!startSignal.isActive(app, contract, marketData)) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }
                log.info("Bot activated");
                

                while (true) {
                    try {
                        marketDataSignal.waitForSignal();
                        //System.out.println("Signal");

                        final double lastPrice = marketData.getLastPrice();
                        double openPrice = marketData.getAskPrice() - 0.01;
                        // double openPrice = (marketData.getBidPrice() + marketData.getAskPrice()) / 2;
                        double profitPrice = openPrice + profitTarget;
                        boolean updateOrders = (openOrder != null && abs(openOrder.lmtPrice() - openPrice) > 0.02);

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
