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
import jo.filter.AllFilters;
import jo.filter.AskIsGreaterThanLastFilter;
import jo.filter.Filter;
import jo.filter.HasAtLeastNBarsFilter;
import jo.filter.LastIsGreaterThanCloseFilter;
import jo.filter.NasdaqRegularHoursFilter;
import jo.filter.NotCloseToDailyHighFilter;
import jo.filter.NotCloseToHourHighFilter;
import jo.filter.RandomFilter;
import jo.util.SyncSignal;

public class RandomBot extends BaseBot {
    private double in = -0.01d;
    private double out = 0.30d;
    private Filter startSignal;

    public RandomBot(Contract contract, int totalQuantity, double in, double out) {
        super(contract, totalQuantity);
        this.in = in;
        this.out = out;

        List<Filter> positionSignals = new ArrayList<>();
        // positionSignals.add(openAfterTimeRestriction); // TODO bullshit, add trend + support/resistance
        // positionSignals.add(new BarShapeHLHRestriction());
        positionSignals.add(new NotCloseToDailyHighFilter(0.3d));
        positionSignals.add(new NotCloseToHourHighFilter(0.3d)); // TODO this is bullshit too

        positionSignals.add(new RandomFilter(0.05d));
        //positionSignals.add(new LastIsGreaterThanCloseRestriction());
        //positionSignals.add(new AskIsGreaterThanLastRestriction());

        // positionSignals.add(new NasdaqRegularHoursRestriction(15));

        positionFilter = new AllFilters(positionSignals);
        startSignal = new HasAtLeastNBarsFilter(6);
    }

    @Override
    public void init(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        md = app.getMarketData(contract.symbol());
        SyncSignal marketDataSignal = md.getUpdateSignal();

        this.thread = new Thread("Bot Rnd#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    if (Thread.interrupted()) {
                        return;
                    }

                    marketDataSignal.waitForSignal();
                    if (startSignal.isActive(app, contract, md)) {
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

                        final double lastPrice = md.getLastPrice();
                        double openPrice = lastPrice + in;
                        double profitPrice = openPrice + out;
                        boolean updateOrders = (openOrder != null && abs(openOrder.lmtPrice() - openPrice) > 0.02);

                        if (profitPrice - openPrice < 0.05) {
                            throw new RuntimeException("Wtf");
                        }

                        openPrice = fixPriceVariance(openPrice);
                        profitPrice = fixPriceVariance(profitPrice);

                        if (positionFilter.isActive(app, contract, md)) {
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

    @Override
    public void runLoop() {
        // TODO Auto-generated method stub
        
    }
}
