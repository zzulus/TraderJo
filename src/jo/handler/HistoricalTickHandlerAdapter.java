package jo.handler;

import java.util.List;

import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;

public class HistoricalTickHandlerAdapter implements IHistoricalTickHandler {

    @Override
    public void historicalTick(int reqId, List<HistoricalTick> ticks, boolean last) {

    }

    @Override
    public void historicalTickBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean last) {

    }

    @Override
    public void historicalTickLast(int reqId, List<HistoricalTickLast> ticks, boolean allReceived) {

    }
}
