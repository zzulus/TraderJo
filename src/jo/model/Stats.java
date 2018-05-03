package jo.model;

import java.io.File;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Stats {
    private double lastKnownPrice;
    private StatVar hiLo;
    private StatVar openClose;

    @Nullable
    public static Stats tryLoad(String symbol) {
        File file = new File("data", symbol.toLowerCase() + "-HistoricalStat.json");
        if (!file.exists())
            return null;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);

            JsonNode json = objectMapper.readTree(bytes);
            Stats value = objectMapper.convertValue(json, Stats.class);

            return value;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public StatVar getHiLo() {
        return hiLo;
    }

    public void setHiLo(StatVar hiLo) {
        this.hiLo = hiLo;
    }

    public StatVar getOpenClose() {
        return openClose;
    }

    public void setOpenClose(StatVar openClose) {
        this.openClose = openClose;
    }

    public double getLastKnownPrice() {
        return lastKnownPrice;
    }

    public void setLastKnownPrice(double lastKnownPrice) {
        this.lastKnownPrice = lastKnownPrice;
    }

}
