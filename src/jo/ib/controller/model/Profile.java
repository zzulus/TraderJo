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

    public String getName() {
        return this.name;
    }

    public Type getType() {
        return this.type;
    }

    public List<Allocation> getAllocations() {
        return this.allocations;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void addAllocation(Allocation allocation) {
        this.allocations.add(allocation);
    }

    public void setAllocations(String allocationsStr) {
        this.allocations.clear();

        StringTokenizer st = new StringTokenizer(allocationsStr, ", ");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(tok, SEPARATOR);

            Allocation alloc = new Allocation();
            alloc.setAccount(st2.nextToken());
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

        public String getAccount() {
            return this.account;
        }

        public String getAmount() {
            return this.amount;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public void amount(String amount) {
            this.amount = amount;
        }

        @Override
        public String toString() {
            return this.account + SEPARATOR + this.amount;
        }
    }
}
