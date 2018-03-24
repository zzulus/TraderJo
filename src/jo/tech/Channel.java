package jo.tech;

public class Channel {
    private final double upper;
    private final double lower;
    private final double middle;

    public Channel(double upper, double lower) {
        this(upper, (upper + lower) / 2, lower);
    }

    public Channel(double upper, double middle, double lower) {
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
