package jo.handler;

public interface IPnLHandler {
    void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL);
}