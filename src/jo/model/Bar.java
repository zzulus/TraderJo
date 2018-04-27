/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import jo.util.Formats;

public class Bar {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss"); // format for historical query

    private long time;
    private double high;
    private double low;
    private double open;
    private double close;
    private double wap;
    private long volume;
    private int count;

    public Bar() {

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

    public long getTime() {
        return time;
    }

    public Date getTimeAsDate() {
        return new Date(this.time * 1000);
    }

    public void setTime(long time) {
        this.time = time;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getWap() {
        return wap;
    }

    public void setWap(double wap) {
        this.wap = wap;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
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
