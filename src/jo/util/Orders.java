package jo.util;

import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types.Action;

import jo.controller.IBroker;

public class Orders {
    public static Order newLimitBuyOrder(IBroker ib, int totalQuantity, double lmtPrice) {
        Order order = new Order();
        order.orderId(ib.getNextOrderId());
        order.action(Action.BUY);
        order.orderType(OrderType.LMT);
        order.totalQuantity(totalQuantity);
        order.lmtPrice(lmtPrice);
        order.transmit(false);

        return order;
    }

    public static Order newLimitSellOrder(IBroker ib, int totalQuantity, double lmtPrice) {
        Order order = new Order();
        order.orderId(ib.getNextOrderId());
        order.action(Action.SELL);
        order.orderType(OrderType.LMT);
        order.totalQuantity(totalQuantity);
        order.lmtPrice(lmtPrice);
        order.transmit(false);

        return order;
    }

    // ==============
    public static Order newMktBuyOrder(IBroker ib, int totalQuantity) {
        Order order = new Order();
        order.orderId(ib.getNextOrderId());
        order.action(Action.BUY);
        order.orderType(OrderType.MKT);
        order.totalQuantity(totalQuantity);
        //order.lmtPrice(lmtPrice);
        order.transmit(false);

        return order;
    }

    public static Order newMktSellOrder(IBroker ib, int totalQuantity) {
        Order order = new Order();
        order.orderId(ib.getNextOrderId());
        order.action(Action.SELL);
        order.orderType(OrderType.MKT);
        order.totalQuantity(totalQuantity);
        order.transmit(false);

        return order;
    }

    // ==============
    public static Order newMocBuyOrder(IBroker ib, int totalQuantity, int parentId) {
        Order order = new Order();
        order.orderId(ib.getNextOrderId());
        order.action(Action.BUY);
        order.orderType(OrderType.MOC);
        order.totalQuantity(totalQuantity);
        order.parentId(parentId);
        order.transmit(false);

        return order;
    }

    public static Order newMocSellOrder(IBroker ib, int totalQuantity, int parentId) {
        Order order = new Order();
        order.orderId(ib.getNextOrderId());
        order.action(Action.SELL);
        order.orderType(OrderType.MOC);
        order.totalQuantity(totalQuantity);
        order.parentId(parentId);
        order.transmit(false);

        return order;
    }

    // ==============
    public static Order newStopBuyOrder(IBroker ib, int totalQuantity, double stopLossPrice, int parentId) {
        Order order = new Order();
        order.orderId(ib.getNextOrderId());
        order.action(Action.BUY);
        order.orderType(OrderType.STP);
        order.totalQuantity(totalQuantity);
        order.auxPrice(stopLossPrice);
        order.parentId(parentId);
        order.transmit(false);

        return order;
    }

    public static Order newStopSellOrder(IBroker ib, int totalQuantity, double stopLossPrice, int parentId) {
        Order order = new Order();
        order.orderId(ib.getNextOrderId());
        order.action(Action.SELL);
        order.orderType(OrderType.STP);
        order.totalQuantity(totalQuantity);
        order.auxPrice(stopLossPrice);
        order.parentId(parentId);
        order.transmit(false);

        return order;
    }
}
