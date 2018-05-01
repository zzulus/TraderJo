package jo.app.data;

import static jo.util.PriceUtils.fixPriceVariance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jo.model.Bars;
import jo.model.HistoricalStat;
import jo.recording.event.AbstractEvent;
import jo.recording.event.EventTypeRegistry;
import jo.recording.event.RealTimeBarEvent;
import jo.tech.SMA;
import jo.util.PriceUtils;

public class CreateHistoricalStatsApp {
    private static File historicalsDir = new File("historical\\2018-03-26-1m-90d");
    private static File dataDir = new File("data");
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }

    public static void main(String[] args) throws Exception {
        for (File file : historicalsDir.listFiles()) {
            Bars bars = loadBars(file);
            String symbol = StringUtils.substringBefore(file.getName(), ".");
            calculate(bars, symbol);
        }
    }

    private static void calculate(Bars bars, String symbol) throws Exception {
        System.out.println(symbol);

        TDoubleList open = bars.getOpen();
        TDoubleList close = bars.getClose();
        TDoubleList low = bars.getLow();
        TDoubleList high = bars.getHigh();

        int size = open.size();

        TDoubleList hiLoDiffs = new TDoubleArrayList();
        TDoubleList openCloseDiffs = new TDoubleArrayList();

        for (int i = 0; i < size; i++) {
            double hiLoDiff = high.get(i) - low.get(i);
            hiLoDiffs.add(hiLoDiff);

            double openCloseDiff = Math.abs(open.get(i) - close.get(i));
            openCloseDiffs.add(openCloseDiff);
        }

        hiLoDiffs.sort();
        openCloseDiffs.sort();

        for (int i = 1; i < 10; i++) {
            double val = hiLoDiffs.get(size * i / 10);
            System.out.println("HiLo P" + i + "0: " + val);
        }

        for (int i = 1; i < 10; i++) {
            double val = openCloseDiffs.get(size * i / 10);
            System.out.println("OpenClose P" + i + "0: " + val);
        }

        HistoricalStat stat = new HistoricalStat();
        stat.setHiLoAvg(fixPriceVariance(SMA.of(hiLoDiffs)));
        stat.setHiLoP10(fixPriceVariance(hiLoDiffs.get(size * 1 / 10)));
        stat.setHiLoP20(fixPriceVariance(hiLoDiffs.get(size * 2 / 10)));
        stat.setHiLoP30(fixPriceVariance(hiLoDiffs.get(size * 3 / 10)));
        stat.setHiLoP40(fixPriceVariance(hiLoDiffs.get(size * 4 / 10)));
        stat.setHiLoP50(fixPriceVariance(hiLoDiffs.get(size * 5 / 10)));
        stat.setHiLoP60(fixPriceVariance(hiLoDiffs.get(size * 6 / 10)));
        stat.setHiLoP70(fixPriceVariance(hiLoDiffs.get(size * 7 / 10)));
        stat.setHiLoP80(fixPriceVariance(hiLoDiffs.get(size * 8 / 10)));
        stat.setHiLoP90(fixPriceVariance(hiLoDiffs.get(size * 9 / 10)));

        stat.setOpenCloseAvg(fixPriceVariance(SMA.of(openCloseDiffs)));
        stat.setOpenCloseP10(fixPriceVariance(openCloseDiffs.get(size * 1 / 10)));
        stat.setOpenCloseP20(fixPriceVariance(openCloseDiffs.get(size * 2 / 10)));
        stat.setOpenCloseP30(fixPriceVariance(openCloseDiffs.get(size * 3 / 10)));
        stat.setOpenCloseP40(fixPriceVariance(openCloseDiffs.get(size * 4 / 10)));
        stat.setOpenCloseP50(fixPriceVariance(openCloseDiffs.get(size * 5 / 10)));
        stat.setOpenCloseP60(fixPriceVariance(openCloseDiffs.get(size * 6 / 10)));
        stat.setOpenCloseP70(fixPriceVariance(openCloseDiffs.get(size * 7 / 10)));
        stat.setOpenCloseP80(fixPriceVariance(openCloseDiffs.get(size * 8 / 10)));
        stat.setOpenCloseP90(fixPriceVariance(openCloseDiffs.get(size * 9 / 10)));

        File symbolFile = new File(dataDir, symbol.toUpperCase() + "-HistoricalStat.txt");
        try (PrintWriter ps = new PrintWriter(new FileOutputStream(symbolFile))) {
            String str = objectMapper.writeValueAsString(stat);
            ps.println(str);
            ps.flush();
        }
    }

    private static Bars loadBars(File file) {
        Bars bars = new Bars();

        List<AbstractEvent> events = loadEvents(file);
        for (AbstractEvent event : events) {
            if (event instanceof RealTimeBarEvent) {
                RealTimeBarEvent rtEvent = (RealTimeBarEvent) event;
                bars.addBar(rtEvent.getBar());
            }
        }

        return bars;
    }

    private static List<AbstractEvent> loadEvents(File file) {
        List<AbstractEvent> events = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                JsonNode json = objectMapper.readTree(line);
                String type = json.get("type").textValue();
                Class<? extends AbstractEvent> valueType = EventTypeRegistry.getByType(type);

                AbstractEvent event = objectMapper.convertValue(json, valueType);
                events.add(event);
            }
        } catch (JsonEOFException eofEx) {
            // ignore, file was not closed properly
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return events;
    }

}
