package jo.recording.event;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class EventTypeRegistry {
    private static final Map<String, Class<? extends AbstractEvent>> REGISTRY = ImmutableMap.<String, Class<? extends AbstractEvent>>builder()
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

    public static Class<? extends AbstractEvent> getByType(String type) {
        return REGISTRY.get(type);
    }
}
