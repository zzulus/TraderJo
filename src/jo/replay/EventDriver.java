package jo.replay;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ib.client.Contract;

import jo.model.MarketData;
import jo.recording.event.AbstractEvent;
import jo.recording.event.ErrorEvent;
import jo.recording.event.MarketDepthEvent;
import jo.recording.event.RealTimeBarEvent;
import jo.recording.event.TickPriceEvent;
import jo.recording.event.TickSizeEvent;
import jo.recording.event.TickStringEvent;

public class EventDriver {
    private ReplayContext ctx;

    public EventDriver(ReplayContext ctx) {
        this.ctx = ctx;
    }

    public void handleReplayEvent(Contract contract, AbstractEvent event) {
        MarketData marketData = ctx.getMarketData(contract.symbol());

        if (event instanceof RealTimeBarEvent) {
            RealTimeBarEvent typedEvent = (RealTimeBarEvent) event;
            marketData.realtimeBar(typedEvent.getBar()); // TODO make event do it itself

        } else if (event instanceof TickPriceEvent) {
            TickPriceEvent typedEvent = (TickPriceEvent) event;
            marketData.tickPrice(typedEvent.getTickType(), typedEvent.getPrice());

        } else if (event instanceof TickSizeEvent) {
            TickSizeEvent typedEvent = (TickSizeEvent) event;
            marketData.tickSize(typedEvent.getTickType(), typedEvent.getSize());

        } else if (event instanceof TickStringEvent) {
            TickStringEvent typedEvent = (TickStringEvent) event;
            marketData.tickString(typedEvent.getTickType(), typedEvent.getValue());

        } else if (event instanceof MarketDepthEvent) {
            MarketDepthEvent typedEvent = (MarketDepthEvent) event;
            // TODO Add support of deep book
            // topMktDataHandler.tickString(typedEvent.getTickType(), typedEvent.getValue());
            marketData.updateMktDepth(typedEvent.getPosition(), typedEvent.getMarketMaker(), typedEvent.getOperation(), typedEvent.getSide(), typedEvent.getPrice(), typedEvent.getSize());

        } else if (event instanceof ErrorEvent) {
            // TODO skip for now, needed for deep book reset
        } else {
            throw new RuntimeException("Unsupported event: " + ToStringBuilder.reflectionToString(event));
        }

        ctx.getReplayBroker().handleReplayEvent(contract, event);
    }
}
