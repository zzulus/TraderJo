package jo.controller;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.Contract;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.ScannerSubscription;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.ExerciseType;
import com.ib.client.Types.FundamentalType;
import com.ib.client.Types.MktDataType;
import com.ib.client.Types.WhatToShow;

import jo.controller.model.AccountSummaryTag;
import jo.controller.model.Group;
import jo.controller.model.Profile;
import jo.handler.IAccountHandler;
import jo.handler.IAccountSummaryHandler;
import jo.handler.IAccountUpdateMultiHandler;
import jo.handler.IBulletinHandler;
import jo.handler.IConnectionHandler;
import jo.handler.IContractDetailsHandler;
import jo.handler.IDeepMktDataHandler;
import jo.handler.IEfpHandler;
import jo.handler.IFundamentalsHandler;
import jo.handler.IHistoricalDataHandler;
import jo.handler.ILiveOrderHandler;
import jo.handler.IMarketValueSummaryHandler;
import jo.handler.IOptHandler;
import jo.handler.IOrderHandler;
import jo.handler.IPositionHandler;
import jo.handler.IPositionMultiHandler;
import jo.handler.IRealTimeBarHandler;
import jo.handler.IScannerHandler;
import jo.handler.ISecDefOptParamsReqHandler;
import jo.handler.ISoftDollarTiersReqHandler;
import jo.handler.ITimeHandler;
import jo.handler.ITopMktDataHandler;
import jo.handler.ITradeReportHandler;

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
    public void reqSoftDollarTiers(ISoftDollarTiersReqHandler handler) {

    }

    @Override
    public void reqSecDefOptParams(String underlyingSymbol, String futFopExchange, String underlyingSecType, int underlyingConId, ISecDefOptParamsReqHandler handler) {

    }

    @Override
    public void show(String string) {

    }

    @Override
    public void cancelAccountUpdatesMulti(IAccountUpdateMultiHandler handler) {

    }

    @Override
    public void reqAccountUpdatesMulti(String account, String modelCode, boolean ledgerAndNLV, IAccountUpdateMultiHandler handler) {

    }

    @Override
    public void cancelPositionsMulti(IPositionMultiHandler handler) {

    }

    @Override
    public void reqPositionsMulti(String account, String modelCode, IPositionMultiHandler handler) {

    }

    @Override
    public void cancelBulletins() {

    }

    @Override
    public void reqBulletins(boolean allMessages, IBulletinHandler handler) {

    }

    @Override
    public void reqCurrentTime(ITimeHandler handler) {

    }

    @Override
    public void reqFundamentals(Contract contract, FundamentalType reportType, IFundamentalsHandler handler) {

    }

    @Override
    public void cancelRealtimeBars(IRealTimeBarHandler handler) {

    }

    @Override
    public void reqRealTimeBars(Contract contract, WhatToShow whatToShow, boolean rthOnly, IRealTimeBarHandler handler) {

    }

    @Override
    public void cancelHistoricalData(IHistoricalDataHandler handler) {

    }

    @Override
    public void reqHistoricalData(Contract contract, String endDateTime, int duration, DurationUnit durationUnit, BarSize barSize, WhatToShow whatToShow, boolean rthOnly, IHistoricalDataHandler handler) {

    }

    @Override
    public void cancelScannerSubscription(IScannerHandler handler) {

    }

    @Override
    public void reqScannerSubscription(ScannerSubscription sub, IScannerHandler handler) {

    }

    @Override
    public void reqScannerParameters(IScannerHandler handler) {

    }

    @Override
    public void removeLiveOrderHandler(ILiveOrderHandler handler) {

    }

    @Override
    public void takeFutureTwsOrders(ILiveOrderHandler handler) {

    }

    @Override
    public void takeTwsOrders(ILiveOrderHandler handler) {

    }

    @Override
    public void reqLiveOrders(ILiveOrderHandler handler) {

    }

    @Override
    public void removeOrderHandler(IOrderHandler handler) {

    }

    @Override
    public void exerciseOption(String account, Contract contract, ExerciseType type, int quantity, boolean override) {

    }

    @Override
    public void updateProfiles(List<Profile> profiles) {

    }

    @Override
    public void updateGroups(List<Group> groups) {

    }

    @Override
    public void reqExecutions(ExecutionFilter filter, ITradeReportHandler handler) {

    }

    @Override
    public void cancelOptionComp(IOptHandler handler) {

    }

    @Override
    public void reqOptionComputation(Contract c, double vol, double underPrice, IOptHandler handler) {

    }

    @Override
    public void reqOptionVolatility(Contract c, double optPrice, double underPrice, IOptHandler handler) {

    }

    @Override
    public void cancelDeepMktData(IDeepMktDataHandler handler) {

    }

    @Override
    public void reqDeepMktData(Contract contract, int numRows, IDeepMktDataHandler handler) {

    }

    @Override
    public void reqMktDataType(MktDataType type) {

    }

    @Override
    public void cancelEfpMktData(IEfpHandler handler) {

    }

    @Override
    public void cancelOptionMktData(IOptHandler handler) {

    }

    @Override
    public void cancelTopMktData(ITopMktDataHandler handler) {

    }

    @Override
    public void reqEfpMktData(Contract contract, String genericTickList, boolean snapshot, IEfpHandler handler) {

    }

    @Override
    public void reqOptionMktData(Contract contract, String genericTickList, boolean snapshot, IOptHandler handler) {

    }

    @Override
    public void reqTopMktData(Contract contract, String genericTickList, boolean snapshot, ITopMktDataHandler handler) {

    }

    @Override
    public void reqContractDetails(Contract contract, IContractDetailsHandler processor) {

    }

    @Override
    public void cancelPositions(IPositionHandler handler) {

    }

    @Override
    public void reqPositions(IPositionHandler handler) {

    }

    @Override
    public void cancelMarketValueSummary(IMarketValueSummaryHandler handler) {

    }

    @Override
    public void reqMarketValueSummary(String group, IMarketValueSummaryHandler handler) {

    }

    @Override
    public void cancelAccountSummary(IAccountSummaryHandler handler) {

    }

    @Override
    public void reqAccountSummary(String group, AccountSummaryTag[] tags, IAccountSummaryHandler handler) {

    }

    @Override
    public void reqAccountUpdates(boolean subscribe, String acctCode, IAccountHandler handler) {

    }

    @Override
    public void setClientId(int clientId) {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void connect(IConnectionHandler handler, String host, int port, int clientId) {

    }

    @Override
    public void connectLocalhostLive(IConnectionHandler handler) {

    }

}
