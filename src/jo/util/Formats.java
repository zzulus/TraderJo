package jo.util;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Formats {
    private static final Format FMT2 = new DecimalFormat("#,##0.00");
    private static final Format FMT0 = new DecimalFormat("#,##0");
    private static final Format PCT = new DecimalFormat("0.0%");
    private static final SimpleDateFormat DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");

    /** Format with two decimals. */
    public static String fmt(double v) {
        return v == Double.MAX_VALUE ? null : FMT2.format(v);
    }

    /** Format with two decimals; return null for zero. */
    public static String fmtNz(double v) {
        return v == Double.MAX_VALUE || v == 0 ? null : FMT2.format(v);
    }

    /** Format with no decimals. */
    public static String fmt0(double v) {
        return v == Double.MAX_VALUE ? null : FMT0.format(v);
    }

    /** Format as percent with one decimal. */
    public static String fmtPct(double v) {
        return v == Double.MAX_VALUE ? null : PCT.format(v);
    }

    public static String fmtDate(long ms) {
        return DATE_TIME.format(new Date(ms));
    }

    public static String fmtDate(Date d) {
        return DATE_TIME.format(d);
    }

    public static String fmtTime(long ms) {
        return TIME.format(new Date(ms));
    }

    public static String fmtTime(Date d) {
        return TIME.format(d);
    }
}
