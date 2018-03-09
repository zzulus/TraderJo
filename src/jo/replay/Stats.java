package jo.replay;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;

public class Stats {
    private Map<Integer, ReplayOrder> openOrders;
    private List<ReplayOrder> filledOrders;
    private Map<String, MutableInt> positions;
    private double pnl;
    private double potentialPnl;
    private double commissions;

    public Stats(Map<Integer, ReplayOrder> openOrders, List<ReplayOrder> filledOrders, Map<String, MutableInt> positions, double pnl, double potentialPnl, double commissions) {
        this.openOrders = openOrders;
        this.filledOrders = filledOrders;
        this.positions = positions;
        this.pnl = pnl;
        this.potentialPnl = potentialPnl;
        this.commissions = commissions;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Filled orders: " + filledOrders.size());
        b.append("\nOpen orders: " + openOrders.size());
        b.append("\nP&L: " + String.format("%.2f", pnl));
        b.append("\nCommisions: " + String.format("%.2f", commissions));
        b.append("\nProfit: " + String.format("%.2f", pnl - commissions));
        b.append("\nPotential Prtofit: " + String.format("%.2f", potentialPnl - commissions));

        b.append("\nPositions: ");
        for (String symbol : positions.keySet()) {
            b.append(String.format("\n %s: %s", symbol, positions.get(symbol)));
        }
        return b.toString();
    }

    public Map<Integer, ReplayOrder> getOpenOrders() {
        return openOrders;
    }

    public void setOpenOrders(Map<Integer, ReplayOrder> openOrders) {
        this.openOrders = openOrders;
    }

    public List<ReplayOrder> getFilledOrders() {
        return filledOrders;
    }

    public void setFilledOrders(List<ReplayOrder> filledOrders) {
        this.filledOrders = filledOrders;
    }

    public Map<String, MutableInt> getPositions() {
        return positions;
    }

    public void setPositions(Map<String, MutableInt> positions) {
        this.positions = positions;
    }

    public double getPnl() {
        return pnl;
    }

    public void setPnl(double pnl) {
        this.pnl = pnl;
    }

    public double getPotentialPnl() {
        return potentialPnl;
    }

    public void setPotentialPnl(double potentialPnl) {
        this.potentialPnl = potentialPnl;
    }

    public double getCommissions() {
        return commissions;
    }

    public void setCommissions(double commissions) {
        this.commissions = commissions;
    }

}
