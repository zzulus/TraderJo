package jo.model;

public class MarketDataTrade {
    // 0 last trade's price,
    // 1 size
    // 2 time
    // 3 current day's total traded volume,
    // 4 Volume Weighted Average Price (VWAP)
    // 5 whether or not the trade was filled by a single market maker

    private double price;
    private int size;
    private long time;
    private int dayTotalVolume;
    private double intradayVwap;

    public MarketDataTrade(double price, int size, long time, int dayTotalVolume, double intradayVwap) {
        this.price = price;
        this.size = size;
        this.time = time;
        this.dayTotalVolume = dayTotalVolume;
        this.intradayVwap = intradayVwap;
    }

    public double getPrice() {
        return price;
    }

    public int getSize() {
        return size;
    }

    public long getTime() {
        return time;
    }

    public int getDayTotalVolume() {
        return dayTotalVolume;
    }

    public double getIntradayVwap() {
        return intradayVwap;
    }

}
