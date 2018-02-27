package jo.signal;

import java.util.List;

import com.google.common.base.Preconditions;
import com.ib.client.Contract;

import jo.app.App;
import jo.model.MarketData;

public class AllSignals implements Signal {
    private List<Signal> signals;

    public AllSignals(List<Signal> signals) {
        Preconditions.checkNotNull(signals, "signals is null");
        Preconditions.checkArgument(!signals.isEmpty(), "signals is empty");
        this.signals = signals;
    }

    @Override
    public boolean isActive(App app, Contract contract, MarketData marketData) {
        for (Signal signal : signals) {
            if (!signal.isActive(app, contract, marketData)) {
                return false;
            }
        }

        return true;
    }

    public String getName() {
        return "AllSignals";
    }

}