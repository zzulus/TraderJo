/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.model;

public class Alias {
    private String account;
    private String alias;

    public String alias() {
        return alias;
    }

    public String account() {
        return account;
    }

    public void alias(String v) {
        alias = v;
    }

    public void account(String v) {
        account = v;
    }

    @Override
    public String toString() {
        return account + " / " + alias;
    }
}
