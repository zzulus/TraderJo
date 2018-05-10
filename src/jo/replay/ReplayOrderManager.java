package jo.replay;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.TickType;
import com.ib.client.Types.Action;

import jo.handler.IOrderHandler;
import jo.model.MarketData;
import jo.model.OrderStatusInput;
import jo.recording.event.AbstractEvent;
import jo.recording.event.TickPriceEvent;

public class ReplayOrderManager {
    private static final Logger log = LogManager.getLogger(ReplayOrderManager.class);
    private static final Set<TickType> acceptedTickTypes = ImmutableSet.of(TickType.ASK, TickType.BID, TickType.LAST);
    private final ReplayContext ctx;
    private final Map<Integer, ReplayOrder> openOrders = new HashMap<>();
    private final List<ReplayOrder> filledOrders = new ArrayList<>();
    private final Map<String, MutableInt> positions = new HashMap<>();
    private double pnl = 0;
    private double commissions = 0;

    public ReplayOrderManager(ReplayContext ctx) {        
        this.ctx = ctx;
    }

    public synchronized void tick(Contract contract, AbstractEvent event) {
        if (!(event instanceof TickPriceEvent)) {
            return;
        }

        TickPriceEvent e = (TickPriceEvent) event;
        TickType tickType = e.getTickType();
        if (!acceptedTickTypes.contains(tickType)) {
            return;
        }

        List<ReplayOrder> openOrderCopy = new ArrayList<>(openOrders.values());

        for (ReplayOrder replayOrder : openOrderCopy) {
            if (!replayOrder.getContract().symbol().equals(contract.symbol())) {
                continue;
            }

            Order order = replayOrder.getOrder();
            int parentId = order.parentId();
            if (parentId != 0 && openOrders.containsKey(parentId)) {
                // bracket order
                continue;
            }

            OrderType orderType = order.orderType();
            Action action = order.action();
            MarketData marketData = ctx.getMarketData(replayOrder.getContract().symbol());

            double buyPrice = marketData.getAskPrice();
            double sellPrice = marketData.getBidPrice();

            if (tickType == TickType.LAST) {
                buyPrice = e.getPrice();
                sellPrice = e.getPrice();
            }

            // TODO Add support of trail limit and stop loss
            if (orderType == OrderType.LMT) {
                if (action == Action.BUY && order.lmtPrice() >= buyPrice) {
                    executeOrder(replayOrder, order.lmtPrice()); // should be bid/ask price, but in fact it is always lmt price
                }
                if (action == Action.SELL && order.lmtPrice() <= sellPrice) {
                    executeOrder(replayOrder, order.lmtPrice());
                }

            } else if (order.orderType() == OrderType.MKT) {
                double executePrice;
                if (action == Action.BUY) {
                    executePrice = buyPrice;
                } else {
                    executePrice = sellPrice;
                }

                executeOrder(replayOrder, executePrice);
            } else if (orderType == OrderType.STP) {
                if (action == Action.BUY && order.lmtPrice() >= buyPrice) {
                    executeOrder(replayOrder, buyPrice);
                }
                if (action == Action.SELL && order.lmtPrice() <= sellPrice) {
                    executeOrder(replayOrder, sellPrice);
                } // TODO Cancel other part of bracket order
            } else {
                throw new IllegalArgumentException("Not supported yet order type " + order.orderType());
            }
        }

    }

    private void executeOrder(ReplayOrder replayOrder, double executePrice) {
        Order order = replayOrder.getOrder();
        openOrders.remove(order.orderId());
        filledOrders.add(replayOrder);

        double totalQuantity = order.totalQuantity();
        String symbol = replayOrder.getContract().symbol();
        Action action = order.action();
        MutableInt position = positions.computeIfAbsent(symbol, (k) -> new MutableInt());

        if (action == Action.BUY) {
            position.add((int) totalQuantity);
            pnl = pnl - totalQuantity * executePrice;
            commissions += 1.0;
        } else {
            position.subtract((int) totalQuantity);
            pnl = pnl + totalQuantity * executePrice;
            commissions += 1.2;
        }
        positions.put(symbol, position);

        log.info("Order executed: {}: {} {} {} for {} -> {}",
                symbol, action, totalQuantity, order.orderType(), order.lmtPrice(), executePrice);
        log.info("P&L: " + String.format("%.2f", pnl));

        OrderState orderState = newOrderState(OrderStatus.Filled.name());
        IOrderHandler handler = replayOrder.getHandler();
        handler.orderState(null, orderState); // TODO

        OrderStatusInput input = new OrderStatusInput();
        input.setStatus(OrderStatus.Filled);
        input.setFilled(order.totalQuantity());
        input.setRemaining(0);
        input.setAvgFillPrice(executePrice);
        input.setParentId(order.parentId());
        input.setLastFillPrice(executePrice);

        handler.orderStatus(input);
    }

    public synchronized void placeOrModifyOrder(Contract contract, Order order, IOrderHandler handler) {
        ReplayOrder replayOrder = new ReplayOrder(contract, order, handler);
        openOrders.put(order.orderId(), replayOrder);
    }

    public synchronized void cancelAllOrders() {
        openOrders.clear();
    }

    public synchronized void cancelOrder(int orderId) {
        ReplayOrder replayOrder = openOrders.remove(orderId);

        if (replayOrder == null) {
            throw new IllegalStateException("Order not found " + orderId);
        }
    }

    private OrderState newOrderState(String status) {
        try {
            Constructor<OrderState> constructor = OrderState.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            OrderState result = constructor.newInstance();
            result.status(status);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized Stats getStats() {
        double potentialPnl = pnl;

        for (ReplayOrder replayOrder : openOrders.values()) {
            Order order = replayOrder.getOrder();
            double totalQuantity = order.totalQuantity();
            double lmtPrice = order.lmtPrice();
            Action action = order.action();

            if (action == Action.BUY) {
                potentialPnl = potentialPnl - totalQuantity * lmtPrice;
                // commissions += 1.0;
            } else {
                potentialPnl = potentialPnl + totalQuantity * lmtPrice;
                // commissions += 1.2;
            }
        }

        Stats stats = new Stats(openOrders, filledOrders, positions, pnl, potentialPnl, commissions);
        return stats;
    }
}
