package jo.bot;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import jo.app.IApp;
import jo.controller.IBroker;
import jo.model.Bars;
import jo.signal.AllSignals;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.NotCloseToDailyHighRestriction;
import jo.signal.Signal;

public class BelowSimpleAverageBot extends BaseBot {    
    private int periodSeconds;
    private Double prevAveragePrice;
    private double belowAverageVal;
    private double profitTarget;

    public BelowSimpleAverageBot(Contract contract, int totalQuantity, int periodSeconds, double belowAverageVal, double profitTarget) {
        super(contract, totalQuantity);
        this.periodSeconds = periodSeconds;
        this.belowAverageVal = belowAverageVal;
        this.profitTarget = profitTarget;

        List<Signal> signals = new ArrayList<>();
        signals.add(new HasAtLeastNBarsSignal(periodSeconds / 5)); // 90 seconds
        signals.add(new NotCloseToDailyHighRestriction(0.2d));
        // signals.add(new BelowSimpleAverageSignal(90 / 5, 0.03d));

        positionSignal = new AllSignals(signals);
    }

    @Override
    public void start(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        marketData = app.getMarketData(contract.symbol());
        Bars bars = marketData.getBars(BarSize._5_secs);

        new Thread("Bot 1#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);

                        if (positionSignal.isActive(app, contract, marketData)) {
                            //log.info("Signal is active " + marketData.getLastPrice());

                            Double averagePrice = bars.getAverageClose(periodSeconds / 5);
                            if (averagePrice == null) {
                                log.error("getAverageClose({}/5)  returned null", periodSeconds);
                                continue;
                            }

                            double openPrice = averagePrice - belowAverageVal;
                            double profitPrice = averagePrice + profitTarget;
                            
                            openPrice = fixPriceVariance(openPrice);
                            profitPrice = fixPriceVariance(profitPrice);
                            
                            boolean needModify = (prevAveragePrice != null && abs(prevAveragePrice - averagePrice) > 0.02);

                            if (!takeProfitOrderIsActive) {
                                openOrder = new Order();
                                openOrder.orderRef("ave" + periodSeconds);
                                openOrder.orderId(ib.getNextOrderId());
                                openOrder.action(Action.BUY);
                                openOrder.orderType(OrderType.LMT);
                                openOrder.totalQuantity(totalQuantity);
                                openOrder.lmtPrice(openPrice);
                                openOrder.transmit(false);

                                takeProfitOrder = new Order();
                                takeProfitOrder.orderRef("ave" + periodSeconds);
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

                            prevAveragePrice = averagePrice;
                        }
                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }
            }
        }.start();
    }
}
