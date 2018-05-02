package jo.handler;

import java.util.List;

import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;

public interface IHistoricalTickHandler {
    void historicalTick(int reqId, List<HistoricalTick> ticks, boolean last);

    void historicalTickBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean last);

    void historicalTickLast(int reqId, List<HistoricalTickLast> ticks, boolean allReceived);
}
