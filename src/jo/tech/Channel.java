package jo.tech;

public class Channel {
    private final double upper;
    private final double lower;
    private final double middle;

    public Channel(double lower, double upper) {
        this(lower, (upper + lower) / 2, upper);
    }

    public Channel(double lower, double middle, double upper) {
        if (lower > upper) {
            throw new IllegalArgumentException("lower bound > upper bound");
        }

        this.upper = upper;
        this.middle = middle;
        this.lower = lower;
    }

    public double getUpper() {
        return upper;
    }

    public double getLower() {
        return lower;
    }

    public double getMiddle() {
        return middle;
    }

}
