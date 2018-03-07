package jo.recording;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;

import jo.controller.IBService;
import jo.handler.ILiveOrderHandler;
import jo.handler.ITradeReportHandler;
import jo.recording.event.BaseEvent;
import jo.recording.event.CommissionReportEvent;
import jo.recording.event.OpenOrderEvent;
import jo.recording.event.OrderErrorEvent;
import jo.recording.event.OrderStatusEvent;
import jo.recording.event.TradeReportEvent;

public class TradeRecorder implements Recorder {
    private static final Logger log = LogManager.getLogger(TradeRecorder.class);
    private OutputStream out;
    private PrintWriter ps;
    private ArrayBlockingQueue<BaseEvent> q = new ArrayBlockingQueue<>(64000);
    private ObjectMapper objectMapper = new ObjectMapper();

    public TradeRecorder() {
        objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }

    public void start(IBService ib) {
        openFile();

        Thread writerThread = new Thread(this::pollQueue);
        writerThread.setDaemon(true);
        writerThread.setName("OrderRecorder");
        writerThread.start();

        Handler handler = new Handler();

        ExecutionFilter filter = new ExecutionFilter();
        ib.reqExecutions(filter, handler);
        ib.reqLiveOrders(handler);
    }

    @Override
    public void stop() {
        ps.close();
    }

    public void pollQueue() {
        try {
            while (true) {
                Object event = q.take();
                String str = objectMapper.writeValueAsString(event);
                ps.println(str); // todo writye to stream closes stream
            }
        } catch (InterruptedException e) {
            // terminated
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void openFile() {
        LocalDateTime now = LocalDateTime.now();
        File dir = new File("log/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE));
        dir.mkdir();

        String logFileName = "Trades.log";
        File logFile = new File(dir, logFileName);

        try {
            out = new FileOutputStream(logFile, true);
            ps = new PrintWriter(out);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private class Handler implements ITradeReportHandler, ILiveOrderHandler {

        @Override
        public void tradeReport(String tradeKey, Contract contract, Execution execution) {
            q.add(new TradeReportEvent(tradeKey, contract, execution));
        }

        @Override
        public void tradeReportEnd() {
            // q.add(new TradeReportEndEvent());
        }

        @Override
        public void commissionReport(String tradeKey, CommissionReport commissionReport) {
            q.add(new CommissionReportEvent(tradeKey, commissionReport));
        }

        @Override
        public void openOrder(Contract contract, Order order, OrderState orderState) {
            q.add(new OpenOrderEvent(contract, order, orderState));
        }

        @Override
        public void openOrderEnd() {

        }

        @Override
        public void orderStatus(int orderId, OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            q.add(new OrderStatusEvent(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld));
        }

        @Override
        public void handle(int orderId, int errorCode, String errorMsg) {
            q.add(new OrderErrorEvent(orderId, errorCode, errorMsg));
        }
    }

}
