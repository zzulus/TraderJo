package jo.handler;

public interface IPnLSingleHandler {
    void pnlSingle(int reqId, int pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value);
}
