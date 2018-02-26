/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.controller;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EWrapper;
import com.ib.client.Order;

public class ApiConnection extends EClientSocket {
    private static final EJavaSignal signal = new EJavaSignal();

    public ApiConnection(EWrapper wrapper) {
        super(wrapper, signal);
    }

    public synchronized void placeOrder(Contract contract, Order order) {
        if (!isConnected()) {
            notConnected();
            return;
        }

        placeOrder(order.orderId(), contract, order);
    }
}
