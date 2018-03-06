/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.EClientErrors;
import com.ib.client.EClientSocket;
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

import jo.constant.ConnectionConstants;
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
import jo.ib.controller.model.AccountSummaryTag;
import jo.ib.controller.model.Group;
import jo.ib.controller.model.MarketValueTag;
import jo.ib.controller.model.Position;
import jo.ib.controller.model.Profile;
import jo.model.Bar;
import jo.util.AdvisorUtil;
import jo.util.ConcurrentHashSet;

public class IBService {
    private static final Logger log = LogManager.getLogger(IBService.class);

    // TODO WTF is that?
    private interface IInternalContractDetailsHandler {
        void contractDetails(ContractDetails data);

        void contractDetailsEnd();
    }

    private int clientId = (int) (System.currentTimeMillis() / 1000);
    private EClientSocket client;
    private EReaderSignal signal;
    private EReader reader;
    private final IBAdapter ibAdapter = new IBAdapter();

    private AtomicInteger reqId = new AtomicInteger();
    private AtomicInteger orderId = new AtomicInteger();

    private IConnectionHandler connectionHandler;
    private ITradeReportHandler tradeReportHandler;
    private IScannerHandler scannerHandler;
    private ITimeHandler timeHandler;
    private IBulletinHandler bulletinHandler;
    private final Map<Integer, IInternalContractDetailsHandler> contractDetailsMap = new ConcurrentHashMap<>();
    private final Map<Integer, IOptHandler> optionCompMap = new ConcurrentHashMap<>();
    private final Map<Integer, IEfpHandler> efpMap = new ConcurrentHashMap<>();
    private final Map<Integer, ITopMktDataHandler> topMktDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, IDeepMktDataHandler> deepMktDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, IScannerHandler> scannerMap = new ConcurrentHashMap<>();
    private final Map<Integer, IRealTimeBarHandler> realTimeBarMap = new ConcurrentHashMap<>();
    private final Map<Integer, IHistoricalDataHandler> historicalDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, IFundamentalsHandler> fundamentalMap = new ConcurrentHashMap<>();
    private final Map<Integer, IOrderHandler> orderHandlers = new ConcurrentHashMap<>();
    private final Map<Integer, IAccountSummaryHandler> acctSummaryHandlers = new ConcurrentHashMap<>();
    private final Map<Integer, IMarketValueSummaryHandler> mktValSummaryHandlers = new ConcurrentHashMap<>();
    private final Map<Integer, IPositionMultiHandler> positionMultiMap = new ConcurrentHashMap<>();
    private final Map<Integer, IAccountUpdateMultiHandler> accountUpdateMultiMap = new ConcurrentHashMap<>();
    private final Map<Integer, ISecDefOptParamsReqHandler> secDefOptParamsReqMap = new ConcurrentHashMap<>();
    private final Map<Integer, ISoftDollarTiersReqHandler> softDollarTiersReqMap = new ConcurrentHashMap<>();
    private final Set<IPositionHandler> positionHandlers = new ConcurrentHashSet<>();
    private final Set<IAccountHandler> accountHandlers = new ConcurrentHashSet<>();
    private final Set<ILiveOrderHandler> liveOrderHandlers = new ConcurrentHashSet<>();

    private volatile boolean isConnected = false;

    public IBService() {
    }

    private void startMsgProcessingThread() {
        reader.start();

        new Thread("IBService") {
            @Override
            public void run() {
                while (getClient().isConnected()) {
                    signal.waitForSignal();
                    try {
                        reader.processMsgs();
                    } catch (IOException e) {
                        ibAdapter.error(e);
                    }
                }
            }
        }.start();
    }

    public void connectLocalhostLive(IConnectionHandler handler) {
        connect(handler, ConnectionConstants.LOCALHOST, ConnectionConstants.LIVE_TRADING_PORT, clientId);
    }

    public void connect(IConnectionHandler handler, String host, int port, int clientId) {
        connectionHandler = handler;

        signal = new EJavaSignal();
        client = new EClientSocket(ibAdapter, signal);
        client.eConnect(host, port, clientId);
        reader = new EReader(client, signal);
        startMsgProcessingThread();
    }

    public void disconnect() {
        if (!checkConnection())
            return;

        isConnected = false;
        client.eDisconnect();
        connectionHandler.disconnected();
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public void reqAccountUpdates(boolean subscribe, String acctCode, IAccountHandler handler) {
        if (!checkConnection())
            return;

        accountHandlers.add(handler);
        client.reqAccountUpdates(subscribe, acctCode);
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

        int reqId = getNextRequestId();
        acctSummaryHandlers.put(reqId, handler);
        client.reqAccountSummary(reqId, group, sb.toString());
    }

    private boolean isConnected() {
        return isConnected;
    }

    public void cancelAccountSummary(IAccountSummaryHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(acctSummaryHandlers, handler);
        if (reqId != null) {
            client.cancelAccountSummary(reqId);
        }
    }

    public void reqMarketValueSummary(String group, IMarketValueSummaryHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        mktValSummaryHandlers.put(reqId, handler);
        client.reqAccountSummary(reqId, group, "$LEDGER");
    }

    public void cancelMarketValueSummary(IMarketValueSummaryHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(mktValSummaryHandlers, handler);
        if (reqId != null) {
            client.cancelAccountSummary(reqId);
        }
    }

    public void reqPositions(IPositionHandler handler) {
        if (!checkConnection())
            return;

        positionHandlers.add(handler);
        client.reqPositions();
    }

    public void cancelPositions(IPositionHandler handler) {
        if (!checkConnection())
            return;

        positionHandlers.remove(handler);
        client.cancelPositions();
    }

    public void reqContractDetails(Contract contract, final IContractDetailsHandler processor) {
        if (!checkConnection())
            return;

        final ArrayList<ContractDetails> list = new ArrayList<>();
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
        int reqId = getNextRequestId();
        contractDetailsMap.put(reqId, processor);

        orderHandlers.put(reqId, new IOrderHandler() {

            public void handle(int errorCode, String errorMsg) {
                processor.contractDetailsEnd();
            }

            @Override
            public void orderState(OrderState orderState) {
                // TODO Auto-generated method stub

            }

            @Override
            public void orderStatus(OrderStatus status,
                    double filled,
                    double remaining,
                    double avgFillPrice,
                    long permId,
                    int parentId,
                    double lastFillPrice,
                    int clientId,
                    String whyHeld) {
                // TODO Auto-generated method stub
            }
        });

        client.reqContractDetails(reqId, contract);
    }

    public void reqTopMktData(Contract contract, String genericTickList, boolean snapshot, ITopMktDataHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        topMktDataMap.put(reqId, handler);
        client.reqMktData(reqId, contract, genericTickList, snapshot, null);
    }

    public void reqOptionMktData(Contract contract, String genericTickList, boolean snapshot, IOptHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        topMktDataMap.put(reqId, handler);
        optionCompMap.put(reqId, handler);
        client.reqMktData(reqId, contract, genericTickList, snapshot, Collections.<TagValue>emptyList());
    }

    public void reqEfpMktData(Contract contract, String genericTickList, boolean snapshot, IEfpHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        topMktDataMap.put(reqId, handler);
        efpMap.put(reqId, handler);
        client.reqMktData(reqId, contract, genericTickList, snapshot, Collections.<TagValue>emptyList());
    }

    public void cancelTopMktData(ITopMktDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.topMktDataMap, handler);
        if (reqId != null) {
            client.cancelMktData(reqId);
        } else {
            show("Error: could not cancel top market data subscription");
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

        client.reqMarketDataType(type.ordinal());
    }

    public void reqDeepMktData(Contract contract, int numRows, IDeepMktDataHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        deepMktDataMap.put(reqId, handler);
        ArrayList<TagValue> mktDepthOptions = new ArrayList<>();
        client.reqMktDepth(reqId, contract, numRows, mktDepthOptions);
    }

    public void cancelDeepMktData(IDeepMktDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(deepMktDataMap, handler);
        if (reqId != null) {
            client.cancelMktDepth(reqId);
        }
    }

    // ---------------------------------------- Option computations ----------------------------------------
    public void reqOptionVolatility(Contract c, double optPrice, double underPrice, IOptHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        optionCompMap.put(reqId, handler);
        client.calculateImpliedVolatility(reqId, c, optPrice, underPrice);
    }

    public void reqOptionComputation(Contract c, double vol, double underPrice, IOptHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        optionCompMap.put(reqId, handler);
        client.calculateOptionPrice(reqId, c, vol, underPrice);
    }

    void cancelOptionComp(IOptHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(optionCompMap, handler);
        if (reqId != null) {
            client.cancelCalculateOptionPrice(reqId);
        }
    }

    public void reqExecutions(ExecutionFilter filter, ITradeReportHandler handler) {
        if (!checkConnection())
            return;

        tradeReportHandler = handler;
        client.reqExecutions(getNextRequestId(), filter);
    }

    public void updateGroups(List<Group> groups) {
        if (!checkConnection())
            return;

        client.replaceFA(FADataType.GROUPS.ordinal(), AdvisorUtil.getGroupsXml(groups));
    }

    public void updateProfiles(List<Profile> profiles) {
        if (!checkConnection())
            return;

        client.replaceFA(FADataType.PROFILES.ordinal(), AdvisorUtil.getProfilesXml(profiles));
    }

    public void placeOrModifyOrder(Contract contract, final Order order, final IOrderHandler handler) {
        if (!checkConnection())
            return;

        // when placing new order, assign new order id
        if (order.orderId() == 0) {
            order.orderId(getNextOrderId());
        }

        if (handler != null) {
            orderHandlers.put(order.orderId(), handler);
        }

        client.placeOrder(order.orderId(), contract, order);
    }

    public void cancelOrder(int orderId) {
        if (!checkConnection())
            return;

        client.cancelOrder(orderId);
    }

    public void cancelAllOrders() {
        if (!checkConnection())
            return;

        client.reqGlobalCancel();
    }

    public void exerciseOption(String account, Contract contract, ExerciseType type, int quantity, boolean override) {
        if (!checkConnection())
            return;

        client.exerciseOptions(getNextRequestId(), contract, type.ordinal(), quantity, account, override ? 1 : 0);
    }

    public void removeOrderHandler(IOrderHandler handler) {
        getAndRemoveKey(this.orderHandlers, handler);
    }

    public void reqLiveOrders(ILiveOrderHandler handler) {
        if (!checkConnection())
            return;

        liveOrderHandlers.add(handler);
        client.reqAllOpenOrders();
    }

    public void takeTwsOrders(ILiveOrderHandler handler) {
        if (!checkConnection())
            return;

        liveOrderHandlers.add(handler);
        client.reqOpenOrders();
    }

    public void takeFutureTwsOrders(ILiveOrderHandler handler) {
        if (!checkConnection())
            return;

        liveOrderHandlers.add(handler);
        client.reqAutoOpenOrders(true);
    }

    public void removeLiveOrderHandler(ILiveOrderHandler handler) {
        liveOrderHandlers.remove(handler);
    }

    public void reqScannerParameters(IScannerHandler handler) {
        if (!checkConnection())
            return;

        scannerHandler = handler;
        client.reqScannerParameters();
    }

    public void reqScannerSubscription(ScannerSubscription sub, IScannerHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        scannerMap.put(reqId, handler);
        ArrayList<TagValue> scannerSubscriptionOptions = new ArrayList<>();
        client.reqScannerSubscription(reqId, sub, scannerSubscriptionOptions);
    }

    public void cancelScannerSubscription(IScannerHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.scannerMap, handler);
        if (reqId != null) {
            client.cancelScannerSubscription(reqId);
        }
    }

    /**
     * @param endDateTime
     *            format is YYYYMMDD HH:MM:SS [TMZ]
     * @param duration
     *            is number of durationUnits
     */
    public void reqHistoricalData(Contract contract,
            String endDateTime,
            int duration,
            DurationUnit durationUnit,
            BarSize barSize,
            WhatToShow whatToShow,
            boolean rthOnly,
            IHistoricalDataHandler handler) {

        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        historicalDataMap.put(reqId, handler);
        String durationStr = duration + " " + durationUnit.toString().charAt(0);
        client.reqHistoricalData(reqId, contract, endDateTime, durationStr, barSize.toString(), whatToShow.toString(), rthOnly ? 1 : 0, 2, Collections.emptyList());
    }

    public void cancelHistoricalData(IHistoricalDataHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(historicalDataMap, handler);
        if (reqId != null) {
            client.cancelHistoricalData(reqId);
        }
    }

    public void reqRealTimeBars(Contract contract, WhatToShow whatToShow, boolean rthOnly, IRealTimeBarHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        realTimeBarMap.put(reqId, handler);
        ArrayList<TagValue> realTimeBarsOptions = new ArrayList<>();
        client.reqRealTimeBars(reqId, contract, 0, whatToShow.toString(), rthOnly, realTimeBarsOptions);
    }

    public void cancelRealtimeBars(IRealTimeBarHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(realTimeBarMap, handler);
        if (reqId != null) {
            client.cancelRealTimeBars(reqId);
        }
    }

    public void reqFundamentals(Contract contract, FundamentalType reportType, IFundamentalsHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        fundamentalMap.put(reqId, handler);
        client.reqFundamentalData(reqId, contract, reportType.getApiString());
    }

    public void reqCurrentTime(ITimeHandler handler) {
        if (!checkConnection())
            return;

        timeHandler = handler;
        client.reqCurrentTime();
    }

    public void reqBulletins(boolean allMessages, IBulletinHandler handler) {
        if (!checkConnection())
            return;

        bulletinHandler = handler;
        client.reqNewsBulletins(allMessages);
    }

    public void cancelBulletins() {
        if (!checkConnection())
            return;

        client.cancelNewsBulletins();
    }

    public void reqPositionsMulti(String account, String modelCode, IPositionMultiHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        positionMultiMap.put(reqId, handler);
        client.reqPositionsMulti(reqId, account, modelCode);
    }

    public void cancelPositionsMulti(IPositionMultiHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.positionMultiMap, handler);
        if (reqId != null) {
            client.cancelPositionsMulti(reqId);
        }
    }

    public void reqAccountUpdatesMulti(String account, String modelCode, boolean ledgerAndNLV, IAccountUpdateMultiHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        accountUpdateMultiMap.put(reqId, handler);
        client.reqAccountUpdatesMulti(reqId, account, modelCode, ledgerAndNLV);
    }

    public void cancelAccountUpdatesMulti(IAccountUpdateMultiHandler handler) {
        if (!checkConnection())
            return;

        Integer reqId = getAndRemoveKey(this.accountUpdateMultiMap, handler);
        if (reqId != null) {
            this.client.cancelAccountUpdatesMulti(reqId);
        }
    }

    public void show(String string) {
        this.connectionHandler.show(string);
    }

    public void reqSecDefOptParams(String underlyingSymbol, String futFopExchange, /* String currency, */ String underlyingSecType, int underlyingConId, ISecDefOptParamsReqHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();
        secDefOptParamsReqMap.put(reqId, handler);
        client.reqSecDefOptParams(reqId, underlyingSymbol, futFopExchange, /* currency, */ underlyingSecType, underlyingConId);
    }

    public void reqSoftDollarTiers(ISoftDollarTiersReqHandler handler) {
        if (!checkConnection())
            return;

        int reqId = getNextRequestId();

        this.softDollarTiersReqMap.put(reqId, handler);
        this.client.reqSoftDollarTiers(reqId);
    }

    public EClientSocket getClient() {
        return client;
    }

    // ==========================================================================
    // Private stuff
    // ==========================================================================
    private int getNextRequestId() {
        return reqId.incrementAndGet();
    }

    public int getNextOrderId() {
        return orderId.incrementAndGet();
    }

    protected boolean checkConnection() {
        // TODO Throw IllegalState exception
        if (!isConnected()) {
            this.ibAdapter.error(EClientErrors.NO_VALID_ID, EClientErrors.NOT_CONNECTED.code(), EClientErrors.NOT_CONNECTED.msg());
            return false;
        }

        return true;
    }

    // TODO Migrate to BiDiMap
    private static <K, V> K getAndRemoveKey(Map<K, V> map, V value) {
        for (Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue() == value) {
                map.remove(entry.getKey());
                return entry.getKey();
            }
        }
        return null;
    }

    // ==========================================================================
    // Thing translating calls from EWrapper to callbacks.
    // ==========================================================================
    private class IBAdapter implements EWrapper {

        @Override
        public void managedAccounts(String accountsStr) {
            List<String> accounts = new ArrayList<>();
            for (StringTokenizer st = new StringTokenizer(accountsStr, ","); st.hasMoreTokens();) {
                accounts.add(st.nextToken());
            }
            connectionHandler.accountList(accounts);
        }

        @Override
        public void nextValidId(int nextOrderId) {
            orderId.set(nextOrderId);
            reqId.set(nextOrderId + 10000000); // let order id's not collide with other request id's
            isConnected = true;
            if (connectionHandler != null) {
                connectionHandler.connected();
            }
        }

        @Override
        public void error(Exception e) {
            connectionHandler.error(e);
        }

        @Override
        public void error(int id, int errorCode, String errorMsg) {
            IOrderHandler handler = orderHandlers.get(id);
            if (handler != null) {
                handler.handle(errorCode, errorMsg);
            }

            for (ILiveOrderHandler liveHandler : IBService.this.liveOrderHandlers) {
                liveHandler.handle(id, errorCode, errorMsg);
            }

            // "no sec def found" response?
            if (errorCode == 200) {
                IInternalContractDetailsHandler hand = IBService.this.contractDetailsMap.remove(id);
                if (hand != null) {
                    hand.contractDetailsEnd();
                }
            }

            connectionHandler.message(id, errorCode, errorMsg);
        }

        @Override
        public void connectionClosed() {
            connectionHandler.disconnected();
            isConnected = false;
        }

        @Override
        public void updateAccountValue(String tag, String value, String currency, String account) {
            if ("Currency".equals(tag)) { // ignore this, it is useless
                return;
            }

            for (IAccountHandler handler : accountHandlers) {
                handler.accountValue(account, tag, value, currency);
            }
        }

        @Override
        public void updateAccountTime(String timeStamp) {
            for (IAccountHandler handler : accountHandlers) {
                handler.accountTime(timeStamp);
            }
        }

        @Override
        public void accountDownloadEnd(String account) {
            for (IAccountHandler handler : accountHandlers) {
                handler.accountDownloadEnd(account);
            }
        }

        @Override
        public void updatePortfolio(Contract contract,
                double positionIn,
                double marketPrice,
                double marketValue,
                double averageCost,
                double unrealizedPNL,
                double realizedPNL,
                String account) {

            contract.exchange(contract.primaryExch());

            Position position = new Position(contract, account, positionIn, marketPrice, marketValue, averageCost, unrealizedPNL, realizedPNL);
            for (IAccountHandler handler : accountHandlers) {
                handler.updatePortfolio(position);
            }
        }

        @Override
        public void accountSummary(int reqId, String account, String tag, String value, String currency) {
            if ("Currency".equals(tag)) { // ignore this, it is useless
                return;
            }

            IAccountSummaryHandler handler = acctSummaryHandlers.get(reqId);
            if (handler != null) {
                handler.accountSummary(account, AccountSummaryTag.valueOf(tag), value, currency);
            }

            IMarketValueSummaryHandler handler2 = mktValSummaryHandlers.get(reqId);
            if (handler2 != null) {
                handler2.marketValueSummary(account, MarketValueTag.valueOf(tag), value, currency);
            }
        }

        @Override
        public void accountSummaryEnd(int reqId) {
            IAccountSummaryHandler handler = acctSummaryHandlers.get(reqId);
            if (handler != null) {
                handler.accountSummaryEnd();
            }

            IMarketValueSummaryHandler handler2 = mktValSummaryHandlers.get(reqId);
            if (handler2 != null) {
                handler2.marketValueSummaryEnd();
            }
        }

        @Override
        public void position(String account, Contract contract, double pos, double avgCost) {
            for (IPositionHandler handler : positionHandlers) {
                handler.position(account, contract, pos, avgCost);
            }
        }

        @Override
        public void positionEnd() {
            for (IPositionHandler handler : positionHandlers) {
                handler.positionEnd();
            }
        }

        @Override
        public void contractDetails(int reqId, ContractDetails contractDetails) {
            IInternalContractDetailsHandler handler = contractDetailsMap.get(reqId);
            if (handler != null) {
                handler.contractDetails(contractDetails);
            } else {
                show("Error: no contract details handler for reqId " + reqId);
            }
        }

        @Override
        public void bondContractDetails(int reqId, ContractDetails contractDetails) {
            IInternalContractDetailsHandler handler = contractDetailsMap.get(reqId);
            if (handler != null) {
                handler.contractDetails(contractDetails);
            } else {
                show("Error: no bond contract details handler for reqId " + reqId);
            }
        }

        @Override
        public void contractDetailsEnd(int reqId) {
            IInternalContractDetailsHandler handler = contractDetailsMap.remove(reqId);
            if (handler != null) {
                handler.contractDetailsEnd();
            } else {
                show("Error: no contract details handler for reqId " + reqId);
            }
        }

        @Override
        public void tickPrice(int reqId, int tickType, double price, int canAutoExecute) {
            ITopMktDataHandler handler = topMktDataMap.get(reqId);
            if (handler != null) {
                handler.tickPrice(TickType.get(tickType), price, canAutoExecute);
            }
        }

        @Override
        public void tickGeneric(int reqId, int tickType, double value) {
            ITopMktDataHandler handler = topMktDataMap.get(reqId);
            if (handler != null) {
                handler.tickPrice(TickType.get(tickType), value, 0);
            }
        }

        @Override
        public void tickSize(int reqId, int tickType, int size) {
            ITopMktDataHandler handler = topMktDataMap.get(reqId);
            if (handler != null) {
                handler.tickSize(TickType.get(tickType), size);
            }
        }

        @Override
        public void tickString(int reqId, int tickType, String value) {
            ITopMktDataHandler handler = topMktDataMap.get(reqId);
            if (handler != null) {
                handler.tickString(TickType.get(tickType), value);
            }
        }

        @Override
        public void tickEFP(int reqId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact,
                double dividendsToLastTradeDate) {
            IEfpHandler handler = efpMap.get(reqId);
            if (handler != null) {
                handler.tickEFP(tickType, basisPoints, formattedBasisPoints, impliedFuture, holdDays, futureLastTradeDate, dividendImpact, dividendsToLastTradeDate);
            }
        }

        @Override
        public void tickSnapshotEnd(int reqId) {
            ITopMktDataHandler handler = topMktDataMap.get(reqId);
            if (handler != null) {
                handler.tickSnapshotEnd();
            }
        }

        @Override
        public void marketDataType(int reqId, int marketDataType) {
            ITopMktDataHandler handler = topMktDataMap.get(reqId);
            if (handler != null) {
                handler.marketDataType(MktDataType.get(marketDataType));
            }
        }

        @Override
        public void updateMktDepth(int reqId, int position, int operation, int side, double price, int size) {
            IDeepMktDataHandler handler = deepMktDataMap.get(reqId);
            if (handler != null) {
                handler.updateMktDepth(position, null, DeepType.get(operation), DeepSide.get(side), price, size);
            }
        }

        @Override
        public void updateMktDepthL2(int reqId, int position, String marketMaker, int operation, int side, double price, int size) {
            IDeepMktDataHandler handler = deepMktDataMap.get(reqId);
            if (handler != null) {
                handler.updateMktDepth(position, marketMaker, DeepType.get(operation), DeepSide.get(side), price, size);
            }
        }

        @Override
        public void tickOptionComputation(int reqId, int tickType, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
            IOptHandler handler = optionCompMap.get(reqId);
            if (handler != null) {
                handler.tickOptionComputation(TickType.get(tickType), impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice);
            } else {
                System.out.println(String.format("not handled %s %s %s %s %s %s %s %s %s", tickType, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice));
            }
        }

        @Override
        public void execDetails(int reqId, Contract contract, Execution execution) {
            log.info("execDetails: req {}, stock {}, orderId {}, avgPrice {}, execId {}",
                    reqId, contract.symbol(), execution.orderId(), execution.avgPrice(), execution.execId());

            if (tradeReportHandler != null) {
                int i = execution.execId().lastIndexOf('.');
                String tradeKey = execution.execId().substring(0, i);
                tradeReportHandler.tradeReport(tradeKey, contract, execution);
            }
        }

        @Override
        public void execDetailsEnd(int reqId) {
            if (tradeReportHandler != null) {
                tradeReportHandler.tradeReportEnd();
            }
        }

        @Override
        public void commissionReport(CommissionReport commissionReport) {
            if (tradeReportHandler != null) {
                int i = commissionReport.m_execId.lastIndexOf('.');
                String tradeKey = commissionReport.m_execId.substring(0, i);
                tradeReportHandler.commissionReport(tradeKey, commissionReport);
            }
        }

        @Override
        public final void receiveFA(int faDataType, String xml) {
            // do nothing
        }

        @Override
        public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
            IOrderHandler handler = orderHandlers.get(orderId);
            if (handler != null) {
                handler.orderState(orderState);
            }

            if (!order.whatIf()) {
                for (ILiveOrderHandler liveHandler : liveOrderHandlers) {
                    liveHandler.openOrder(contract, order, orderState);
                }
            }
        }

        @Override
        public void openOrderEnd() {
            for (ILiveOrderHandler handler : liveOrderHandlers) {
                handler.openOrderEnd();
            }
        }

        @Override
        public void orderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            IOrderHandler handler = orderHandlers.get(orderId);
            if (handler != null) {
                handler.orderStatus(OrderStatus.valueOf(status), filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
            }

            for (ILiveOrderHandler liveOrderHandler : liveOrderHandlers) {
                liveOrderHandler.orderStatus(orderId, OrderStatus.valueOf(status), filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);
            }
        }

        @Override
        public void scannerParameters(String xml) {
            scannerHandler.scannerParameters(xml);
        }

        @Override
        public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
            IScannerHandler handler = scannerMap.get(reqId);
            if (handler != null) {
                handler.scannerData(rank, contractDetails, legsStr);
            }
        }

        @Override
        public void scannerDataEnd(int reqId) {
            IScannerHandler handler = scannerMap.get(reqId);
            if (handler != null) {
                handler.scannerDataEnd();
            }
        }

        @Override
        public void historicalData(int reqId, String date, double open, double high, double low, double close, int volume, int count, double wap, boolean hasGaps) {
            IHistoricalDataHandler handler = historicalDataMap.get(reqId);
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

        @Override
        public void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count) {
            IRealTimeBarHandler handler = realTimeBarMap.get(reqId);
            if (handler != null) {
                Bar bar = new Bar(time, high, low, open, close, wap, volume, count);
                handler.realtimeBar(bar);
            }
        }

        @Override
        public void fundamentalData(int reqId, String data) {
            IFundamentalsHandler handler = fundamentalMap.get(reqId);
            if (handler != null) {
                handler.fundamentals(data);
            }
        }

        @Override
        public void currentTime(long time) {
            timeHandler.currentTime(time);
        }

        @Override
        public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
            bulletinHandler.bulletin(msgId, NewsType.get(msgType), message, origExchange);
        }

        @Override
        public void positionMulti(int reqId, String account, String modelCode, Contract contract, double pos, double avgCost) {
            IPositionMultiHandler handler = positionMultiMap.get(reqId);
            if (handler != null) {
                handler.positionMulti(account, modelCode, contract, pos, avgCost);
            }
        }

        @Override
        public void positionMultiEnd(int reqId) {
            IPositionMultiHandler handler = positionMultiMap.get(reqId);
            if (handler != null) {
                handler.positionMultiEnd();
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

        /** Obsolete, never called. */
        @Override
        public void error(String str) {
            throw new RuntimeException(str);
        }

        @Override
        public void connectAck() {
            if (client.isAsyncEConnect()) {
                client.startAPI();
            }
        }

        @Override
        public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {
            IAccountUpdateMultiHandler handler = accountUpdateMultiMap.get(reqId);
            if (handler != null) {
                handler.accountUpdateMulti(account, modelCode, key, value, currency);
            }
        }

        @Override
        public void accountUpdateMultiEnd(int reqId) {
            IAccountUpdateMultiHandler handler = accountUpdateMultiMap.get(reqId);
            if (handler != null) {
                handler.accountUpdateMultiEnd();
            }
        }

        @Override
        public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId, String tradingClass,
                String multiplier, Set<String> expirations, Set<Double> strikes) {
            ISecDefOptParamsReqHandler handler = secDefOptParamsReqMap.get(reqId);

            if (handler != null) {
                handler.securityDefinitionOptionalParameter(exchange, underlyingConId, tradingClass, multiplier, expirations, strikes);
            }
        }

        @Override
        public void securityDefinitionOptionalParameterEnd(int reqId) {
            ISecDefOptParamsReqHandler handler = secDefOptParamsReqMap.get(reqId);
            if (handler != null) {
                handler.securityDefinitionOptionalParameterEnd(reqId);
            }
        }

        @Override
        public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {
            ISoftDollarTiersReqHandler handler = softDollarTiersReqMap.get(reqId);

            if (handler != null) {
                handler.softDollarTiers(tiers);
            }
        }
    }
}
