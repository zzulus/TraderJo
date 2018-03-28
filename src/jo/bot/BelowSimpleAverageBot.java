/*
 * 
 */
package jo.bot;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;

import com.google.common.base.Joiner;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;

import gnu.trove.list.array.TDoubleArrayList;
import jo.app.IApp;
import jo.controller.IBroker;
import jo.filter.AllFilters;
import jo.filter.Filter;
import jo.filter.HasAtLeastNBarsFilter;
import jo.filter.NotCloseToDailyHighFilter;
import jo.model.Bars;
import jo.util.SyncSignal;

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

        List<Filter> signals = new ArrayList<>();
        signals.add(new HasAtLeastNBarsFilter(periodSeconds / 5)); // 90 seconds
        signals.add(new NotCloseToDailyHighFilter(0.2d));
        // signals.add(new BelowSimpleAverageSignal(90 / 5, 0.03d));

        positionFilter = new AllFilters(signals);
    }

    @Override
    public void init(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());
        md = app.getMarketData(contract.symbol());
        SyncSignal marketDataSignal = md.getUpdateSignal();
        Bars bars = md.getBars(BarSize._5_secs);
        TDoubleArrayList close = bars.getClose();

        new Thread("Bot 1#" + contract.symbol()) {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (Thread.interrupted()) {
                            return;
                        }

                        marketDataSignal.waitForSignal();

                        double lastPrice = md.getLastPrice();
                        int size = close.size();
                        Double averagePrice0 = getAverageClose(close, 0, periodSeconds / 5);
                        Double averagePrice1 = getAverageClose(close, 1, periodSeconds / 5);
                        Double averagePrice2 = getAverageClose(close, 2, periodSeconds / 5);
                        Double averagePrice3 = getAverageClose(close, 3, periodSeconds / 5);
                        Double averagePrice4 = getAverageClose(close, 4, periodSeconds / 5);
                        Double averagePrice5 = getAverageClose(close, 5, periodSeconds / 5);
                        Double averagePrice6 = getAverageClose(close, 6, periodSeconds / 5);
                        if (averagePrice0 == null || averagePrice6 == null) {
                            // log.error("getAverageClose({}/5) returned null", periodSeconds);
                            continue;
                        }

                        double close0 = close.get(size - 1);
                        double close1 = close.get(size - 2);
                        double close2 = close.get(size - 3);
                        double close3 = close.get(size - 4);
                        double close4 = close.get(size - 5);
                        double close5 = close.get(size - 6);
                        double close6 = close.get(size - 7);

                        String closeDiffs = fmtSign(4,
                                close5 - close6,
                                close4 - close5,
                                close3 - close4,
                                close2 - close3,
                                close1 - close2,
                                lastPrice - close0);

                        String avgs = fmt(2,
                                averagePrice6, averagePrice5, averagePrice4, averagePrice3, averagePrice2, averagePrice1, averagePrice0);

                        String avgDiffs = fmtSign(4,
                                averagePrice5 - averagePrice6,
                                averagePrice4 - averagePrice5,
                                averagePrice3 - averagePrice4,
                                averagePrice2 - averagePrice3,
                                averagePrice1 - averagePrice2,
                                lastPrice - averagePrice0);

                        String bidLastAskPrice = fmt(2, md.getBidPrice(), md.getLastPrice(), md.getAskPrice());
                        String bidLastAskSize = "[" + md.getBidSize() + ", " + md.getLastSize() + ", " + md.getAskSize() + "]";

                        // String msg = String.format("Last %.2favg %s, diff %s", lastPrice, avgs, avgDiffs);

                        System.out.println("BLA:   " + bidLastAskPrice);
                        System.out.println("VOL:   " + bidLastAskSize);
                        System.out.println("Cdiff: " + closeDiffs);
                        // System.out.println("Avgs: " + avgs);
                        System.out.println("AvgD:  " + avgDiffs);
                        System.out.println();

                        // log.info(msg);

                        // if (positionSignal.isActive(app, contract, marketData)) {
                        // //log.info("Signal is active " + marketData.getLastPrice());
                        //
                        // Double averagePrice = bars.getAverageClose(periodSeconds / 5);
                        // if (averagePrice == null) {
                        // log.error("getAverageClose({}/5) returned null", periodSeconds);
                        // continue;
                        // }
                        //
                        // double openPrice = averagePrice - belowAverageVal;
                        // double profitPrice = averagePrice + profitTarget;
                        //
                        // openPrice = fixPriceVariance(openPrice);
                        // profitPrice = fixPriceVariance(profitPrice);
                        //
                        // boolean needModify = (prevAveragePrice != null && abs(prevAveragePrice - averagePrice) > 0.02);
                        //
                        // if (!takeProfitOrderIsActive) {
                        // openOrder = new Order();
                        // openOrder.orderRef("ave" + periodSeconds);
                        // openOrder.orderId(ib.getNextOrderId());
                        // openOrder.action(Action.BUY);
                        // openOrder.orderType(OrderType.LMT);
                        // openOrder.totalQuantity(totalQuantity);
                        // openOrder.lmtPrice(openPrice);
                        // openOrder.transmit(false);
                        //
                        // takeProfitOrder = new Order();
                        // takeProfitOrder.orderRef("ave" + periodSeconds);
                        // takeProfitOrder.orderId(ib.getNextOrderId());
                        // takeProfitOrder.action(Action.SELL);
                        // takeProfitOrder.orderType(OrderType.LMT);
                        // takeProfitOrder.totalQuantity(totalQuantity);
                        // takeProfitOrder.lmtPrice(profitPrice);
                        // takeProfitOrder.parentId(openOrder.orderId());
                        // takeProfitOrder.transmit(true);
                        //
                        // placeOrders(ib);
                        //
                        // } else if (openOrderIsActive && takeProfitOrderIsActive && needModify) {
                        // openOrder.lmtPrice(openPrice);
                        // takeProfitOrder.lmtPrice(profitPrice);
                        //
                        // modifyOrders(ib);
                        // }
                        //
                        // prevAveragePrice = averagePrice;
                        // }
                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }
            }

        }.start();
    }

    /**
     * Gets the average close.
     *
     * @param close
     *            the close
     * @param offset
     *            - positive value indicating offset of the window from the end
     */
    @Nullable
    public static Double getAverageClose(TDoubleArrayList close, int offset, int period) {
        int size = close.size();
        if (size < period + offset) {
            return null;
        }

        double acc = 0;
        int start = size - period - offset;
        int end = start + period;

        for (int i = start; i < end; i++) {
            acc = acc + close.get(i);
        }

        return acc / period;
    }

    private String fmt(int decimals, Double... value) {
        String floatFormat = "%." + decimals + "f";
        return Arrays.stream(value)
                .map(d -> String.format(floatFormat, d))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String fmtSign(int decimals, Double... value) {
        String floatFormat = "%+." + decimals + "f";
        return Arrays.stream(value)
                .map(d -> String.format(floatFormat, d))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    public static void main(String[] args) {
        TDoubleArrayList close = new TDoubleArrayList();
        close.add(0);
        close.add(1);
        close.add(1);
        close.add(1);
        close.add(2);

        System.out.println(getAverageClose(close, 0, 3));
        System.out.println(getAverageClose(close, 1, 3));
        System.out.println(getAverageClose(close, 2, 3));
    }

    @Override
    public void runLoop() {
        // TODO Auto-generated method stub
        
    }
}
