package jo.controller;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;

import jo.handler.IDeepMktDataHandler;
import jo.handler.IHistoricalDataHandler;
import jo.handler.IOptHandler;
import jo.handler.IOrderHandler;
import jo.handler.IRealTimeBarHandler;
import jo.handler.ITopMktDataHandler;

public interface IBroker {
    int getNextOrderId();

    void cancelRealtimeBars(IRealTimeBarHandler handler);

    void reqRealTimeBars(Contract contract, WhatToShow whatToShow, boolean rthOnly, IRealTimeBarHandler handler);

    void cancelAllOrders();

    void cancelOrder(int orderId);

    void placeOrModifyOrder(Contract contract, final Order order, final IOrderHandler handler);

    void cancelDeepMktData(IDeepMktDataHandler handler);

    void reqDeepMktData(Contract contract, int numRows, IDeepMktDataHandler handler);

    void cancelOptionMktData(IOptHandler handler);

    void cancelTopMktData(ITopMktDataHandler handler);

    void reqOptionMktData(Contract contract, String genericTickList, boolean snapshot, IOptHandler handler);

    void reqTopMktData(Contract contract, String genericTickList, boolean snapshot, ITopMktDataHandler handler);

    void reqHistoricalData(Contract contract, String endDateTime, int duration, DurationUnit durationUnit, BarSize barSize, WhatToShow whatToShow, boolean rthOnly, IHistoricalDataHandler handler);
}
