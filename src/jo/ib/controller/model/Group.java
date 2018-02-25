/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.model;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.ib.client.Types.Method;

public class Group {
    private String name;
    private Method defaultMethod;
    private ArrayList<String> accounts = new ArrayList<String>();

    public String name() {
        return this.name;
    }

    public Method defaultMethod() {
        return this.defaultMethod;
    }

    public ArrayList<String> accounts() {
        return this.accounts;
    }

    public void name(String v) {
        this.name = v;
    }

    public void defaultMethod(Method v) {
        this.defaultMethod = v;
    }

    public void addAccount(String acct) {
        this.accounts.add(acct);
    }

    /**
     * @param val
     *            is a comma or space delimited string of accounts
     */
    public void setAllAccounts(String val) {
        this.accounts.clear();

        StringTokenizer st = new StringTokenizer(val, " ,");
        while (st.hasMoreTokens()) {
            this.accounts.add(st.nextToken());
        }
    }
}
