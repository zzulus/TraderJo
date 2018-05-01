package jo.handler;

import com.ib.client.TickAttr;

public interface ITickByTickDataHandler {
    void tickByTickAllLast(int reqId, int tickType, long time, double price, int size, TickAttr attribs, String exchange, String specialConditions);

    void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, int bidSize, int askSize, TickAttr attribs);

    void tickByTickMidPoint(int reqId, long time, double midPoint);

}
