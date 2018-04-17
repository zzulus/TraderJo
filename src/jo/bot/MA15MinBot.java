package jo.bot;

import java.util.ArrayList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;

import jo.controller.IBroker;
import jo.filter.AllFilters;
import jo.filter.BelowSimpleAverageFilter;
import jo.filter.Filter;
import jo.filter.HasAtLeastNBarsFilter;
import jo.filter.NotCloseToDailyHighFilter;
import jo.model.IApp;

public class MA15MinBot extends BaseBot {
    public MA15MinBot(Contract contract, int totalQuantity) {
        super(contract, totalQuantity);

        List<Filter> filters = new ArrayList<>();
        filters.add(new HasAtLeastNBarsFilter(180));
        filters.add(new NotCloseToDailyHighFilter(0.2d));
        filters.add(new BelowSimpleAverageFilter(900 / 5, 0.20d)); // 15 min

        positionFilter = new AllFilters(filters);
    }

    @Override
    public void init(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        md = app.getMarketData(contract.symbol());

        new Thread("Bot 2#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(200);
                        //                        if (takeProfitOrderIsActive) {
                        //                            continue;
                        //                        }

                        if (positionFilter.isActive(app, contract, md)) {
                            log.info("Signal is active " + md.getLastPrice());

                            final double lastPrice = md.getLastPrice();
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

                            closeOrder = new Order();
                            closeOrder.orderRef("ave15");
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
        }.start();
    }

    @Override
    public void runLoop() {
        // TODO Auto-generated method stub

    }
}
