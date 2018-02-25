package jo.ib.controller.handler;

public interface IEfpHandler extends ITopMktDataHandler {
    void tickEFP(int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact, double dividendsToLastTradeDate);
}