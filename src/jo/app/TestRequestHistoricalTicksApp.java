package jo.app;

import java.util.Date;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ib.client.Contract;
import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.ConnectionHandlerAdapter;
import jo.handler.IHistoricalDataHandler;
import jo.handler.IHistoricalTickHandler;
import jo.model.Bar;
import jo.model.Bars;
import jo.util.AsyncVal;

// http://etfdb.com/type/equity/all/leveraged/#etfs&sort_name=three_month_average_volume&sort_order=desc&page=1
public class TestRequestHistoricalTicksApp {
    public static void main(String[] args) throws InterruptedException {
        IBService ib = new IBService();
        Contract contract = Stocks.TQQQ(true);

        AsyncVal<Bars> barsEx = new AsyncVal<>();

        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            @Override
            public void connected() {
                ib.reqHistoricalData(contract, "", 1, DurationUnit.DAY, BarSize._1_min, WhatToShow.TRADES, true, new IHistoricalDataHandler() {
                    final Bars bars = new Bars();

                    @Override
                    public void historicalDataEnd() {
                        barsEx.set(bars);
                    }

                    @Override
                    public void historicalData(Bar bar) {
                        bars.addBar(bar);
                    }
                });
            }
        });

        Bars bars = barsEx.get();

        AsyncVal<String> ticksEx = new AsyncVal<>();

        ib.reqHistoricalTicks(contract, "20180501 06:30:00", null, 1000, WhatToShow.TRADES, true, false, new IHistoricalTickHandler() {
            @Override
            public void historicalTick(int reqId, List<HistoricalTick> ticks, boolean last) {
                System.out.println("historicalTick: ");
                for (HistoricalTick tick : ticks) {
                    System.out.println(ToStringBuilder.reflectionToString(tick));
                }
            }

            @Override
            public void historicalTickBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean last) {
                // TODO Auto-generated method stub

            }

            @Override
            public void historicalTickLast(int reqId, List<HistoricalTickLast> ticks, boolean allReceived) {
                System.out.println("historicalTickLast: allReceived " + allReceived);
                for (HistoricalTickLast tick : ticks) {
                    System.out.println(new Date(tick.time() * 1000) + " " + ToStringBuilder.reflectionToString(tick));
                }
                ticksEx.set("");
            }

        });

        ticksEx.get();

        ib.disconnect();
        System.exit(0);
    }

    static void calcDays(Contract contract, Bars bars) {
        TDoubleList open = bars.getOpen();
        TDoubleList close = bars.getClose();
        TDoubleList low = bars.getLow();
        TDoubleList high = bars.getHigh();

        int size = open.size();

        TDoubleList hiLoDiffs = new TDoubleArrayList();
        TDoubleList openCloseDiffs = new TDoubleArrayList();

        for (int i = 0; i < size; i++) {
            double hiLoDiff = high.get(i) - low.get(i);
            hiLoDiffs.add(hiLoDiff);

            double openCloseDiff = open.get(i) - close.get(i);
            openCloseDiffs.add(openCloseDiff);
        }

        hiLoDiffs.sort();
        openCloseDiffs.sort();

        double closePrice = close.get(size - 1);
        double percent = hiLoDiffs.get(size / 2) * 100d / closePrice;
        System.out.println("==================");
        System.out.println(contract.symbol() + "    Close " + closePrice + ", percent " + Double.toString(percent).substring(0, 4));
        System.out.println("==================");

        for (int i = 1; i < 10; i++) {
            double val = hiLoDiffs.get(size * i / 10);
            System.out.println("HiLo P" + i + "0: " + val);
        }

        for (int i = 1; i < 10; i++) {
            double val = openCloseDiffs.get(size * i / 10);
            System.out.println("OpenClose P" + i + "0: " + val);
        }

        System.out.println();
    }
}
