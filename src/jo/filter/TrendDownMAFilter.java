package jo.filter;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.TDoubleList;
import jo.controller.IApp;
import jo.model.MarketData;

public class TrendDownMAFilter implements Filter {
    private int period;
    private TDoubleList close;

    public TrendDownMAFilter(int period) {
        this.period = period;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        if (close == null) {
            close = marketData.getBars(BarSize._5_secs).getClose();
        }

        try {
            int size = close.size();
            if (size < period) {
                return false;
            }

            double acc = 0;
            for (int i = size - period; i < size; i++) {
                acc = acc + close.get(i);
            }
            double avg = acc / period;

            return marketData.getLastPrice() < avg;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getName() {
        return "Below indicator";
    }

}
