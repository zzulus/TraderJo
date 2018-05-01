package jo.model;

import java.io.File;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HistoricalStat {
    private double hiLoAvg;
    private double hiLoP10;
    private double hiLoP20;
    private double hiLoP30;
    private double hiLoP40;
    private double hiLoP50;
    private double hiLoP60;
    private double hiLoP70;
    private double hiLoP80;
    private double hiLoP90;

    private double openCloseAvg;
    private double openCloseP10;
    private double openCloseP20;
    private double openCloseP30;
    private double openCloseP40;
    private double openCloseP50;
    private double openCloseP60;
    private double openCloseP70;
    private double openCloseP80;
    private double openCloseP90;

    @Nullable
    public static HistoricalStat tryLoad(String symbol) {
        File file = new File("data", symbol.toLowerCase() + "-HistoricalStat.txt");
        if (!file.exists())
            return null;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);

            JsonNode json = objectMapper.readTree(bytes);
            HistoricalStat value = objectMapper.convertValue(json, HistoricalStat.class);

            return value;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public double getHiLoAvg() {
        return hiLoAvg;
    }

    public void setHiLoAvg(double hiLoAvg) {
        this.hiLoAvg = hiLoAvg;
    }

    public double getHiLoP10() {
        return hiLoP10;
    }

    public void setHiLoP10(double hiLoP10) {
        this.hiLoP10 = hiLoP10;
    }

    public double getHiLoP20() {
        return hiLoP20;
    }

    public void setHiLoP20(double hiLoP20) {
        this.hiLoP20 = hiLoP20;
    }

    public double getHiLoP30() {
        return hiLoP30;
    }

    public void setHiLoP30(double hiLoP30) {
        this.hiLoP30 = hiLoP30;
    }

    public double getHiLoP40() {
        return hiLoP40;
    }

    public void setHiLoP40(double hiLoP40) {
        this.hiLoP40 = hiLoP40;
    }

    public double getHiLoP50() {
        return hiLoP50;
    }

    public void setHiLoP50(double hiLoP50) {
        this.hiLoP50 = hiLoP50;
    }

    public double getHiLoP60() {
        return hiLoP60;
    }

    public void setHiLoP60(double hiLoP60) {
        this.hiLoP60 = hiLoP60;
    }

    public double getHiLoP70() {
        return hiLoP70;
    }

    public void setHiLoP70(double hiLoP70) {
        this.hiLoP70 = hiLoP70;
    }

    public double getHiLoP80() {
        return hiLoP80;
    }

    public void setHiLoP80(double hiLoP80) {
        this.hiLoP80 = hiLoP80;
    }

    public double getHiLoP90() {
        return hiLoP90;
    }

    public void setHiLoP90(double hiLoP90) {
        this.hiLoP90 = hiLoP90;
    }

    public double getOpenCloseAvg() {
        return openCloseAvg;
    }

    public void setOpenCloseAvg(double openCloseAvg) {
        this.openCloseAvg = openCloseAvg;
    }

    public double getOpenCloseP10() {
        return openCloseP10;
    }

    public void setOpenCloseP10(double openCloseP10) {
        this.openCloseP10 = openCloseP10;
    }

    public double getOpenCloseP20() {
        return openCloseP20;
    }

    public void setOpenCloseP20(double openCloseP20) {
        this.openCloseP20 = openCloseP20;
    }

    public double getOpenCloseP30() {
        return openCloseP30;
    }

    public void setOpenCloseP30(double openCloseP30) {
        this.openCloseP30 = openCloseP30;
    }

    public double getOpenCloseP40() {
        return openCloseP40;
    }

    public void setOpenCloseP40(double openCloseP40) {
        this.openCloseP40 = openCloseP40;
    }

    public double getOpenCloseP50() {
        return openCloseP50;
    }

    public void setOpenCloseP50(double openCloseP50) {
        this.openCloseP50 = openCloseP50;
    }

    public double getOpenCloseP60() {
        return openCloseP60;
    }

    public void setOpenCloseP60(double openCloseP60) {
        this.openCloseP60 = openCloseP60;
    }

    public double getOpenCloseP70() {
        return openCloseP70;
    }

    public void setOpenCloseP70(double openCloseP70) {
        this.openCloseP70 = openCloseP70;
    }

    public double getOpenCloseP80() {
        return openCloseP80;
    }

    public void setOpenCloseP80(double openCloseP80) {
        this.openCloseP80 = openCloseP80;
    }

    public double getOpenCloseP90() {
        return openCloseP90;
    }

    public void setOpenCloseP90(double openCloseP90) {
        this.openCloseP90 = openCloseP90;
    }

}
