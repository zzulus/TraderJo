package jo.position;

public interface PositionSizeStrategy {
    int getPositionSize(double openPrice, double dollarRiskPerShare);
}
