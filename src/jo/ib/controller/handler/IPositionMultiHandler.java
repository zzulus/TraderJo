package jo.ib.controller.handler;

import com.ib.client.Contract;

// ---------------------------------------- Position Multi handling ----------------------------------------
public interface IPositionMultiHandler {
    void positionMulti(String account, String modelCode, Contract contract, double pos, double avgCost);

    void positionMultiEnd();
}