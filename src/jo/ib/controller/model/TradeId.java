/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.model;

public class TradeId {
    private String key;
    private String full;

    public String getKey() {
        return this.key;
    }

    public String getFull() {
        return this.full;
    }

    public TradeId(String id) {
        this.full = id;
        int i = id.lastIndexOf('.');
        this.key = id.substring(i + 1);
    }
}
