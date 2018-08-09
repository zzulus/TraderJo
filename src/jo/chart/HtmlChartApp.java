package jo.chart;

import static jo.util.Formats.fmt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.client.Types.BarSize;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TLongList;
import jo.model.Bar;
import jo.model.BarType;
import jo.model.Bars;
import jo.model.MarketData;
import jo.recording.event.AbstractEvent;
import jo.recording.event.EventTypeRegistry;
import jo.recording.event.MarketDepthEvent;
import jo.recording.event.RealTimeBarEvent;
import jo.recording.event.TickSizeEvent;
import jo.tech.EMA;
import jo.util.Formats;

public class HtmlChartApp {
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) {
        List<AbstractEvent> events = loadBars();
        MarketData md = new MarketData();

        for (AbstractEvent event : events) {
            RealTimeBarEvent barEvent = (RealTimeBarEvent) event;
            Bar rtBar = barEvent.getBar();
            md.realtimeBar(rtBar);
        }

        Bars oneMinBars = md.getBars(BarSize._1_min);
        int size = oneMinBars.getSize();

        TLongList time = oneMinBars.getTime();
        TDoubleList low = oneMinBars.getLow();
        TDoubleList open = oneMinBars.getOpen();
        TDoubleList close = oneMinBars.getClose();
        TDoubleList high = oneMinBars.getHigh();

        for (int i = 0; i < size; i++) {
            EMA ema = new EMA(oneMinBars, BarType.CLOSE, 18, size - i);

            String s = String.format("['%s',  %.2f, %.2f, %.2f, %.2f,  %s]",
                    TIME_FMT.format(new Date(time.get(i) * 1000)),
                    low.get(i),
                    open.get(i),
                    close.get(i),
                    high.get(i),                    
                    fmt(ema.get())                    

            );
            System.out.print(s);

            if (i < size - 1) {
                System.out.print(',');
            }

            System.out.println();
        }

    }

    private static List<AbstractEvent> loadBars() {
        File input = new File("D:\\autobot\\TraderJo\\log\\2018-05-09\\market\\Market-AAPL-2018-05-09T05-51-57.826.log");

        Set<Class<? extends AbstractEvent>> validEventTypes = new HashSet<>();
        validEventTypes.add(RealTimeBarEvent.class);

        List<AbstractEvent> events = loadEvents(input, validEventTypes);
        return events;
    }

    private static List<AbstractEvent> loadEvents(File file, Set<Class<? extends AbstractEvent>> validEventTypes) {
        List<AbstractEvent> events = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

            while ((line = in.readLine()) != null) {
                JsonNode json = objectMapper.readTree(line);
                String type = json.get("type").textValue();

                Class<? extends AbstractEvent> valueType = EventTypeRegistry.getByType(type);
                if (validEventTypes.contains(valueType)) {
                    AbstractEvent event = objectMapper.convertValue(json, valueType);
                    events.add(event);
                }
            }
        } catch (JsonEOFException eofEx) {
            // ignore, file was not closed properly
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return events;
    }
}
