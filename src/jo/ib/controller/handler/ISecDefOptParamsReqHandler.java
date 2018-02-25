package jo.ib.controller.handler;

import java.util.Set;

public interface ISecDefOptParamsReqHandler {
    void securityDefinitionOptionalParameter(String exchange, int underlyingConId, String tradingClass,
            String multiplier, Set<String> expirations, Set<Double> strikes);

    void securityDefinitionOptionalParameterEnd(int reqId);
}