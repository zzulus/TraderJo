package jo.app;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ib.client.Contract;
import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.client.Types.WhatToShow;

import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.ConnectionHandlerAdapter;
import jo.handler.HistoricalTickHandlerAdapter;
import jo.util.AsyncExec;
import jo.util.AsyncVal;
import jo.util.Formats;

public class LeapsApp {
    public static void main(String[] args) throws Exception {
        SimpleDateFormat dateTimeFmt = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        String leapOptionDate = "20200117";
        IBService ib = new IBService();

        List<Holder> list = new ArrayList<>();

        List<String> lines = IOUtils.readLines(new FileInputStream("Leaps2020.txt"), StandardCharsets.ISO_8859_1);
        for (String line : lines) {
            String[] split = StringUtils.split(line, "\t", 2);

            Contract contract = Stocks.of(split[0], true);

            Holder holder = new Holder();
            holder.contract = contract;
            holder.companyName = split[1];

            list.add(holder);
        }

        AsyncVal<String> priceWait = AsyncVal.create();

        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            AsyncVal<Double> stockPriceVal;
            AsyncVal<Double> optionPriceWait;

            @Override
            public void connected() {
                //String endDateTime = dateTimeFmt.format(new Date());
                String endDateTime = "20180809 12:50:00";
                //System.out.println(endDateTime);

                AsyncExec.execute(() -> {
                    for (Holder h : list) {
                        stockPriceVal = AsyncVal.create();
                        System.out.println(h.contract.symbol());

                        ib.reqHistoricalTicks(h.contract, null, endDateTime, 1, WhatToShow.MIDPOINT, true, true,
                                new HistoricalTickHandlerAdapter() {
                                    public void historicalTick(int reqId, List<HistoricalTick> ticks, boolean last) {
                                        System.out.println("S historicalTick");
                                        if (!last)
                                            return;

                                        if (!ticks.isEmpty()) {
                                            stockPriceVal.set(ticks.get(0).price());
                                        } else {
                                            stockPriceVal.set(null);
                                        }
                                    }

                                    @Override
                                    public void historicalTickBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean last) {
                                        System.out.println("S historicalTickBidAsk");
                                        if (last) {
                                            if (!ticks.isEmpty()) {
                                                stockPriceVal.set(ticks.get(0).priceAsk());
                                            } else {
                                                stockPriceVal.set(null);
                                            }
                                        }
                                    }

                                    public void historicalTickLast(int reqId, List<HistoricalTickLast> ticks, boolean allReceived) {
                                        System.out.println("S historicalTickLast");
                                    }
                                });
                        final Double stockPrice = stockPriceVal.get();
                        if (stockPrice == null || stockPrice < 15) {
                            continue;
                        }

                        //System.out.println(h.contract.symbol() + "\t" + stockPrice + "\t" + h.companyName);

                        h.price = stockPrice;
                        List<Integer> strikePrices = getStrikePrices(stockPrice);
                        System.out.println(h.contract.symbol() + " " + stockPrice);

                        for (Integer strikePrice : strikePrices) {
                            Contract optionContract = Stocks.callOptionOf(h.contract.symbol(), true, strikePrice, leapOptionDate);
                            if (strikePrice < stockPrice) {
                                optionContract.right("C");
                            } else {
                                optionContract.right("P");
                            }

                            optionPriceWait = AsyncVal.create();

                            ib.reqHistoricalTicks(optionContract, null, endDateTime, 1, WhatToShow.MIDPOINT, true, true,
                                    new HistoricalTickHandlerAdapter() {
                                        public void historicalTick(int reqId, List<HistoricalTick> ticks, boolean last) {
                                            System.out.println("O historicalTick");

                                            if (!last)
                                                return;

                                            if (!ticks.isEmpty()) {
                                                double strike = optionContract.strike();
                                                double strikePrice = ticks.get(0).price();

                                                double timeValue;
                                                if (strike < stockPrice) { // call in money
                                                    timeValue = strikePrice - (stockPrice - strike);
                                                } else {
                                                    timeValue = strikePrice - (strike - stockPrice);
                                                }

                                                double timeValuePerc = timeValue / (stockPrice / 100d);

                                                System.out.println("  strike " + strike + ": " + strikePrice
                                                        + "   T " + Formats.fmt(timeValue)
                                                        + "   P " + Formats.fmt(timeValuePerc) + "%");
                                                h.strikes.add(strike);
                                                h.strikesPrice.add(strikePrice);
                                                optionPriceWait.set(strikePrice);
                                            } else {
                                                optionPriceWait.set(null);
                                            }
                                        }

                                        @Override
                                        public void historicalTickBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean last) {
                                            System.out.println("O historicalTickBidAsk");
                                        }

                                        public void historicalTickLast(int reqId, List<HistoricalTickLast> ticks, boolean allReceived) {
                                            System.out.println("O historicalTickLast");
                                        }
                                    });
                            optionPriceWait.get();
                        }
                        System.out.println();
                    }

                    priceWait.set("");
                });
            }

            @Override
            public void message(int id, int errorCode, String errorMsg) {
                System.err.println(errorCode + "  " + errorMsg);

                Set<Integer> ignore = Sets.newHashSet(2104, 2106, 2103, 2105);
                if (ignore.contains(errorCode))
                    return;

                if (stockPriceVal != null)
                    stockPriceVal.set(null);

                if (optionPriceWait != null)
                    optionPriceWait.set(null);

                Set<Integer> silent = Sets.newHashSet(200);
                if (!silent.contains(errorCode))
                    System.err.println(errorCode + "  " + errorMsg);
            }
        });

        priceWait.get();
        System.out.println("=========================================");
        for (Holder h : list) {
            StringBuilder buf = new StringBuilder();
            buf.append(h.contract.symbol());
            buf.append("\t");

            System.out.println(h.contract.symbol() + "\t" + h.price + "\t" + h.companyName);
        }

        System.exit(0);

    }

    static class Holder {
        Contract contract;
        String companyName;
        double price;
        List<Double> strikes = new ArrayList<>();
        List<Double> strikesPrice = new ArrayList<>();
    }

    static List<Integer> getStrikePrices(double price) {
        int i = (int) (((int) (price) / 5) * 5);
        if (i > price) {
            i -= 5;
        }
        List<Integer> result = Lists.newArrayList(i - 5, i, i + 5, i + 10);
        return result;
    }

}
