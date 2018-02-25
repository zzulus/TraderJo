package jo.ib.controller.handler;

import com.ib.client.TickType;
import com.ib.client.Types.MktDataType;

public class TopMktDataAdapter implements ITopMktDataHandler {
    @Override
    public void tickPrice(TickType tickType, double price, int canAutoExecute) {
    }

    @Override
    public void tickSize(TickType tickType, int size) {
    }

    @Override
    public void tickString(TickType tickType, String value) {
    }

    @Override
    public void tickSnapshotEnd() {
    }

    @Override
    public void marketDataType(MktDataType marketDataType) {
    }
}