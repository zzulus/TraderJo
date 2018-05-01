package jo.app;

import java.util.concurrent.SynchronousQueue;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.ConnectionHandlerAdapter;
import jo.handler.IHistoricalDataHandler;
import jo.model.Bar;
import jo.model.Bars;

// http://etfdb.com/type/equity/all/leveraged/#etfs&sort_name=three_month_average_volume&sort_order=desc&page=1
public class TestRequestHistoricalTicksApp {
    public static void main(String[] args) throws InterruptedException {
        final IBService ib = new IBService();
        Contract contract = Stocks.TQQQ(true);

        SynchronousQueue<Bars> q = new SynchronousQueue<>();

        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            @Override
            public void connected() {
                ib.reqHistoricalData(contract, "20180226 23:59:59 GMT", 90, DurationUnit.DAY, BarSize._30_secs, WhatToShow.TRADES, true, new IHistoricalDataHandler() {
                    final Bars bars = new Bars();

                    @Override
                    public void historicalDataEnd() {
                        // log.info("End");
                        q.offer(bars);
                    }

                    @Override
                    public void historicalData(Bar bar) {
                        // log.info("Bar: {} / {}", bar.getLow(), bar.getHigh());
                        bars.addBar(bar);
                    }
                });
            }
        });

        Bars bars = q.take();

        calcDays(contract, bars);
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
