package jo.handler;

import com.ib.client.Types.DeepSide;
import com.ib.client.Types.DeepType;

// ---------------------------------------- Deep Market Data handling ----------------------------------------
public interface IDeepMktDataHandler {
    void updateMktDepth(int position, String marketMaker, DeepType operation, DeepSide side, double price, int size);
}