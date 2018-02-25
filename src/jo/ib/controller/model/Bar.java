/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import jo.ib.controller.util.Formats;

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

    public long time() {
        return this.time;
    }

    public double high() {
        return this.high;
    }

    public double low() {
        return this.low;
    }

    public double open() {
        return this.open;
    }

    public double close() {
        return this.close;
    }

    public double wap() {
        return this.wap;
    }

    public long volume() {
        return this.volume;
    }

    public int count() {
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
