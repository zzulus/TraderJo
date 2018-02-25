package jo.model;

public class Bot {
    private Strategy openPositionSrategy;
    private Strategy profitTakerStrategy;
    private Strategy stopLossStrategy;

    private ContractTradingDetails tradedContract;
    private Position position;
}
