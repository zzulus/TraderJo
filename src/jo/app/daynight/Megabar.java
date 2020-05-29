package jo.app.daynight;

import jo.model.Bar;

import java.util.Map;

public class Megabar {
    long time;
    double tradeOpenR;
    double tradeOpenE;
    double tradeCloseR;
    double tradeCloseE;

    double bidOpenR;
    double bidOpenE;
    double bidCloseR;
    double bidCloseE;

    double askOpenR;
    double askOpenE;
    double askCloseR;
    double askCloseE;

    double midOpenR;
    double midOpenE;
    double midCloseR;
    double midCloseE;

    public Megabar() {
    }

    public Megabar(
            long time,
            Map<Long, Bar> tradesR,
            Map<Long, Bar> tradesE,
            Map<Long, Bar> bidR,
            Map<Long, Bar> bidE,
            Map<Long, Bar> askR,
            Map<Long, Bar> askE,
            Map<Long, Bar> midR,
            Map<Long, Bar> midE) {
        this.time = time;
        this.tradeOpenR = tradesR.get(time).getOpen();
        this.tradeCloseR = tradesR.get(time).getClose();
        this.tradeOpenE = tradesE.get(time).getOpen();
        this.tradeCloseE = tradesE.get(time).getClose();
        this.bidOpenR = bidR.get(time).getOpen();
        this.bidCloseR = bidR.get(time).getClose();
        this.bidOpenE = bidE.get(time).getOpen();
        this.bidCloseE = bidE.get(time).getClose();
        this.askOpenR = askR.get(time).getOpen();
        this.askCloseR = askR.get(time).getClose();
        this.askOpenE = askE.get(time).getOpen();
        this.askCloseE = askE.get(time).getClose();
        this.midOpenR = midR.get(time).getOpen();
        this.midCloseR = midR.get(time).getClose();
        this.midOpenE = midE.get(time).getOpen();
        this.midCloseE = midE.get(time).getClose();
    }
}