package jo.signal;

import java.util.List;

import com.google.common.base.Preconditions;
import com.ib.client.Contract;

import jo.app.TraderApp;
import jo.model.MarketData;

public class AnySignal implements Signal {
    private List<Signal> signals;

    public AnySignal(List<Signal> signals) {
        Preconditions.checkNotNull(signals, "signals is null");
        Preconditions.checkArgument(!signals.isEmpty(), "signals is empty");
        this.signals = signals;
    }

    @Override
    public boolean isActive(TraderApp app, Contract contract, MarketData marketData) {
        for (Signal signal : signals) {
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