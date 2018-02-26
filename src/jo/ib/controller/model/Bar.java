/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import jo.util.Formats;

public class Bar {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss"); // format for historical query

    private final long time;
    private final double high;
    private final double low;
    private final double open;
    private final double close;
    private final double wap;
    private final long volume;
    private final int count;

    public long getTime() {
        return this.time;
    }

    public double getHigh() {
        return this.high;
    }

    public double getLow() {
        return this.low;
    }

    public double getOpen() {
        return this.open;
    }

    public double getClose() {
        return this.close;
    }

    public double getWap() {
        return this.wap;
    }

    public long getVolume() {
        return this.volume;
    }

    public int getCount() {
        return this.count;
    }

    public Bar(long time, double high, double low, double open, double close, double wap, long volume, int count) {
        this.time = time;
        this.high = high;
        this.low = low;
        this.open = open;
        this.close = close;
        this.wap = wap;
        this.volume = volume;
        this.count = count;
    }

    public String formattedTime() {
        return Formats.fmtDate(this.time * 1000);
    }

    /** Format for query. */
    public static String format(long ms) {
        return FORMAT.format(new Date(ms));
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s %s", formattedTime(), this.open, this.high, this.low, this.close);
    }
}
