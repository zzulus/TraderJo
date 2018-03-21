package jo.replay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import jo.bot.Bot;
import jo.bot.MA90SecBot;
import jo.bot.RandomBot;
import jo.constant.Stocks;
import jo.model.MarketData;
import jo.recording.event.AbstractEvent;
import jo.recording.event.EventTypeRegistry;
import jo.recording.event.MarketDepthEvent;
import jo.recording.event.TickSizeEvent;

public class PlayerApp {
    private static final Logger log = LogManager.getLogger(PlayerApp.class);
    private static Map<File, List<AbstractEvent>> EVENTS_CACHE = new HashMap<>();

    private Contract contract;
    private ReplayBroker ib;
    private ReplayApp app;

    public static void main(String[] args) {
        List<File> files = Lists.newArrayList(
                new File("D:\\autobot\\TraderJo\\log\\2018-03-06\\Market-TQQQ.log"),
                new File("D:\\autobot\\TraderJo\\log\\2018-03-07\\Market-TQQQ.log"),
                new File("D:\\autobot\\TraderJo\\log\\2018-03-08\\Market-TQQQ.log"),
                new File("D:\\autobot\\TraderJo\\log\\2018-03-09\\Market-TQQQ.log"));

        System.out.print("In\tOut\tTotal Profit");
        for (int i = 0; i < files.size(); i++) {
            System.out.print("\tTrades\tPotential Profit");
        }
        System.out.println();

        for (double in = -0.01; in < 0.10; in += 0.02) {
            for (double out = 0.09; out < 2.0; out += 0.02) {
                // System.out.println(String.format("in %.2f, out %.2f", in, out));

                double totalProfit = 0;
                StringBuilder t = new StringBuilder();

                for (File file : files) {
                    //MA90SecBot bot = new MA90SecBot(Stocks.TQQQ(true), 100, in, out);
                    RandomBot bot = new RandomBot(Stocks.TQQQ(true), 100, in, out);
                    PlayerApp player = new PlayerApp();
                    Stats stats = player.replay(file, bot);
                    bot.shutdown();

                    player.app.getMarketDataMap().values().forEach(md -> md.getUpdateSignal().signalAll());

                    t.append(String.format("\t%d\t%.2f",
                            stats.getFilledOrders().size(),
                            stats.getPotentialPnl() - stats.getCommissions()));
                    totalProfit += stats.getPotentialPnl() - stats.getCommissions();
                    // System.out.println(stats);
                }

                StringBuilder h = new StringBuilder();
                h.append(String.format("%.2f\t%.2f\t%.2f", in, out, totalProfit));

                // System.out.println(String.format("\t\t\t\t\t\t\t%.2f", totalProfit));
                System.out.println(h.append(t));
            }
        }
        
        System.exit(0);
    }

    public Stats replay(File file, Bot bot) {
        initApp();

        bot.start(ib, app);

        readAndPlay(file);

        Stats stats = ib.getOrderManager().getStats();
        return stats;
    }

    private void initApp() {
        contract = Stocks.TQQQ(true);
        ib = new ReplayBroker();
        app = new ReplayApp(ib);

        MarketData marketData = app.initMarketData(contract.symbol());
        for (BarSize barSize : BarSize.values()) {
            marketData.initBars(barSize);
        }
    }

    private void readAndPlay(File file) {
        List<AbstractEvent> events = loadEvents(file);
        for (AbstractEvent event : events) {
            if (event instanceof MarketDepthEvent
                    || event instanceof MarketDepthEvent
                    || event instanceof TickSizeEvent) {
                continue;
            }
            app.handleReplayEvent(contract, event);
        }
    }

    private List<AbstractEvent> loadEvents(File file) {
        List<AbstractEvent> events = EVENTS_CACHE.get(file);
        if (events == null) {
            events = new ArrayList<>();

            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                String line;

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

                while ((line = in.readLine()) != null) {
                    JsonNode json = objectMapper.readTree(line);
                    String type = json.get("type").textValue();
                    Class<? extends AbstractEvent> valueType = EventTypeRegistry.getByType(type);

                    AbstractEvent event = objectMapper.convertValue(json, valueType);
                    events.add(event);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            EVENTS_CACHE.put(file, events);
        }
        return events;
    }
}