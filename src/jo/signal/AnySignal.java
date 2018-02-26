package jo.signal;

import java.util.List;

import com.google.common.base.Preconditions;

public class AnySignal implements Signal {
    private List<Signal> signals;

    public AnySignal(List<Signal> signals) {
        Preconditions.checkNotNull(signals, "signals is null");
        Preconditions.checkArgument(!signals.isEmpty(), "signals is empty");
        this.signals = signals;
    }

    @Override
    public boolean isActive() {
        for (Signal signal : signals) {
            if (signal.isActive()) {
                return true;
            }
        }

        return false;
    }
    
    public String getName() {
        return "AnySignal";
    }
}