package jo.controller;

import java.util.List;

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

public interface IBroker {

    int getNextOrderId();

    void reqSoftDollarTiers(ISoftDollarTiersReqHandler handler);

    void reqSecDefOptParams(String underlyingSymbol, String futFopExchange, String underlyingSecType, int underlyingConId, ISecDefOptParamsReqHandler handler);

    void show(String string);

    void cancelAccountUpdatesMulti(IAccountUpdateMultiHandler handler);

    void reqAccountUpdatesMulti(String account, String modelCode, boolean ledgerAndNLV, IAccountUpdateMultiHandler handler);

    void cancelPositionsMulti(IPositionMultiHandler handler);

    void reqPositionsMulti(String account, String modelCode, IPositionMultiHandler handler);

    void cancelBulletins();

    void reqBulletins(boolean allMessages, IBulletinHandler handler);

    void reqCurrentTime(ITimeHandler handler);

    void reqFundamentals(Contract contract, FundamentalType reportType, IFundamentalsHandler handler);

    void cancelRealtimeBars(IRealTimeBarHandler handler);

    void reqRealTimeBars(Contract contract, WhatToShow whatToShow, boolean rthOnly, IRealTimeBarHandler handler);

    void cancelHistoricalData(IHistoricalDataHandler handler);

    void reqHistoricalData(Contract contract, String endDateTime, int duration, DurationUnit durationUnit, BarSize barSize, WhatToShow whatToShow, boolean rthOnly, IHistoricalDataHandler handler);

    void cancelScannerSubscription(IScannerHandler handler);

    void reqScannerSubscription(ScannerSubscription sub, IScannerHandler handler);

    void reqScannerParameters(IScannerHandler handler);

    void removeLiveOrderHandler(ILiveOrderHandler handler);

    void takeFutureTwsOrders(ILiveOrderHandler handler);

    void takeTwsOrders(ILiveOrderHandler handler);

    void reqLiveOrders(ILiveOrderHandler handler);

    void removeOrderHandler(IOrderHandler handler);

    void exerciseOption(String account, Contract contract, ExerciseType type, int quantity, boolean override);

    void cancelAllOrders();

    void cancelOrder(int orderId);

    void placeOrModifyOrder(Contract contract, final Order order, final IOrderHandler handler);

    void updateProfiles(List<Profile> profiles);

    void updateGroups(List<Group> groups);

    void reqExecutions(ExecutionFilter filter, ITradeReportHandler handler);

    void cancelOptionComp(IOptHandler handler);

    void reqOptionComputation(Contract c, double vol, double underPrice, IOptHandler handler);

    void reqOptionVolatility(Contract c, double optPrice, double underPrice, IOptHandler handler);

    void cancelDeepMktData(IDeepMktDataHandler handler);

    void reqDeepMktData(Contract contract, int numRows, IDeepMktDataHandler handler);

    void reqMktDataType(MktDataType type);

    void cancelEfpMktData(IEfpHandler handler);

    void cancelOptionMktData(IOptHandler handler);

    void cancelTopMktData(ITopMktDataHandler handler);

    void reqEfpMktData(Contract contract, String genericTickList, boolean snapshot, IEfpHandler handler);

    void reqOptionMktData(Contract contract, String genericTickList, boolean snapshot, IOptHandler handler);

    void reqTopMktData(Contract contract, String genericTickList, boolean snapshot, ITopMktDataHandler handler);

    void reqContractDetails(Contract contract, final IContractDetailsHandler processor);

    void cancelPositions(IPositionHandler handler);

    void reqPositions(IPositionHandler handler);

    void cancelMarketValueSummary(IMarketValueSummaryHandler handler);

    void reqMarketValueSummary(String group, IMarketValueSummaryHandler handler);

    void cancelAccountSummary(IAccountSummaryHandler handler);

    void reqAccountSummary(String group, AccountSummaryTag[] tags, IAccountSummaryHandler handler);

    void reqAccountUpdates(boolean subscribe, String acctCode, IAccountHandler handler);

    void setClientId(int clientId);

    void disconnect();

    void connect(IConnectionHandler handler, String host, int port, int clientId);

    void connectLocalhostLive(IConnectionHandler handler);

}
