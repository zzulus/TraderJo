package jo.ib.controller.handler;

import jo.ib.controller.model.Bar;

// ----------------------------------------- Historical data handling ----------------------------------------
public interface IHistoricalDataHandler {
    void historicalData(Bar bar, boolean hasGaps);

    void historicalDataEnd();
}