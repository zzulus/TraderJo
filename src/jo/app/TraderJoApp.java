package jo.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.Types.BarSize;

import jo.constant.Stocks;

public class TraderJoApp implements EWrapper {
	private static final String LOCALHOST = "127.0.0.1";
	private static final int REAL_PORT = 7496;
	private static final int PAPER_PORT = 7497;
	private static final int CLIENT_ID = (int) (System.currentTimeMillis() / 10000);
	private final Logger log = LogManager.getLogger(TraderJoApp.class);

	private EJavaSignal signal;
	private EClientSocket client;
	private EReader reader;

	private AtomicInteger nextValidOrderId = new AtomicInteger();
	private AtomicInteger requestId = new AtomicInteger();
	private List<String> managedAccounts;

	public void setNextOrderId(int nextOrderId) {
		this.nextValidOrderId.set(nextOrderId);
	}

	public int getNextOrderId() {
		return nextValidOrderId.incrementAndGet();
	}

	public int getNextRequestId() {
		return requestId.incrementAndGet();
	}

	public List<String> getManagedAccounts() {
		return managedAccounts;
	}

	public void setManagedAccounts(List<String> managedAccounts) {
		this.managedAccounts = managedAccounts;
	}

	public void connect() {
		signal = new EJavaSignal();
		client = new EClientSocket(this, signal);
		client.eConnect(LOCALHOST, REAL_PORT, CLIENT_ID);
		log.info("Connecting");
		Preconditions.checkArgument(client.isConnected(), "Not connected");

		reader = new EReader(client, signal);
		reader.start();

		new Thread() {
			public void run() {
				while (client.isConnected()) {
					// System.out.println("Waiting For Signal");
					signal.waitForSignal();
					// System.out.println("Signal!!!");
					try {
						reader.processMsgs();
					} catch (IOException e) {
						error(e);
					}
				}
			}
		}.start();
	}

	private void onConnected() {
		System.out.println("OnConnected");
		Contract contract = Stocks.TQQQ();
		ArrayList<TagValue> realTimeBarsOptions = new ArrayList<TagValue>();

		client.reqContractDetails(getNextRequestId(), contract);
		// client.reqMktData(getNextRequestId(), contract, "", false, null);
		// client.reqRealTimeBars(getNextRequestId(), contract, 0, "TRADES",
		// false, realTimeBarsOptions);
		// client.reqMktDepth(getNextRequestId(), contract, 16, null);
		client.reqHistoricalData(getNextRequestId(), contract, "201802021 23:59:59 GMT", "30 D",
				BarSize._1_min.toString(), "TRADES", 1, 1, null);
		// 1 secs, 5 secs, 10 secs, 15 secs, 30 secs, 1 min, 2 mins, 3 mins, 5
		// mins, 10 mins, 15 mins, 20 mins, 30 mins, 1 hour, 2 hours, 3 hours, 4
		// hours, 8 hours, 1 day, 1W, 1M
	}

	private static synchronized void printf(String s, Object... args) {
		System.out.println(String.format(s, args));
	}

	@Override
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		printf("tickPrice: tickerId %d, field %d, price %s, canAutoExecute %d", tickerId, field, price, canAutoExecute);
	}

	@Override
	public void tickSize(int tickerId, int field, int size) {
		printf("tickSize: tickerId %d, field %d, size %s", tickerId, field, size);
	}

	@Override
	public void tickOptionComputation(int tickerId, int field, double impliedVol, double delta, double optPrice,
			double pvDividend, double gamma, double vega, double theta, double undPrice) {

	}

	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {
		printf("tickGeneric: tickerId %d, tickType %d, value %s", tickerId, tickType, value);
	}

	@Override
	public void tickString(int tickerId, int tickType, String value) {
		// printf("tickString: tickerId %d, tickType %d, value %s", tickerId,
		// tickType, value);
	}

	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints,
			double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact,
			double dividendsToLastTradeDate) {

	}

	@Override
	public void orderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice,
			int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {

	}

	@Override
	public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {

	}

	@Override
	public void openOrderEnd() {

	}

	@Override
	public void updateAccountValue(String key, String value, String currency, String accountName) {

	}

	@Override
	public void updatePortfolio(Contract contract, double position, double marketPrice, double marketValue,
			double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {

	}

	@Override
	public void updateAccountTime(String timeStamp) {
		System.out.println("updateAccountTime " + timeStamp);
	}

	@Override
	public void accountDownloadEnd(String accountName) {

	}

	@Override
	public void nextValidId(int nextOrderId) {
		System.out.println("nextOrderId: " + nextOrderId);
		setNextOrderId(nextOrderId);

		onConnected();
	}

	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) {
		System.out.println("contractDetails: " + reqId + ": " + contractDetails);
	}

	@Override
	public void contractDetailsEnd(int reqId) {
		System.out.println("contractDetailsEnd: " + reqId);
	}

	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {

	}

	@Override
	public void execDetails(int reqId, Contract contract, Execution execution) {

	}

	@Override
	public void execDetailsEnd(int reqId) {

	}

	@Override
	public void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size) {
		printf("updateMktDepth tickerId=%s, position=%s, operation=%s, side=%s, price=%s, size=%s", tickerId, position,
				operation, side, price, size);
	}

	@Override
	public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price,
			int size) {
		printf("updateMktDepthL2 tickerId=%s, position=%s, operation=%s, side=%s, price=%s, size=%s", tickerId,
				position, operation, side, price, size);
	}

	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {

	}

	@Override
	public void managedAccounts(String accountsStr) {
		System.out.println("managedAccounts: " + accountsStr);

		String[] arr = StringUtils.split(accountsStr, ',');
		List<String> accounts = Arrays.asList(arr);

		setManagedAccounts(accounts);
	}

	@Override
	public void receiveFA(int faDataType, String xml) {

	}

	@Override
	public void historicalData(int reqId, String date, double open, double high, double low, double close, int volume,
			int count, double WAP, boolean hasGaps) {

		printf("historicalData: reqId %s, date %s, open %s, high %s, low %s, close %s, volume %s, count %s, WAP %s, hasGaps %s",
				reqId, date, open, high, low, close, volume, count, WAP, hasGaps);
	}

	@Override
	public void scannerParameters(String xml) {

	}

	@Override
	public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark,
			String projection, String legsStr) {

	}

	@Override
	public void scannerDataEnd(int reqId) {

	}

	@Override
	public void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume,
			double wap, int count) {

		printf("realtimeBar reqId=%s, time=%s, open=%s, high=%s, low=%s, close=%s, volume=%s, wap=%s, count=%s", reqId,
				time, open, high, low, close, volume, wap, count);

	}

	@Override
	public void currentTime(long time) {
		printf("currentTime %s", time);
	}

	@Override
	public void fundamentalData(int reqId, String data) {

	}

	@Override
	public void deltaNeutralValidation(int reqId, DeltaNeutralContract underComp) {

	}

	@Override
	public void tickSnapshotEnd(int reqId) {
		printf("tickSnapshotEnd %d", reqId);
	}

	@Override
	public void marketDataType(int reqId, int marketDataType) {

	}

	@Override
	public void commissionReport(CommissionReport commissionReport) {

	}

	@Override
	public void position(String account, Contract contract, double pos, double avgCost) {

	}

	@Override
	public void positionEnd() {

	}

	@Override
	public void accountSummary(int reqId, String account, String tag, String value, String currency) {

	}

	@Override
	public void accountSummaryEnd(int reqId) {

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

	@Override
	public void error(Exception e) {
		System.err.println("Got error: ");
		e.printStackTrace();
	}

	@Override
	public void error(String str) {
		System.err.println("Got error: " + str);
	}

	@Override
	public void error(int requestId, int errorCode, String errorMsg) {
		String msg = String.format(": requestId=%d, errorCode=%d, errorMsg=%s", requestId, errorCode, errorMsg);
		if (requestId == -1) {
			System.out.println("Info" + msg);
		} else {
			System.out.println("Error" + msg);
		}
	}

	@Override
	public void connectionClosed() {
		System.out.println("connectionClosed");
	}

	@Override
	public void connectAck() {
		System.out.println("connectAck");
	}

	@Override
	public void positionMulti(int reqId, String account, String modelCode, Contract contract, double pos,
			double avgCost) {

	}

	@Override
	public void positionMultiEnd(int reqId) {

	}

	@Override
	public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value,
			String currency) {

	}

	@Override
	public void accountUpdateMultiEnd(int reqId) {

	}

	@Override
	public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId,
			String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes) {

	}

	@Override
	public void securityDefinitionOptionalParameterEnd(int reqId) {

	}

	@Override
	public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {

	}

}
