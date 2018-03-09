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
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import jo.bot.Bot;
import jo.bot.MA90SecBot;
import jo.bot.RandomBot;
import jo.constant.Stocks;
import jo.model.MarketData;
import jo.recording.event.AbstractEvent;
import jo.recording.event.EventTypeRegistry;

public class Player {
    private static final Logger log = LogManager.getLogger(Player.class);
    private static Map<File, List<AbstractEvent>> EVENTS_CACHE = new HashMap<>();

    private Contract contract;
    private ReplayBroker ib;
    private ReplayApp app;

    public static void main(String[] args) {
        File file1 = new File("D:\\autobot\\TraderJo\\log\\2018-03-06\\Market-TQQQ.log");
        File file2 = new File("D:\\autobot\\TraderJo\\log\\2018-03-06\\Market-TQQQ.log");
        File file3 = new File("D:\\autobot\\TraderJo\\log\\2018-03-08\\Market-TQQQ.log");
        File[] files = new File[] { file1, file2, file3 };

        System.out.println(String.format("In\tOut\tTotal Profit"
                + "\tTrades\tP&L\tPotential P&L\tCommissions\tPotential Profit"
                + "\tTrades\tP&L\tPotential P&L\tCommissions\tPotential Profit"
                + "\tTrades\tP&L\tPotential P&L\tCommissions\tPotential Profit"));

        for (double in = -0.01; in < 0.10; in += 0.01) {
            for (double out = 0.09; out < 2.0; out += 0.01) {
                // System.out.println(String.format("in %.2f, out %.2f", in, out));

                double totalProfit = 0;
                StringBuilder t = new StringBuilder();

                for (File file : files) {
                    MA90SecBot bot = new MA90SecBot(Stocks.TQQQ(true), 100, in, out);
                    // RandomBot bot = new RandomBot(Stocks.TQQQ(true), 100, in, out);
                    Player player = new Player();
                    Stats stats = player.replay(file, bot);
                    bot.shutdown();
                    
                    player.app.getMarketDataMap().values().forEach(md -> md.getUpdateSignal().signalAll());

                    t.append(String.format("\t%d\t%.2f\t%.2f\t%.2f\t%.2f",
                            stats.getFilledOrders().size(),
                            stats.getPnl(),
                            stats.getPotentialPnl(),
                            stats.getCommissions(),
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
