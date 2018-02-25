/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.EClientErrors;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EReaderSignal;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.ScannerSubscription;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.TickType;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DeepSide;
import com.ib.client.Types.DeepType;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.ExerciseType;
import com.ib.client.Types.FADataType;
import com.ib.client.Types.FundamentalType;
import com.ib.client.Types.MktDataType;
import com.ib.client.Types.NewsType;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.ApiConnection.ILogger;

import jo.ib.controller.handler.IAccountHandler;
import jo.ib.controller.handler.IAccountSummaryHandler;
import jo.ib.controller.handler.IAccountUpdateMultiHandler;
import jo.ib.controller.handler.IBulletinHandler;
import jo.ib.controller.handler.IConnectionHandler;
import jo.ib.controller.handler.IContractDetailsHandler;
import jo.ib.controller.handler.IDeepMktDataHandler;
import jo.ib.controller.handler.IEfpHandler;
import jo.ib.controller.handler.IFundamentalsHandler;
import jo.ib.controller.handler.IHistoricalDataHandler;
import jo.ib.controller.handler.ILiveOrderHandler;
import jo.ib.controller.handler.IMarketValueSummaryHandler;
import jo.ib.controller.handler.IOptHandler;
import jo.ib.controller.handler.IOrderHandler;
import jo.ib.controller.handler.IPositionHandler;
import jo.ib.controller.handler.IPositionMultiHandler;
import jo.ib.controller.handler.IRealTimeBarHandler;
import jo.ib.controller.handler.IScannerHandler;
import jo.ib.controller.handler.ISecDefOptParamsReqHandler;
import jo.ib.controller.handler.ISoftDollarTiersReqHandler;
import jo.ib.controller.handler.ITimeHandler;
import jo.ib.controller.handler.ITopMktDataHandler;
import jo.ib.controller.handler.ITradeReportHandler;
import jo.ib.controller.model.AccountSummaryTag;
import jo.ib.controller.model.Bar;
import jo.ib.controller.model.Group;
import jo.ib.controller.model.MarketValueTag;
import jo.ib.controller.model.Position;
import jo.ib.controller.model.Profile;
import jo.ib.controller.util.AdvisorUtil;
import jo.ib.controller.util.ConcurrentHashSet;

public class ApiController implements EWrapper {
    private interface IInternalContractDetailsHandler {
        void contractDetails(ContractDetails data);

        void contractDetailsEnd();
    }

    private ApiConnection client;
    private int reqId; // used for all requests except orders; designed not to conflict with orderId
    private int orderId;

    private final IConnectionHandler connectionHandler;
    private ITradeReportHandler tradeReportHandler;
    private IScannerHandler scannerHandler;
    private ITimeHandler timeHandler;
    private IBulletinHandler bulletinHandler;
    private final Map<Integer, IInternalContractDetailsHandler> contractDetailsMap = new HashMap<Integer, IInternalContractDetailsHandler>();
    private final Map<Integer, IOptHandler> optionCompMap = new HashMap<Integer, IOptHandler>();
    private final Map<Integer, IEfpHandler> efpMap = new HashMap<Integer, IEfpHandler>();
    private final Map<Integer, ITopMktDataHandler> topMktDataMap = new HashMap<Integer, ITopMktDataHandler>();
    private final Map<Integer, IDeepMktDataHandler> deepMktDataMap = new HashMap<Integer, IDeepMktDataHandler>();
    private final Map<Integer, IScannerHandler> scannerMap = new HashMap<Integer, IScannerHandler>();
    private final Map<Integer, IRealTimeBarHandler> realTimeBarMap = new HashMap<Integer, IRealTimeBarHandler>();
    private final Map<Integer, IHistoricalDataHandler> historicalDataMap = new HashMap<Integer, IHistoricalDataHandler>();
    private final Map<Integer, IFundamentalsHandler> fundMap = new HashMap<Integer, IFundamentalsHandler>();
    private final Map<Integer, IOrderHandler> orderHandlers = new HashMap<Integer, IOrderHandler>();
    private final Map<Integer, IAccountSummaryHandler> acctSummaryHandlers = new HashMap<Integer, IAccountSummaryHandler>();
    private final Map<Integer, IMarketValueSummaryHandler> mktValSummaryHandlers = new HashMap<Integer, IMarketValueSummaryHandler>();
    private final ConcurrentHashSet<IPositionHandler> positionHandlers = new ConcurrentHashSet<IPositionHandler>();
    private final ConcurrentHashSet<IAccountHandler> accountHandlers = new ConcurrentHashSet<IAccountHandler>();
    private final ConcurrentHashSet<ILiveOrderHandler> liveOrderHandlers = new ConcurrentHashSet<ILiveOrderHandler>();
    private final Map<Integer, IPositionMultiHandler> positionMultiMap = new HashMap<Integer, IPositionMultiHandler>();
    private final Map<Integer, IAccountUpdateMultiHandler> accountUpdateMultiMap = new HashMap<Integer, IAccountUpdateMultiHandler>();
    private final Map<Integer, ISecDefOptParamsReqHandler> secDefOptParamsReqMap = new HashMap<Integer, ISecDefOptParamsReqHandler>();
    private final Map<Integer, ISoftDollarTiersReqHandler> softDollarTiersReqMap = new HashMap<>();
    private boolean connected = false;

    public ApiConnection client() {
        return this.client;
    }

    public ApiController(IConnectionHandler handler) {
        this.connectionHandler = handler;
        this.client = new ApiConnection(this);
    }

    private void startMsgProcessingThread() {
        final EReaderSignal signal = new EJavaSignal();
        final EReader reader = new EReader(client(), signal);

        reader.start();

        new Thread() {
            @Override
            public void run() {
                while (client().isConnected()) {
                    signal.waitForSignal();
                    try {
                        reader.processMsgs();
                    } catch (IOException e) {
                        error(e);
                    }
                }
            }
        }.start();
    }

    public void connect(String host, int port, int clientId, String connectionOpts) {
        this.client.eConnect(host, port, clientId);
        startMsgProcessingThread();

    }

    public void disconnect() {
        if (!checkConnection())
            return;

        this.client.eDisconnect();
        this.connectionHandler.disconnected();
        this.connected = false;

    }

    @Override
    public void managedAccounts(String accounts) {
        ArrayList<String> list = new ArrayList<String>();
        for (StringTokenizer st = new StringTokenizer(accounts, ","); st.hasMoreTokens();) {
            list.add(st.nextToken());
        }
        this.connectionHandler.accountList(list);
    }

    @Override
    public void nextValidId(int orderId) {
        this.orderId = orderId;
        this.reqId = this.orderId + 10000000; // let order id's not collide with other request id's
        this.connected = true;
        if (this.connectionHandler != null) {
            this.connectionHandler.connected();
        }
    }

    @Override
    public void error(Exception e) {
        this.connectionHandler.error(e);
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        IOrderHandler handler = this.orderHandlers.get(id);
        if (handler != null) {
            handler.handle(errorCode, errorMsg);
        }

        for (ILiveOrderHandler liveHandler : this.liveOrderHandlers) {
            liveHandler.handle(id, errorCode, errorMsg);
        }

        // "no sec def found" response?
        if (errorCode == 200) {
            IInternalContractDetailsHandler hand = this.contractDetailsMap.remove(id);
            if (hand != null) {
                hand.contractDetailsEnd();
            }
        }

        this.connectionHandler.message(id, errorCode, errorMsg);
    }

    @Override
    public void connectionClosed() {
        this.connectionHandler.disconnected();
        this.connected = false;
    }

    public void reqAccountUpdates(boolean subscribe, String acctCode, IAccountHandler handler) {
        if (!checkConnection())
            return;

        this.accountHandlers.add(handler);
        this.client.reqAccountUpdates(subscribe, acctCode);

    }

    @Override
    public void updateAccountValue(String tag, String value, String currency, String account) {
        if (tag.equals("Currency")) { // ignore this, it is useless
            return;
        }

        for (IAccountHandler handler : this.accountHandlers) {
            handler.accountValue(account, tag, value, currency);
        }
    }

    @Override
    public void updateAccountTime(String timeStamp) {
        for (IAccountHandler handler : this.accountHandlers) {
            handler.accountTime(timeStamp);
        }
    }

    @Override
    public void accountDownloadEnd(String account) {
        for (IAccountHandler handler : this.accountHandlers) {
            handler.accountDownloadEnd(account);
        }
    }

    @Override
    public void updatePortfolio(Contract contract, double positionIn, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String account) {
        contract.exchange(contract.primaryExch());

        Position position = new Position(contract, account, positionIn, marketPrice, marketValue, averageCost, unrealizedPNL, realizedPNL);
        for (IAccountHandler handler : this.accountHandlers) {
            handler.updatePortfolio(position);
        }
    }

    /**
     * @param group
     *            pass "All" to get data for all accounts
     */
    public void reqAccountSummary(String group, AccountSummaryTag[] tags, IAccountSummaryHandler handler) {
        if (!checkConnection())
            return;

        StringBuilder sb = new StringBuilder();
        for (AccountSummaryTag tag : tags) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(tag);
        }

        int reqId = this.reqId++;
        this.acctSummaryHandlers.put(reqId, handler);
        this.client.reqAccountSummary(reqId, group, sb.toString());

    }

    private boolean isConnected() {
        return this.connected;
    }

    public void cancelAccountSummary(IAccountSummaryHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.acctSummaryHandlers, handler);
        if (reqId != null) {
            this.client.cancelAccountSummary(reqId);

        }
    }

    public void reqMarketValueSummary(String group, IMarketValueSummaryHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.mktValSummaryHandlers.put(reqId, handler);
        this.client.reqAccountSummary(reqId, group, "$LEDGER");

    }

    public void cancelMarketValueSummary(IMarketValueSummaryHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.mktValSummaryHandlers, handler);
        if (reqId != null) {
            this.client.cancelAccountSummary(reqId);
        }
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        if (tag.equals("Currency")) { // ignore this, it is useless
            return;
        }

        IAccountSummaryHandler handler = this.acctSummaryHandlers.get(reqId);
        if (handler != null) {
            handler.accountSummary(account, AccountSummaryTag.valueOf(tag), value, currency);
        }

        IMarketValueSummaryHandler handler2 = this.mktValSummaryHandlers.get(reqId);
        if (handler2 != null) {
            handler2.marketValueSummary(account, MarketValueTag.valueOf(tag), value, currency);
        }

    }

    @Override
    public void accountSummaryEnd(int reqId) {
        IAccountSummaryHandler handler = this.acctSummaryHandlers.get(reqId);
        if (handler != null) {
            handler.accountSummaryEnd();
        }

        IMarketValueSummaryHandler handler2 = this.mktValSummaryHandlers.get(reqId);
        if (handler2 != null) {
            handler2.marketValueSummaryEnd();
        }

    }

    public void reqPositions(IPositionHandler handler) {
        if (!checkConnection())
            return;

        this.positionHandlers.add(handler);
        this.client.reqPositions();

    }

    public void cancelPositions(IPositionHandler handler) {
        if (!checkConnection())
            return;

        this.positionHandlers.remove(handler);
        this.client.cancelPositions();

    }

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        for (IPositionHandler handler : this.positionHandlers) {
            handler.position(account, contract, pos, avgCost);
        }
    }

    @Override
    public void positionEnd() {
        for (IPositionHandler handler : this.positionHandlers) {
            handler.positionEnd();
        }
    }

    public void reqContractDetails(Contract contract, final IContractDetailsHandler processor) {
        if (!checkConnection())
            return;

        final ArrayList<ContractDetails> list = new ArrayList<ContractDetails>();
        internalReqContractDetails(contract, new IInternalContractDetailsHandler() {
            @Override
            public void contractDetails(ContractDetails data) {
                list.add(data);
            }

            @Override
            public void contractDetailsEnd() {
                processor.contractDetails(list);
            }
        });
    }

    private void internalReqContractDetails(Contract contract, final IInternalContractDetailsHandler processor) {
        int reqId = this.reqId++;
        this.contractDetailsMap.put(reqId, processor);
        this.orderHandlers.put(reqId, new IOrderHandler() {
            public void handle(int errorCode, String errorMsg) {
                processor.contractDetailsEnd();
            }

            @Override
            public void orderState(OrderState orderState) {
                // TODO Auto-generated method stub

            }

            @Override
            public void orderStatus(OrderStatus status, double filled,
                    double remaining, double avgFillPrice, long permId,
                    int parentId, double lastFillPrice, int clientId, String whyHeld) {
                // TODO Auto-generated method stub

            }
        });

        this.client.reqContractDetails(reqId, contract);
    }

    @Override
    public void contractDetails(int reqId, ContractDetails contractDetails) {
        IInternalContractDetailsHandler handler = this.contractDetailsMap.get(reqId);
        if (handler != null) {
            handler.contractDetails(contractDetails);
        } else {
            show("Error: no contract details handler for reqId " + reqId);
        }
    }

    @Override
    public void bondContractDetails(int reqId, ContractDetails contractDetails) {
        IInternalContractDetailsHandler handler = this.contractDetailsMap.get(reqId);
        if (handler != null) {
            handler.contractDetails(contractDetails);
        } else {
            show("Error: no bond contract details handler for reqId " + reqId);
        }
    }

    @Override
    public void contractDetailsEnd(int reqId) {
        IInternalContractDetailsHandler handler = this.contractDetailsMap.remove(reqId);
        if (handler != null) {
            handler.contractDetailsEnd();
        } else {
            show("Error: no contract details handler for reqId " + reqId);
        }
    }

    public void reqTopMktData(Contract contract, String genericTickList, boolean snapshot, ITopMktDataHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.topMktDataMap.put(reqId, handler);
        this.client.reqMktData(reqId, contract, genericTickList, snapshot, Collections.<TagValue>emptyList());

    }

    public void reqOptionMktData(Contract contract, String genericTickList, boolean snapshot, IOptHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.topMktDataMap.put(reqId, handler);
        this.optionCompMap.put(reqId, handler);
        this.client.reqMktData(reqId, contract, genericTickList, snapshot, Collections.<TagValue>emptyList());

    }

    public void reqEfpMktData(Contract contract, String genericTickList, boolean snapshot, IEfpHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.topMktDataMap.put(reqId, handler);
        this.efpMap.put(reqId, handler);
        this.client.reqMktData(reqId, contract, genericTickList, snapshot, Collections.<TagValue>emptyList());

    }

    public void cancelTopMktData(ITopMktDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.topMktDataMap, handler);
        if (reqId != null) {
            this.client.cancelMktData(reqId);
        } else {
            show("Error: could not cancel top market data");
        }

    }

    public void cancelOptionMktData(IOptHandler handler) {
        cancelTopMktData(handler);
        getAndRemoveKey(this.optionCompMap, handler);
    }

    public void cancelEfpMktData(IEfpHandler handler) {
        cancelTopMktData(handler);
        getAndRemoveKey(this.efpMap, handler);
    }

    public void reqMktDataType(MktDataType type) {
        if (!checkConnection())
            return;

        this.client.reqMarketDataType(type.ordinal());

    }

    @Override
    public void tickPrice(int reqId, int tickType, double price, int canAutoExecute) {
        ITopMktDataHandler handler = this.topMktDataMap.get(reqId);
        if (handler != null) {
            handler.tickPrice(TickType.get(tickType), price, canAutoExecute);
        }
    }

    @Override
    public void tickGeneric(int reqId, int tickType, double value) {
        ITopMktDataHandler handler = this.topMktDataMap.get(reqId);
        if (handler != null) {
            handler.tickPrice(TickType.get(tickType), value, 0);
        }
    }

    @Override
    public void tickSize(int reqId, int tickType, int size) {
        ITopMktDataHandler handler = this.topMktDataMap.get(reqId);
        if (handler != null) {
            handler.tickSize(TickType.get(tickType), size);
        }
    }

    @Override
    public void tickString(int reqId, int tickType, String value) {
        ITopMktDataHandler handler = this.topMktDataMap.get(reqId);
        if (handler != null) {
            handler.tickString(TickType.get(tickType), value);
        }
    }

    @Override
    public void tickEFP(int reqId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact,
            double dividendsToLastTradeDate) {
        IEfpHandler handler = this.efpMap.get(reqId);
        if (handler != null) {
            handler.tickEFP(tickType, basisPoints, formattedBasisPoints, impliedFuture, holdDays, futureLastTradeDate, dividendImpact, dividendsToLastTradeDate);
        }
    }

    @Override
    public void tickSnapshotEnd(int reqId) {
        ITopMktDataHandler handler = this.topMktDataMap.get(reqId);
        if (handler != null) {
            handler.tickSnapshotEnd();
        }
    }

    @Override
    public void marketDataType(int reqId, int marketDataType) {
        ITopMktDataHandler handler = this.topMktDataMap.get(reqId);
        if (handler != null) {
            handler.marketDataType(MktDataType.get(marketDataType));
        }
    }

    public void reqDeepMktData(Contract contract, int numRows, IDeepMktDataHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.deepMktDataMap.put(reqId, handler);
        ArrayList<TagValue> mktDepthOptions = new ArrayList<TagValue>();
        this.client.reqMktDepth(reqId, contract, numRows, mktDepthOptions);

    }

    public void cancelDeepMktData(IDeepMktDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.deepMktDataMap, handler);
        if (reqId != null) {
            this.client.cancelMktDepth(reqId);

        }
    }

    @Override
    public void updateMktDepth(int reqId, int position, int operation, int side, double price, int size) {
        IDeepMktDataHandler handler = this.deepMktDataMap.get(reqId);
        if (handler != null) {
            handler.updateMktDepth(position, null, DeepType.get(operation), DeepSide.get(side), price, size);
        }
    }

    @Override
    public void updateMktDepthL2(int reqId, int position, String marketMaker, int operation, int side, double price, int size) {
        IDeepMktDataHandler handler = this.deepMktDataMap.get(reqId);
        if (handler != null) {
            handler.updateMktDepth(position, marketMaker, DeepType.get(operation), DeepSide.get(side), price, size);
        }
    }

    // ---------------------------------------- Option computations ----------------------------------------
    public void reqOptionVolatility(Contract c, double optPrice, double underPrice, IOptHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.optionCompMap.put(reqId, handler);
        this.client.calculateImpliedVolatility(reqId, c, optPrice, underPrice);

    }

    public void reqOptionComputation(Contract c, double vol, double underPrice, IOptHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.optionCompMap.put(reqId, handler);
        this.client.calculateOptionPrice(reqId, c, vol, underPrice);

    }

    void cancelOptionComp(IOptHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.optionCompMap, handler);
        if (reqId != null) {
            this.client.cancelCalculateOptionPrice(reqId);

        }
    }

    @Override
    public void tickOptionComputation(int reqId, int tickType, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
        IOptHandler handler = this.optionCompMap.get(reqId);
        if (handler != null) {
            handler.tickOptionComputation(TickType.get(tickType), impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice);
        } else {
            System.out.println(String.format("not handled %s %s %s %s %s %s %s %s %s", tickType, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice));
        }
    }

    public void reqExecutions(ExecutionFilter filter, ITradeReportHandler handler) {
        if (!checkConnection())
            return;

        this.tradeReportHandler = handler;
        this.client.reqExecutions(this.reqId++, filter);

    }

    @Override
    public void execDetails(int reqId, Contract contract, Execution execution) {
        if (this.tradeReportHandler != null) {
            int i = execution.execId().lastIndexOf('.');
            String tradeKey = execution.execId().substring(0, i);
            this.tradeReportHandler.tradeReport(tradeKey, contract, execution);
        }
    }

    @Override
    public void execDetailsEnd(int reqId) {
        if (this.tradeReportHandler != null) {
            this.tradeReportHandler.tradeReportEnd();
        }
    }

    @Override
    public void commissionReport(CommissionReport commissionReport) {
        if (this.tradeReportHandler != null) {
            int i = commissionReport.m_execId.lastIndexOf('.');
            String tradeKey = commissionReport.m_execId.substring(0, i);
            this.tradeReportHandler.commissionReport(tradeKey, commissionReport);
        }
    }

    public void updateGroups(List<Group> groups) {
        if (!checkConnection())
            return;

        this.client.replaceFA(FADataType.GROUPS.ordinal(), AdvisorUtil.getGroupsXml(groups));

    }

    public void updateProfiles(List<Profile> profiles) {
        if (!checkConnection())
            return;

        this.client.replaceFA(FADataType.PROFILES.ordinal(), AdvisorUtil.getProfilesXml(profiles));

    }

    @Override
    public final void receiveFA(int faDataType, String xml) {
        // do nothing
    }

    public void placeOrModifyOrder(Contract contract, final Order order, final IOrderHandler handler) {
        if (!checkConnection())
            return;

        // when placing new order, assign new order id
        if (order.orderId() == 0) {
            order.orderId(this.orderId++);
            if (handler != null) {
                this.orderHandlers.put(order.orderId(), handler);
            }
        }

        this.client.placeOrder(contract, order);

    }

    public void cancelOrder(int orderId) {
        if (!checkConnection())
            return;

        this.client.cancelOrder(orderId);

    }

    public void cancelAllOrders() {
        if (!checkConnection())
            return;

        this.client.reqGlobalCancel();

    }

    public void exerciseOption(String account, Contract contract, ExerciseType type, int quantity, boolean override) {
        if (!checkConnection())
            return;

        this.client.exerciseOptions(this.reqId++, contract, type.ordinal(), quantity, account, override ? 1 : 0);

    }

    public void removeOrderHandler(IOrderHandler handler) {
        getAndRemoveKey(this.orderHandlers, handler);
    }

    public void reqLiveOrders(ILiveOrderHandler handler) {
        if (!checkConnection())
            return;

        this.liveOrderHandlers.add(handler);
        this.client.reqAllOpenOrders();

    }

    public void takeTwsOrders(ILiveOrderHandler handler) {
        if (!checkConnection())
            return;

        this.liveOrderHandlers.add(handler);
        this.client.reqOpenOrders();

    }

    public void takeFutureTwsOrders(ILiveOrderHandler handler) {
        if (!checkConnection())
            return;

        this.liveOrderHandlers.add(handler);
        this.client.reqAutoOpenOrders(true);

    }

    public void removeLiveOrderHandler(ILiveOrderHandler handler) {
        this.liveOrderHandlers.remove(handler);
    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        IOrderHandler handler = this.orderHandlers.get(orderId);
        if (handler != null) {
            handler.orderState(orderState);
        }

        if (!order.whatIf()) {
            for (ILiveOrderHandler liveHandler : this.liveOrderHandlers) {
                liveHandler.openOrder(contract, order, orderState);
            }
        }
    }

    @Override
    public void openOrderEnd() {
        for (ILiveOrderHandler handler : this.liveOrderHandlers) {
            handler.openOrderEnd();
        }
    }

    @Override
    public void orderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        IOrderHandler handler = this.orderHandlers.get(orderId);
        if (handler != null) {
            handler.orderStatus(OrderStatus.valueOf(status), filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
        }

        for (ILiveOrderHandler liveOrderHandler : this.liveOrderHandlers) {
            liveOrderHandler.orderStatus(orderId, OrderStatus.valueOf(status), filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
        }
    }

    public void reqScannerParameters(IScannerHandler handler) {
        if (!checkConnection())
            return;

        this.scannerHandler = handler;
        this.client.reqScannerParameters();

    }

    public void reqScannerSubscription(ScannerSubscription sub, IScannerHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.scannerMap.put(reqId, handler);
        ArrayList<TagValue> scannerSubscriptionOptions = new ArrayList<TagValue>();
        this.client.reqScannerSubscription(reqId, sub, scannerSubscriptionOptions);

    }

    public void cancelScannerSubscription(IScannerHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.scannerMap, handler);
        if (reqId != null) {
            this.client.cancelScannerSubscription(reqId);

        }
    }

    @Override
    public void scannerParameters(String xml) {
        this.scannerHandler.scannerParameters(xml);
    }

    @Override
    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
        IScannerHandler handler = this.scannerMap.get(reqId);
        if (handler != null) {
            handler.scannerData(rank, contractDetails, legsStr);
        }
    }

    @Override
    public void scannerDataEnd(int reqId) {
        IScannerHandler handler = this.scannerMap.get(reqId);
        if (handler != null) {
            handler.scannerDataEnd();
        }
    }

    /**
     * @param endDateTime
     *            format is YYYYMMDD HH:MM:SS [TMZ]
     * @param duration
     *            is number of durationUnits
     */
    public void reqHistoricalData(Contract contract, String endDateTime, int duration, DurationUnit durationUnit, BarSize barSize, WhatToShow whatToShow, boolean rthOnly, IHistoricalDataHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.historicalDataMap.put(reqId, handler);
        String durationStr = duration + " " + durationUnit.toString().charAt(0);
        this.client.reqHistoricalData(reqId, contract, endDateTime, durationStr, barSize.toString(), whatToShow.toString(), rthOnly ? 1 : 0, 2, Collections.<TagValue>emptyList());

    }

    public void cancelHistoricalData(IHistoricalDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.historicalDataMap, handler);
        if (reqId != null) {
            this.client.cancelHistoricalData(reqId);

        }
    }

    @Override
    public void historicalData(int reqId, String date, double open, double high, double low, double close, int volume, int count, double wap, boolean hasGaps) {
        IHistoricalDataHandler handler = this.historicalDataMap.get(reqId);
        if (handler != null) {
            if (date.startsWith("finished")) {
                handler.historicalDataEnd();
            } else {
                long longDate;
                if (date.length() == 8) {
                    int year = Integer.parseInt(date.substring(0, 4));
                    int month = Integer.parseInt(date.substring(4, 6));
                    int day = Integer.parseInt(date.substring(6));
                    longDate = new GregorianCalendar(year - 1900, month - 1, day).getTimeInMillis() / 1000;
                } else {
                    longDate = Long.parseLong(date);
                }
                Bar bar = new Bar(longDate, high, low, open, close, wap, volume, count);
                handler.historicalData(bar, hasGaps);
            }
        }
    }

    public void reqRealTimeBars(Contract contract, WhatToShow whatToShow, boolean rthOnly, IRealTimeBarHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.realTimeBarMap.put(reqId, handler);
        ArrayList<TagValue> realTimeBarsOptions = new ArrayList<TagValue>();
        this.client.reqRealTimeBars(reqId, contract, 0, whatToShow.toString(), rthOnly, realTimeBarsOptions);

    }

    public void cancelRealtimeBars(IRealTimeBarHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.realTimeBarMap, handler);
        if (reqId != null) {
            this.client.cancelRealTimeBars(reqId);

        }
    }

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count) {
        IRealTimeBarHandler handler = this.realTimeBarMap.get(reqId);
        if (handler != null) {
            Bar bar = new Bar(time, high, low, open, close, wap, volume, count);
            handler.realtimeBar(bar);
        }
    }

    public void reqFundamentals(Contract contract, FundamentalType reportType, IFundamentalsHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.fundMap.put(reqId, handler);
        this.client.reqFundamentalData(reqId, contract, reportType.getApiString());

    }

    @Override
    public void fundamentalData(int reqId, String data) {
        IFundamentalsHandler handler = this.fundMap.get(reqId);
        if (handler != null) {
            handler.fundamentals(data);
        }
    }

    public void reqCurrentTime(ITimeHandler handler) {
        if (!checkConnection())
            return;

        this.timeHandler = handler;
        this.client.reqCurrentTime();

    }

    protected boolean checkConnection() {
        if (!isConnected()) {
            error(EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED.code(), EClientErrors.NOT_CONNECTED.msg());
            return false;
        }

        return true;
    }

    @Override
    public void currentTime(long time) {
        this.timeHandler.currentTime(time);
    }

    public void reqBulletins(boolean allMessages, IBulletinHandler handler) {
        if (!checkConnection())
            return;

        this.bulletinHandler = handler;
        this.client.reqNewsBulletins(allMessages);

    }

    public void cancelBulletins() {
        if (!checkConnection())
            return;

        this.client.cancelNewsBulletins();
    }

    @Override
    public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
        this.bulletinHandler.bulletin(msgId, NewsType.get(msgType), message, origExchange);
    }

    public void reqPositionsMulti(String account, String modelCode, IPositionMultiHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.positionMultiMap.put(reqId, handler);
        this.client.reqPositionsMulti(reqId, account, modelCode);

    }

    public void cancelPositionsMulti(IPositionMultiHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.positionMultiMap, handler);
        if (reqId != null) {
            this.client.cancelPositionsMulti(reqId);

        }
    }

    @Override
    public void positionMulti(int reqId, String account, String modelCode, Contract contract, double pos, double avgCost) {
        IPositionMultiHandler handler = this.positionMultiMap.get(reqId);
        if (handler != null) {
            handler.positionMulti(account, modelCode, contract, pos, avgCost);
        }
    }

    @Override
    public void positionMultiEnd(int reqId) {
        IPositionMultiHandler handler = this.positionMultiMap.get(reqId);
        if (handler != null) {
            handler.positionMultiEnd();
        }
    }

    public void reqAccountUpdatesMulti(String account, String modelCode, boolean ledgerAndNLV, IAccountUpdateMultiHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.accountUpdateMultiMap.put(reqId, handler);
        this.client.reqAccountUpdatesMulti(reqId, account, modelCode, ledgerAndNLV);

    }

    public void cancelAccountUpdatesMulti(IAccountUpdateMultiHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.accountUpdateMultiMap, handler);
        if (reqId != null) {
            this.client.cancelAccountUpdatesMulti(reqId);

        }
    }

    @Override
    public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {
        IAccountUpdateMultiHandler handler = this.accountUpdateMultiMap.get(reqId);
        if (handler != null) {
            handler.accountUpdateMulti(account, modelCode, key, value, currency);
        }
    }

    @Override
    public void accountUpdateMultiEnd(int reqId) {
        IAccountUpdateMultiHandler handler = this.accountUpdateMultiMap.get(reqId);
        if (handler != null) {
            handler.accountUpdateMultiEnd();
        }
    }

    @Override
    public void verifyMessageAPI(String apiData) {
    }

    @Override
    public void verifyCompleted(boolean isSuccessful, String errorText) {
    }

    @Override
    public void verifyAndAuthMessageAPI(String apiData, String xyzChallange) {
    }

    @Override
    public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {
    }

    @Override
    public void displayGroupList(int reqId, String groups) {
    }

    @Override
    public void displayGroupUpdated(int reqId, String contractInfo) {
    }

    // ---------------------------------------- other methods ----------------------------------------
    /** Not supported in ApiController. */
    @Override
    public void deltaNeutralValidation(int reqId, DeltaNeutralContract underComp) {
        show("RECEIVED DN VALIDATION");
    }

    public void show(String string) {
        this.connectionHandler.show(string);
    }

    private static <K, V> K getAndRemoveKey(Map<K, V> map, V value) {
        for (Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue() == value) {
                map.remove(entry.getKey());
                return entry.getKey();
            }
        }
        return null;
    }

    /** Obsolete, never called. */
    @Override
    public void error(String str) {
        throw new RuntimeException(str);
    }

    @Override
    public void connectAck() {
        if (this.client.isAsyncEConnect())
            this.client.startAPI();
    }

    public void reqSecDefOptParams(String underlyingSymbol, String futFopExchange, /* String currency, */ String underlyingSecType, int underlyingConId, ISecDefOptParamsReqHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;
        this.secDefOptParamsReqMap.put(reqId, handler);
        this.client.reqSecDefOptParams(reqId, underlyingSymbol, futFopExchange, /* currency, */ underlyingSecType, underlyingConId);

    }

    @Override
    public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId, String tradingClass,
            String multiplier, Set<String> expirations, Set<Double> strikes) {
        ISecDefOptParamsReqHandler handler = this.secDefOptParamsReqMap.get(reqId);

        if (handler != null) {
            handler.securityDefinitionOptionalParameter(exchange, underlyingConId, tradingClass, multiplier, expirations, strikes);
        }
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int reqId) {
        ISecDefOptParamsReqHandler handler = this.secDefOptParamsReqMap.get(reqId);
        if (handler != null) {
            handler.securityDefinitionOptionalParameterEnd(reqId);
        }
    }

    public void reqSoftDollarTiers(ISoftDollarTiersReqHandler handler) {
        if (!checkConnection())
            return;

        int reqId = this.reqId++;

        this.softDollarTiersReqMap.put(reqId, handler);
        this.client.reqSoftDollarTiers(reqId);
    }

    @Override
    public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {
        ISoftDollarTiersReqHandler handler = this.softDollarTiersReqMap.get(reqId);

        if (handler != null) {
            handler.softDollarTiers(tiers);
        }
    }
}
