package jo.ib.controller.handler;

import com.ib.client.Contract;

// ---------------------------------------- Position handling ----------------------------------------
public interface IPositionHandler {
    void position(String account, Contract contract, double pos, double avgCost);

    void positionEnd();
}