package jo.util;

import java.util.concurrent.TimeUnit;

import com.ib.client.Types.BarSize;

public class BarSizeUtils {
    public static final BarSize REALTIME_BAR_SIZE = BarSize._5_secs;

    // getRatio(BarSize._1_min, BarSize._5_secs) -> 1_min / 5_secs
    public static int getRatio(BarSize from, BarSize to) {
        long fromSec = getInSeconds(from);
        long toSec = getInSeconds(to);

        if (fromSec < toSec) {
            throw new IllegalArgumentException("Cannot convert from " + from + " to " + to);
        }

        return (int) (fromSec / toSec);
    }

    public static long getInSeconds(BarSize barSize) {
        String[] split = barSize.name().split("\\_");
        int duration = Integer.parseInt(split[1]);
        String timeUnitName = split[2];
        TimeUnit timeUnit;

        // _1_secs, _5_secs, _10_secs, _15_secs, _30_secs, _1_min, _2_mins, _3_mins, _5_mins, _10_mins, _15_mins, _20_mins, _30_mins, _1_hour, _4_hours, _1_day, _1_week;
        if (timeUnitName.startsWith("sec")) {
            timeUnit = TimeUnit.SECONDS;
        } else if (timeUnitName.startsWith("min")) {
            timeUnit = TimeUnit.MINUTES;
        } else if (timeUnitName.startsWith("hour")) {
            timeUnit = TimeUnit.HOURS;
        } else if (timeUnitName.startsWith("day")) {
            timeUnit = TimeUnit.DAYS;
        } else if (timeUnitName.startsWith("week")) {
            duration = duration * 7;
            timeUnit = TimeUnit.DAYS;
        } else {
            throw new IllegalArgumentException("Unsupported size " + barSize);
        }

        return timeUnit.toSeconds(duration);
    }

    public static void main(String[] args) {
        System.out.println(getRatio(BarSize._1_min, BarSize._5_secs));
    }
}
