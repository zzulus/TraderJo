package jo.app.data;

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
import com.ib.controller.Formats;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import jo.model.Bars;
import jo.model.StatVar;
import jo.model.Stats;
import jo.recording.event.AbstractEvent;
import jo.recording.event.EventTypeRegistry;
import jo.recording.event.RealTimeBarEvent;

public class CreateContractStatsApp {
    private static File historicalsDir = new File("historical/2018-05-02-1m-20d");
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
        //System.out.println(symbol);

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

        double hiLoP90 = hiLoDiffs.get(size * 9 / 10);
        if (hiLoP90 < 0.05) {
            System.out.println(symbol + " HiLo P90: " + Formats.fmt(hiLoP90) + "   Price " + Formats.fmt(close.get(size - 1)));
        }

        Stats stat = new Stats();
        stat.setLastKnownPrice(close.get(size - 1));
        stat.setHiLo(StatVar.of(hiLoDiffs));
        stat.setOpenClose(StatVar.of(openCloseDiffs));

        File symbolFile = new File("data", symbol.toUpperCase() + "-HistoricalStat.json");
        try (PrintWriter ps = new PrintWriter(new FileOutputStream(symbolFile))) {
            String str = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(stat);
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
