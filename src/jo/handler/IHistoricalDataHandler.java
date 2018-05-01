package jo.handler;

import jo.model.Bar;

// ----------------------------------------- Historical data handling ----------------------------------------
public interface IHistoricalDataHandler {
    void historicalData(Bar bar);

    void historicalDataEnd();
}