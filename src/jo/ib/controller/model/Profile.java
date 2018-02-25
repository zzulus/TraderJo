/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.ib.client.Types;

public class Profile {
    private static final String SEPARATOR = "/";

    private String name;
    private Type type;
    private List<Allocation> allocations = new ArrayList<Allocation>();

    public String name() {
        return this.name;
    }

    public Type type() {
        return this.type;
    }

    public List<Allocation> allocations() {
        return this.allocations;
    }

    public void name(String v) {
        this.name = v;
    }

    public void type(Type v) {
        this.type = v;
    }

    public void add(Allocation v) {
        this.allocations.add(v);
    }

    public void setAllocations(String val) {
        this.allocations.clear();

        StringTokenizer st = new StringTokenizer(val, ", ");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(tok, SEPARATOR);

            Allocation alloc = new Allocation();
            alloc.account(st2.nextToken());
            alloc.amount(st2.nextToken());

            this.allocations.add(alloc);
        }
    }

    public static enum Type {
        NONE, Percents, Ratios, Shares;

        public static Type get(int ordinal) {
            return Types.getEnum(ordinal, values());
        }
    };

    public static class Allocation {
        private String account;
        private String amount;

        public String account() {
            return this.account;
        }

        public String amount() {
            return this.amount;
        }

        public void account(String v) {
            this.account = v;
        }

        public void amount(String v) {
            this.amount = v;
        }

        @Override
        public String toString() {
            return this.account + SEPARATOR + this.amount;
        }
    }
}
