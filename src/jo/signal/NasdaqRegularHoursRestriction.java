package jo.signal;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import com.ib.client.Contract;

import jo.app.TraderApp;
import jo.model.MarketData;

// nasdaq
// Pre-Market:     4:00 a.m. to 9:30 a.m.
// Regular Market: 9:30 a.m. to 4:00 p.m.
// After Market:   4:00 p.m. to 8:00 p.m.
public class NasdaqRegularHoursRestriction implements Signal {
    private static final ZoneId EST_ZONE = ZoneId.of("America/New_York");
    private int endOffsetMinutes;

    public NasdaqRegularHoursRestriction(int endOffsetMinutes) {
        this.endOffsetMinutes = endOffsetMinutes;
    }

    @Override
    public boolean isActive(TraderApp app, Contract contract, MarketData marketData) {
        // TODO switch to epochTime and precomputed ranges
        ZonedDateTime timeInNY = ZonedDateTime.now(EST_ZONE);

        int dayOfWeek = timeInNY.get(ChronoField.DAY_OF_WEEK);
        int hour = timeInNY.get(ChronoField.HOUR_OF_DAY);
        int minute = timeInNY.get(ChronoField.MINUTE_OF_HOUR);
        int longTime = hour * 100 + minute;

        return dayOfWeek >= 1 && dayOfWeek <= 5 &&
                longTime >= 930 && longTime <= 1600 - endOffsetMinutes;
    }

    public String getName() {
        return "NasdaqRegularHoursSignal";
    }

    public void setEndOffsetMinutes(int endOffsetMinutes) {
        this.endOffsetMinutes = endOffsetMinutes;
    }
}
