/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EMessage;
import com.ib.client.EWrapper;
import com.ib.client.Order;

public class ApiConnection extends EClientSocket {
    public static final char EOL = 0;
    public static final char LOG_EOL = '_';

    private static final EJavaSignal signal = new EJavaSignal();

    public ApiConnection(EWrapper wrapper) {
        super(wrapper, signal);
    }

    @Override
    protected void sendMsg(EMessage msg) throws IOException {
        super.sendMsg(msg);
    }

    @Override
    public int readInt() throws IOException {
        int c = super.readInt();
        return c;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int n = super.read(buf, off, len);
        return n;
    }

    public synchronized void placeOrder(Contract contract, Order order) {
        if (!isConnected()) {
            notConnected();
            return;
        }

        placeOrder(order.orderId(), contract, order);
    }
}
