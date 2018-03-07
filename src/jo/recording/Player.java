package jo.recording;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.ib.client.Types.BarSize;

import jo.bot.Bot;
import jo.handler.ITopMktDataHandler;
import jo.model.MarketData;
import jo.recording.event.AbstractEvent;
import jo.recording.event.CommissionReportEvent;
import jo.recording.event.MarketDepthEvent;
import jo.recording.event.OpenOrderEvent;
import jo.recording.event.OrderErrorEvent;
import jo.recording.event.OrderStatusEvent;
import jo.recording.event.RealTimeBarEvent;
import jo.recording.event.TickPriceEvent;
import jo.recording.event.TickSizeEvent;
import jo.recording.event.TickStringEvent;
import jo.recording.event.TradeReportEvent;

public class Player {
    private static final Logger log = LogManager.getLogger(Player.class);
    private static final Map<String, Class<? extends AbstractEvent>> TYPE_NAME_TO_CLASS = ImmutableMap.<String, Class<? extends AbstractEvent>>builder()
            .put(CommissionReportEvent.TYPE, CommissionReportEvent.class)
            .put(MarketDepthEvent.TYPE, MarketDepthEvent.class)
            .put(OpenOrderEvent.TYPE, OpenOrderEvent.class)
            .put(OrderErrorEvent.TYPE, OrderErrorEvent.class)
            .put(OrderStatusEvent.TYPE, OrderStatusEvent.class)
            .put(RealTimeBarEvent.TYPE, RealTimeBarEvent.class)
            .put(TickPriceEvent.TYPE, TickPriceEvent.class)
            .put(TickSizeEvent.TYPE, TickSizeEvent.class)
            .put(TickStringEvent.TYPE, TickStringEvent.class)
            .put(TradeReportEvent.TYPE, TradeReportEvent.class)
            .build();

    public static void main(String[] args) {
        Player player = new Player();

        player.replay(null, new File("D:\\autobot\\TraderJo\\log\\2018-03-06\\Market-TQQQ.log"));
    }

    public Player() {

    }

    public void replay(Bot bot, File file) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        final MarketData marketData = new MarketData();
        marketData.initBars(BarSize._5_secs);
        
        final ITopMktDataHandler topMktDataHandler = marketData.getTopMktDataHandler();

        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                JsonNode json = objectMapper.readTree(line);
                String type = json.get("type").textValue();
                Class<? extends AbstractEvent> valueType = TYPE_NAME_TO_CLASS.get(type);

                AbstractEvent event = objectMapper.convertValue(json, valueType);

                if (event instanceof RealTimeBarEvent) {
                    RealTimeBarEvent typedEvent = (RealTimeBarEvent) event;
                    marketData.addBar(BarSize._5_secs, typedEvent.getBar());

                } else if (event instanceof TickPriceEvent) {
                    TickPriceEvent typedEvent = (TickPriceEvent) event;
                    topMktDataHandler.tickPrice(typedEvent.getTickType(), typedEvent.getPrice(), typedEvent.getCanAutoExecute());

                } else if (event instanceof TickSizeEvent) {
                    TickSizeEvent typedEvent = (TickSizeEvent) event;
                    topMktDataHandler.tickSize(typedEvent.getTickType(), typedEvent.getSize());

                } else if (event instanceof TickStringEvent) {
                    TickStringEvent typedEvent = (TickStringEvent) event;
                    topMktDataHandler.tickString(typedEvent.getTickType(), typedEvent.getValue());

                } else if (event instanceof MarketDepthEvent) {
                    MarketDepthEvent typedEvent = (MarketDepthEvent) event;
                    // TODO Add support of deep book
                    // topMktDataHandler.tickString(typedEvent.getTickType(), typedEvent.getValue());

                } else {
                    throw new RuntimeException("Unsupported event: " + ToStringBuilder.reflectionToString(event));
                }

                //System.out.println(ToStringBuilder.reflectionToString(event));
            }

        } catch (Exception e) {
            log.error(e, e);
        }
    }
}
