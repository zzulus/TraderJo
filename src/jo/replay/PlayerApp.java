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
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import jo.bot.Bot;
import jo.bot.DonchianBot;
import jo.constant.Stocks;
import jo.model.MarketData;
import jo.position.DollarValueWithRiskPositionSizeStrategy;
import jo.recording.event.AbstractEvent;
import jo.recording.event.EventTypeRegistry;
import jo.recording.event.MarketDepthEvent;
import jo.recording.event.TickSizeEvent;

public class PlayerApp {
    private static final Logger log = LogManager.getLogger(PlayerApp.class);
    private static Map<File, List<AbstractEvent>> EVENTS_CACHE = new HashMap<>();
    private static final String SYMBOL = "AAPL";
    private static final Contract CONTRACT = Stocks.smartOf(SYMBOL);

    private Contract contract;
    private ReplayContext ctx;
    private ReplayBroker replayBroker;
    private EventDriver driver;
    private ReplayOrderManager orderManager;
    private Bot bot;

    public static void main(String[] args) {
        List<File> files = Lists.newArrayList(
                new File("D:\\autobot\\TraderJo\\log\\2018-05-09\\market\\Market-AAPL-2018-05-09T05-51-57.826.log"));

        System.out.print("LowerP\tUpperP\tTotal Profit");
        for (int i = 0; i < files.size(); i++) {
            System.out.print(String.format("\tTrades #%1$s\tPotential Profit #%1$s", i + 1));
        }
        System.out.println();

        DollarValueWithRiskPositionSizeStrategy positionSizeStrategy = new DollarValueWithRiskPositionSizeStrategy(1000, 10);

        for (double out = 0.09; out < 2.0; out += 0.02) {
            for (int lowerPeriod = 1; lowerPeriod < 50; lowerPeriod += 1) {
                for (int upperPeriod = 1; upperPeriod < 50; upperPeriod += 1) {
                    // System.out.println(String.format("in %.2f, out %.2f", in, out));

                    double totalProfit = 0;
                    StringBuilder t = new StringBuilder();

                    for (File file : files) {
                        DonchianBot bot = new DonchianBot(CONTRACT, positionSizeStrategy, 0.7);
                        bot.lowerPeriod = lowerPeriod;
                        bot.upperPeriod = upperPeriod;
                        PlayerApp player = new PlayerApp();
                        Stats stats = player.replay(file, bot);
                        bot.shutdown();

                        player.ctx.getMarketData(SYMBOL).getSignal().signalAll();

                        t.append(String.format("\t%d\t%.2f",
                                stats.getFilledOrders().size(),
                                stats.getPotentialPnl() - stats.getCommissions()));
                        totalProfit += stats.getPotentialPnl() - stats.getCommissions();
                        // System.out.println(stats);
                    }

                    StringBuilder h = new StringBuilder();
                    //h.append(String.format("%.2f\t%.2f\t%.2f", lowerPeriod, upperPeriod, totalProfit));
                    h.append(String.format("%d\t%d\t%.2f", lowerPeriod, upperPeriod, totalProfit));

                    // System.out.println(String.format("\t\t\t\t\t\t\t%.2f", totalProfit));
                    System.out.println(h.append(t));
                }
            }
        }

        System.exit(0);
    }

    public Stats replay(File file, Bot bot) {
        initReplayContext();

        this.bot = bot;
        bot.init(ctx);

        readAndPlay(file);

        Stats stats = ctx.getOrderManager().getStats();
        return stats;
    }

    private void initReplayContext() {
        this.contract = CONTRACT;

        this.ctx = new ReplayContext();

        this.replayBroker = new ReplayBroker(ctx);
        this.driver = new EventDriver(ctx);
        this.orderManager = new ReplayOrderManager(ctx);

        ctx.setReplayBroker(replayBroker);
        ctx.setOrderManager(orderManager);

        MarketData marketData = ctx.initMarketData(contract);
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
            driver.handleReplayEvent(contract, event);
            bot.runLoop();
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
            } catch (JsonEOFException eofEx) {
                // ignore, file was not closed properly
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            EVENTS_CACHE.put(file, events);
        }
        return events;
    }
}
