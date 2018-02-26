package jo.signal;

import com.google.common.base.Preconditions;

public class RandomSignal implements Signal {
    private double weight;

    public RandomSignal(double weight) {
        Preconditions.checkArgument(weight > 0.0d && weight < 1.0d, "weight value should be in (0, 1): %s", weight);
        this.weight = weight;
    }

    @Override
    public boolean isActive() {
        return Math.random() <= weight;
    }
    
    public String getName() {
        return "RandomSignal";
    }
}
