package jo.handler;

import jo.controller.model.MarketValueTag;

public interface IMarketValueSummaryHandler {
    void marketValueSummary(String account, MarketValueTag tag, String value, String currency);

    void marketValueSummaryEnd();
}