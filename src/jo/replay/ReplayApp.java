package jo.replay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import jo.app.IApp;
import jo.controller.IBroker;
import jo.handler.ITopMktDataHandler;
import jo.model.MarketData;
import jo.recording.event.AbstractEvent;
import jo.recording.event.MarketDepthEvent;
import jo.recording.event.RealTimeBarEvent;
import jo.recording.event.TickPriceEvent;
import jo.recording.event.TickSizeEvent;
import jo.recording.event.TickStringEvent;

public class ReplayApp implements IApp {
    private static final Logger log = LogManager.getLogger(ReplayApp.class);
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
        ITopMktDataHandler topMktDataHandler = marketData.getTopMktDataHandler();

        if (event instanceof RealTimeBarEvent) {
            RealTimeBarEvent typedEvent = (RealTimeBarEvent) event;
            marketData.addBar(BarSize._5_secs, typedEvent.getBar()); // TODO make event do it itself

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

        ib.handleReplayEvent(contract, event);
    }
}
