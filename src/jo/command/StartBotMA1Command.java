package jo.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.Types.Action;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.WhatToShow;

import jo.app.App;
import jo.controller.IBService;
import jo.handler.IOrderHandler;
import jo.model.Bars;
import jo.model.MarketData;
import jo.signal.AllSignals;
import jo.signal.BelowSimpleAverageSignal;
import jo.signal.HasAtLeastNBarsSignal;
import jo.signal.NotCloseToDailyHighSignal;
import jo.signal.Signal;

public class StartBotMA1Command implements AppCommand {
    private static final Logger log = LogManager.getLogger(StartBotMA1Command.class);
    private Contract contract;
    private MarketData marketData;
    private Bars bars;

    private Signal signal;
    private volatile boolean hasActiveTrade = false;

    private OpenPositionOrderHandler openPositionOrderHandler = new OpenPositionOrderHandler();
    private TakeProfitOrderHandler takeProfitOrderHandler = new TakeProfitOrderHandler();

    public StartBotMA1Command(Contract contract) {
        this.contract = contract;

        List<Signal> signals = new ArrayList<>();
        signals.add(new HasAtLeastNBarsSignal(90 / 5)); // 90 seconds
        signals.add(new NotCloseToDailyHighSignal(0.2d));
        signals.add(new BelowSimpleAverageSignal(90 / 5, 0.03d)); // 90 seconds

        signal = new AllSignals(signals);
    }

    @Override
    public void execute(IBService ib, App app) {
        log.info("Start bot for {}", contract.symbol());
        marketData = app.getStockMarketData(contract.symbol());
        bars = marketData.getBars(BarSize._5_secs);

        new Thread("Bot 1#" + contract.symbol()) {
            @Override
            public void run() {
                int activeCnt = 0;
                while (true) {
                    try {
                        Thread.currentThread().sleep(100l);
                        if (hasActiveTrade) {
                            continue;
                        }

                        if (signal.isActive(app, contract, marketData)) {
                            activeCnt++;
                            log.info("Signal is active " + marketData.getLastPrice());

                            if (activeCnt > 3) {
                                int totalQuantity = 50;
                                double lastPrice = marketData.getLastPrice();

                                Order openOrder = new Order();
                                openOrder.orderId(ib.getNextOrderId());
                                openOrder.action(Action.BUY);
                                openOrder.orderType("LMT");
                                openOrder.totalQuantity(totalQuantity);
                                openOrder.lmtPrice(lastPrice - 0.02d);
                                openOrder.transmit(false);

                                Order takeProfitOrder = new Order();
                                takeProfitOrder.orderId(ib.getNextOrderId());
                                takeProfitOrder.action(Action.SELL);
                                takeProfitOrder.orderType("LMT");
                                takeProfitOrder.totalQuantity(totalQuantity);
                                takeProfitOrder.lmtPrice(lastPrice + 0.10d);
                                takeProfitOrder.parentId(openOrder.orderId());
                                takeProfitOrder.transmit(true);

                                log.info("Placing order: Open at {}, close at {}, last {}", openOrder.lmtPrice(), takeProfitOrder.lmtPrice(), lastPrice);
                                                                
                                ib.placeOrModifyOrder(contract, openOrder, openPositionOrderHandler);
                                ib.placeOrModifyOrder(contract, takeProfitOrder, takeProfitOrderHandler);

                                hasActiveTrade = true;
                            }
                        } else {
                            // log.info("Signal is passive");
                            activeCnt = 0;
                        }

                    } catch (Exception e) {
                        log.error("Error in bot", e);
                    }
                }
            }
        }.start();
    }

    private class OpenPositionOrderHandler implements IOrderHandler {
        private final Logger log = LogManager.getLogger(OpenPositionOrderHandler.class);

        @Override
        public void orderState(OrderState orderState) {
            log.error("orderState: {}", orderState.getStatus());
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.error("orderStatus: status {}, filled {}, remaining {}", status, filled, remaining);
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
        }

    }

    private class TakeProfitOrderHandler implements IOrderHandler {
        private final Logger log = LogManager.getLogger(TakeProfitOrderHandler.class);

        @Override
        public void orderState(OrderState orderState) {
            log.info("orderState: {}", orderState.getStatus());
            if ("Filled".equals(orderState.getStatus())) {
                hasActiveTrade = false;
            }
        }

        @Override
        public void orderStatus(OrderStatus status, double filled, double remaining, double avgFillPrice, long permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
            log.error("orderStatus: status {}, filled {}, remaining {}", status, filled, remaining);
        }

        @Override
        public void handle(int errorCode, String errorMsg) {
            log.error("Error: {} - {}", errorCode, errorMsg);
        }
    }
}
