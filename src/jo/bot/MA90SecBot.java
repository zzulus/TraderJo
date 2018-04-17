package jo.bot;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.controller.IBroker;
import jo.filter.AllFilters;
import jo.filter.Filter;
import jo.filter.TwoMAFilter;
import jo.model.Bars;
import jo.model.IApp;
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

        List<Filter> signals = new ArrayList<>();
        //signals.add(new HasAtLeastNBarsSignal(90 / 5)); // 90 seconds
        //signals.add(new NasdaqRegularHoursRestriction(0));
        //signals.add(new NotCloseToDailyHighRestriction(0.2d));
        // signals.add(new BelowSimpleAverageSignal(90 / 5, 0.03d));
        // signals.add(new BelowSimpleAverageSignal((5 * 60) / 5, 0.03d));

        int n = 5;
        signals.add(new TwoMAFilter(n, 2 * n + 1));

        positionFilter = new AllFilters(signals);
    }

    @Override
    public void init(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        md = app.getMarketData(contract.symbol());
        Bars bars = md.getBars(BarSize._5_secs);
        SyncSignal marketDataSignal = md.getUpdateSignal();

        this.thread = new Thread("Bot 1#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    if (Thread.interrupted()) {
                        return;
                    }

                    try {
                        marketDataSignal.waitForSignal();

                        //                        if (takeProfitOrderIsActive) {
                        //                            continue;
                        //                        }
                        //double lastPrice = marketData.getLastPrice();
                        double basePrice = md.getAskPrice();

                        if (positionFilter.isActive(app, contract, md)) {
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

                            closeOrder = new Order();
                            closeOrder.orderId(ib.getNextOrderId());
                            closeOrder.action(Action.SELL);
                            closeOrder.orderType(OrderType.LMT);
                            closeOrder.totalQuantity(totalQuantity);
                            closeOrder.lmtPrice(profitPrice);
                            closeOrder.parentId(openOrder.orderId());
                            closeOrder.transmit(true);

                            //                            placeOrders(ib);
                        }
                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }
            }
        };
        thread.start();
    }

    @Override
    public void runLoop() {
        // TODO Auto-generated method stub

    }
}
