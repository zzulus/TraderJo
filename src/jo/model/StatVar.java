package jo.model;

import static jo.util.PriceUtils.fixPriceVariance;

import gnu.trove.list.TDoubleList;
import jo.tech.SMA;

public class StatVar {
    private double avg;
    private double p10;
    private double p20;
    private double p30;
    private double p40;
    private double p50;
    private double p60;
    private double p70;
    private double p80;
    private double p90;
    private double p95;
    private double p99;

    public static StatVar of(TDoubleList values) {
        StatVar stat = new StatVar();

        int size = values.size();
        stat.setAvg(fixPriceVariance(SMA.of(values)));
        stat.setP10(fixPriceVariance(values.get(size * 1 / 10)));
        stat.setP20(fixPriceVariance(values.get(size * 2 / 10)));
        stat.setP30(fixPriceVariance(values.get(size * 3 / 10)));
        stat.setP40(fixPriceVariance(values.get(size * 4 / 10)));
        stat.setP50(fixPriceVariance(values.get(size * 5 / 10)));
        stat.setP60(fixPriceVariance(values.get(size * 6 / 10)));
        stat.setP70(fixPriceVariance(values.get(size * 7 / 10)));
        stat.setP80(fixPriceVariance(values.get(size * 8 / 10)));
        stat.setP90(fixPriceVariance(values.get(size * 9 / 10)));
        stat.setP95(fixPriceVariance(values.get(size * 95 / 100)));
        stat.setP99(fixPriceVariance(values.get(size * 99 / 100)));

        return stat;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public double getP10() {
        return p10;
    }

    public void setP10(double p10) {
        this.p10 = p10;
    }

    public double getP20() {
        return p20;
    }

    public void setP20(double p20) {
        this.p20 = p20;
    }

    public double getP30() {
        return p30;
    }

    public void setP30(double p30) {
        this.p30 = p30;
    }

    public double getP40() {
        return p40;
    }

    public void setP40(double p40) {
        this.p40 = p40;
    }

    public double getP50() {
        return p50;
    }

    public void setP50(double p50) {
        this.p50 = p50;
    }

    public double getP60() {
        return p60;
    }

    public void setP60(double p60) {
        this.p60 = p60;
    }

    public double getP70() {
        return p70;
    }

    public void setP70(double p70) {
        this.p70 = p70;
    }

    public double getP80() {
        return p80;
    }

    public void setP80(double p80) {
        this.p80 = p80;
    }

    public double getP90() {
        return p90;
    }

    public void setP90(double p90) {
        this.p90 = p90;
    }

    public double getP99() {
        return p99;
    }

    public void setP99(double p99) {
        this.p99 = p99;
    }

    public double getP95() {
        return p95;
    }

    public void setP95(double p95) {
        this.p95 = p95;
    }

}
