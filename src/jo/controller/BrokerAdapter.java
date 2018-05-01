package jo.controller;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class BrokerAdapter implements IBroker {
    protected static final Logger log = LogManager.getLogger(BrokerAdapter.class);
    protected AtomicInteger reqId = new AtomicInteger();
    protected AtomicInteger orderId = new AtomicInteger();

    protected int getNextRequestId() {
        return reqId.incrementAndGet();
    }

    // ORDERS

    @Override
    public int getNextOrderId() {
        return orderId.incrementAndGet();
    }

    @Override
    public void placeOrModifyOrder(Contract contract, Order order, IOrderHandler handler) {
        log.info("placeOrModifyOrder: {}, {}", contract.symbol(), order.action());
    }

    @Override
    public void cancelAllOrders() {

    }

    @Override
    public void cancelOrder(int orderId) {

    }

    //

    @Override
    public void cancelRealtimeBars(IRealTimeBarHandler handler) {

    }

    @Override
    public void reqRealTimeBars(Contract contract, WhatToShow whatToShow, boolean rthOnly, IRealTimeBarHandler handler) {

    }

    @Override
    public void cancelDeepMktData(IDeepMktDataHandler handler) {

    }

    @Override
    public void reqDeepMktData(Contract contract, int numRows, IDeepMktDataHandler handler) {

    }

    @Override
    public void cancelOptionMktData(IOptHandler handler) {

    }

    @Override
    public void cancelTopMktData(ITopMktDataHandler handler) {

    }

    @Override
    public void reqOptionMktData(Contract contract, String genericTickList, boolean snapshot, IOptHandler handler) {

    }

    @Override
    public void reqTopMktData(Contract contract, String genericTickList, boolean snapshot, ITopMktDataHandler handler) {

    }

    @Override
    public void reqHistoricalData(Contract contract, String endDateTime, int duration, DurationUnit durationUnit, BarSize barSize, WhatToShow whatToShow, boolean rthOnly, IHistoricalDataHandler handler) {
    }

}
