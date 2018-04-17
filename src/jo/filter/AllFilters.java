package jo.filter;

import java.util.List;

import com.google.common.base.Preconditions;
import com.ib.client.Contract;

import jo.model.IApp;
import jo.model.MarketData;

public class AllFilters implements Filter {
    private List<Filter> signals;

    public AllFilters(List<Filter> signals) {
        Preconditions.checkNotNull(signals, "signals is null");
        Preconditions.checkArgument(!signals.isEmpty(), "signals is empty");
        this.signals = signals;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        for (Filter signal : signals) {
            if (!signal.isActive(app, contract, marketData)) {
                //System.out.println(signal.getClass() + " is not active");
                return false;
            }
        }

        return true;
    }

    public String getName() {
        return "AllSignals";
    }

}