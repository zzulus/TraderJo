package jo.bot;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;

import jo.app.App;
import jo.controller.IBService;
import jo.signal.AllSignals;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.LastClosePositiveRestriction;
import jo.signal.NasdaqRegularHoursRestriction;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.OpenAfterTimeRestriction;
import jo.signal.RandomSignal;
import jo.signal.Signal;

public class RandomBot extends BaseBot {
    private double profitTarget = 0.30d;
    private Signal startSignal;
    private OpenAfterTimeRestriction openAfterTimeRestriction;

    public RandomBot(Contract contract, int totalQuantity, double profitTarget) {
        super(contract, totalQuantity);
        this.profitTarget = profitTarget;

        openAfterTimeRestriction = new OpenAfterTimeRestriction(0);

        List<Signal> positionSignals = new ArrayList<>();
        positionSignals.add(openAfterTimeRestriction);
        positionSignals.add(new NotCloseToDailyHighRestriction(0.3d));
        positionSignals.add(new RandomSignal(0.3d));
        positionSignals.add(new LastClosePositiveRestriction(6));
        positionSignals.add(new NasdaqRegularHoursRestriction(15));

        positionSignal = new AllSignals(positionSignals);
        startSignal = new HasAtLeastNBarsSignal(6);
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
                        if (!startSignal.isActive(app, contract, marketData)) {
                            continue;
                        }

                        // final double lastPrice = marketData.getLastPrice();
                        // final double lastPrice = marketData.getAskPrice();
                        double openPrice = (marketData.getBidPrice() + marketData.getAskPrice()) / 2d;
                        double profitPrice = openPrice + profitTarget;
                        boolean updateOrders = (openOrder != null && abs(openOrder.lmtPrice() - openPrice) > 0.02);

                        openPrice = fixPriceVariance(openPrice);
                        profitPrice = fixPriceVariance(profitPrice);

                        if (positionSignal.isActive(app, contract, marketData)) {
                            // log.info("Signal is active " + marketData.getLastPrice());

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
        openAfterTimeRestriction.setOpenAfterTimeMillis(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1));
        super.takeProfitOrderFilled();
    }
}
