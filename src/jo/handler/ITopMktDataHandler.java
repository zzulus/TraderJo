package jo.handler;

import com.ib.client.TickType;
import com.ib.client.Types.MktDataType;

// ---------------------------------------- Top Market Data handling ----------------------------------------
public interface ITopMktDataHandler {
    void tickPrice(TickType tickType, double price, int canAutoExecute);

    void tickSize(TickType tickType, int size);

    void tickString(TickType tickType, String value);

    void tickSnapshotEnd();

    void marketDataType(MktDataType marketDataType);
}