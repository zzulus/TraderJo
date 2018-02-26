package jo.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.TickType;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.MktDataType;

import jo.handler.ITopMktDataHandler;

public class MarketData {
    private static final Logger log = LogManager.getLogger(MarketData.class);

    private Map<BarSize, Bars> barsMap = new ConcurrentHashMap<>();
    private CircularFifoQueue<MarketDataTrade> trades = new CircularFifoQueue<>(4096);
    private double todayOpenPrice;
    private double prevDayClosePrice;
    private double todayLowPrice;
    private double todayHighPrice;

    private double high13Price;
    private double low13Price;
    private double high26Price;
    private double low26Price;
    private double high52Price;
    private double low52Price;

    private double askPrice;
    private int askSize;
    private double bidPrice;
    private int bidSize;
    private double lastPrice;
    private int lastSize;
    private int averageVolume;
    private int todayVolume;

    private ITopMktDataHandler topMktDataHandler = new TopMktDataHandler();

    public void addTrade(MarketDataTrade trade) {
        synchronized (trades) {
            trades.add(trade);
        }
    }

    public MarketDataTrade getLastTrade() {
        synchronized (trades) {
            return trades.get(trades.size() - 1);
        }
    }

    public MarketDataTrade getTrade(int tradesBack) {
        synchronized (trades) {
            return trades.get(trades.size() - 1 - tradesBack);
        }
    }

    public double getTodayOpenPrice() {
        return todayOpenPrice;
    }

    public double getPrevDayClosePrice() {
        return prevDayClosePrice;
    }

    public double getTodayLowPrice() {
        return todayLowPrice;
    }

    public double getTodayHighPrice() {
        return todayHighPrice;
    }

    public ITopMktDataHandler getTopMktDataHandler() {
        return topMktDataHandler;
    }

    public void addBar(BarSize barSize, Bar bar) {
        barsMap.get(barSize).addBar(bar);
    }

    public Bars getBars(BarSize barSize) {
        return barsMap.get(barSize);
    }

    public Bars initBars(BarSize barSize) {
        return barsMap.computeIfAbsent(barSize, (k) -> new Bars());
    }

    public Map<BarSize, Bars> getBarsMap() {
        return barsMap;
    }

    public CircularFifoQueue<MarketDataTrade> getTrades() {
        return trades;
    }

    public double getHigh13Price() {
        return high13Price;
    }

    public double getLow13Price() {
        return low13Price;
    }

    public double getHigh26Price() {
        return high26Price;
    }

    public double getLow26Price() {
        return low26Price;
    }

    public double getHigh52Price() {
        return high52Price;
    }

    public double getLow52Price() {
        return low52Price;
    }

    public double getAskPrice() {
        return askPrice;
    }

    public int getAskSize() {
        return askSize;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public int getBidSize() {
        return bidSize;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public int getLastSize() {
        return lastSize;
    }

    public int getAverageVolume() {
        return averageVolume;
    }

    public int getTodayVolume() {
        return todayVolume;
    }

    // TODO Extract
    private class TopMktDataHandler implements ITopMktDataHandler {

        @Override
        public void tickPrice(TickType tickType, double price, int canAutoExecute) {
            log.info("tickPrice: {} {}", tickType, price);

            switch (tickType) {
            case ASK:
                askPrice = price;
                break;
            case BID:
                bidPrice = price;
                break;
            case LAST:
                lastPrice = price;
                break;
            case HIGH:
                todayHighPrice = price;
                break;
            case LOW:
                todayLowPrice = price;
                break;
            case CLOSE:
                prevDayClosePrice = price;
                break;
            case OPEN:
                todayOpenPrice = price;
                break;
            case HIGH_13_WEEK:
                high13Price = price;
                break;
            case HIGH_26_WEEK:
                high26Price = price;
                break;
            case HIGH_52_WEEK:
                high52Price = price;
                break;
            case LOW_13_WEEK:
                low13Price = price;
                break;
            case LOW_26_WEEK:
                low26Price = price;
                break;
            case LOW_52_WEEK:
                low52Price = price;
                break;
            default:
                break;
            }
        }

        @Override
        public void tickSize(TickType tickType, int size) {
            switch (tickType) {
            case ASK_SIZE:
                askSize = size;
                break;
            case BID_SIZE:
                bidSize = size;
                break;
            case LAST_SIZE:
                lastSize = size;
                break;
            case AVG_VOLUME:
                averageVolume = size;
                break;
            case VOLUME:
                todayVolume = size;
                break;
            default:
                break;
            }
        }

        @Override
        public void tickString(TickType tickType, String value) {
            switch (tickType) {
            case RT_TRD_VOLUME:
                log.info("tickString: {} {}", tickType, value);
                processRTVolume(value);
                break;

            default:
                break;
            }
        }

        private void processRTVolume(String value) {
            // 0 last trade's price,
            // 1 size
            // 2 time
            // 3 current day's total traded volume,
            // 4 Volume Weighted Average Price (VWAP)
            // 5 whether or not the trade was filled by a single market maker

            // Ex: ;0;1519636841808;21;170.83991182;true

            String[] split = StringUtils.splitPreserveAllTokens(value, ';');
            if (split.length == 6) {
                String priceStr = split[0];
                String sizeStr = split[1];
                String timeStr = split[2];
                String todayVolumeStr = split[3];
                String vwapStr = split[4];

                if (!priceStr.isEmpty() && !sizeStr.isEmpty() && !timeStr.isEmpty() && !todayVolumeStr.isEmpty() && !vwapStr.isEmpty()) {
                    double price = Double.parseDouble(priceStr);
                    int size = Integer.parseInt(sizeStr);
                    long time = Long.parseLong(timeStr);
                    int dayTotalVolume = Integer.parseInt(todayVolumeStr);
                    double vwap = Double.parseDouble(vwapStr);

                    MarketDataTrade trade = new MarketDataTrade(price, size, time, dayTotalVolume, vwap);
                    addTrade(trade);
                }
            }
        }

        @Override
        public void tickSnapshotEnd() {

        }

        @Override
        public void marketDataType(MktDataType marketDataType) {

        }

    }
}
