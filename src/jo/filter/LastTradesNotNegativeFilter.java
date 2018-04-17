package jo.filter;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import com.ib.client.Contract;

import jo.model.IApp;
import jo.model.MarketData;
import jo.model.MarketDataTrade;

public class LastTradesNotNegativeFilter implements Filter {
    private int len;

    public LastTradesNotNegativeFilter(int len) {
        this.len = len;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        CircularFifoQueue<MarketDataTrade> trades = marketData.getTrades();
        int size = trades.size();
        boolean isActive = true;
        if (size < len) {
            return false;
        }

        int end = size - 1;
        int start = size - len;

        for (int i = start; i < end; i++) {
            double p1 = trades.get(i).getPrice();
            double p2 = trades.get(i + 1).getPrice();
            if (p1 > p2) {
                isActive = false;
                break;
            }
        }

        return isActive;
    }

    @Override
    public String getName() {
        return "";
    };
}
