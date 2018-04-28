package jo.util;

public class PriceUtils {
    public static double fixPriceVariance(double price) {
        double minTick = 0.01;
        int d = (int) (price / minTick);
        return d * minTick;
    }

    public static Double fixPriceVariance(Double price) {
        if (price == null)
            return null;

        double minTick = 0.01;
        int d = (int) (price / minTick);
        return d * minTick;
    }
}
