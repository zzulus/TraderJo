package jo.ib.controller.handler;

import com.ib.client.TickType;

public interface IOptHandler extends ITopMktDataHandler {
    void tickOptionComputation(TickType tickType, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice);
}