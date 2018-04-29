package jo.replay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ib.client.Contract;

import jo.controller.IBroker;
import jo.model.IApp;
import jo.model.MarketData;
import jo.recording.event.AbstractEvent;
import jo.recording.event.ErrorEvent;
import jo.recording.event.MarketDepthEvent;
import jo.recording.event.RealTimeBarEvent;
import jo.recording.event.TickPriceEvent;
import jo.recording.event.TickSizeEvent;
import jo.recording.event.TickStringEvent;

public class ReplayApp implements IApp {

    private Map<String, MarketData> marketDataMap = new ConcurrentHashMap<>();
    private ReplayBroker ib;

    public ReplayApp(ReplayBroker ib) {
        this.ib = ib;
        ib.setApp(this);
    }

    public MarketData initMarketData(String symbol) {
        return marketDataMap.computeIfAbsent(symbol, (k) -> new MarketData());
    }

    @Override
    public MarketData getMarketData(String symbol) {
        return marketDataMap.get(symbol);
    }

    @Override
    public Map<String, MarketData> getMarketDataMap() {
        return marketDataMap;
    }

    @Override
    public IBroker getIb() {
        return ib;
    }

    public void handleReplayEvent(Contract contract, AbstractEvent event) {
        // System.out.println(ToStringBuilder.reflectionToString(event));

        MarketData marketData = marketDataMap.get(contract.symbol());

        if (event instanceof RealTimeBarEvent) {
            RealTimeBarEvent typedEvent = (RealTimeBarEvent) event;
            marketData.realtimeBar(typedEvent.getBar()); // TODO make event do it itself

        } else if (event instanceof TickPriceEvent) {
            TickPriceEvent typedEvent = (TickPriceEvent) event;
            marketData.tickPrice(typedEvent.getTickType(), typedEvent.getPrice(), typedEvent.getCanAutoExecute());

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

        ib.handleReplayEvent(contract, event);
    }

    @Override
    public void initMarketData(Contract contract) {
        // TODO Auto-generated method stub       
    }
}
