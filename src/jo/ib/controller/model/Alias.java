/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.model;

public class Alias {
    private String account;
    private String alias;

    public String getAlias() {
        return alias;
    }

    public String getAccount() {
        return account;
    }

    public void setAlias(String v) {
        alias = v;
    }

    public void setAccount(String v) {
        account = v;
    }

    @Override
    public String toString() {
        return account + " / " + alias;
    }
}
