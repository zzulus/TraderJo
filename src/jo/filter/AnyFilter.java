package jo.filter;

import java.util.List;

import com.google.common.base.Preconditions;
import com.ib.client.Contract;

import jo.app.IApp;
import jo.model.MarketData;

public class AnyFilter implements Filter {
    private List<Filter> signals;

    public AnyFilter(List<Filter> filters) {
        Preconditions.checkNotNull(filters, "filters is null");
        Preconditions.checkArgument(!filters.isEmpty(), "signals is empty");
        this.signals = filters;
    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        for (Filter signal : signals) {
            if (signal.isActive(app, contract, marketData)) {
                return true;
            }
        }

        return false;
    }

    public String getName() {
        return "AnySignal";
    }
}