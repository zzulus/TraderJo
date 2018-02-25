/* Copyright (C) 2013 Interactive Brokers LLC. All rights reserved.  This code is subject to the terms
 * and conditions of the IB API Non-Commercial License or the IB API Commercial License, as applicable. */

package jo.ib.controller.model;

import com.ib.client.Contract;

public class Position {
    private Contract contract;
    private String account;
    private double position;
    private double marketPrice;
    private double marketValue;
    private double averageCost;
    private double unrealPnl;
    private double realPnl;

    public Contract contract() {
        return this.contract;
    }

    public int conid() {
        return this.contract.conid();
    }

    public double averageCost() {
        return this.averageCost;
    }

    public double marketPrice() {
        return this.marketPrice;
    }

    public double marketValue() {
        return this.marketValue;
    }

    public double realPnl() {
        return this.realPnl;
    }

    public double unrealPnl() {
        return this.unrealPnl;
    }

    public double position() {
        return this.position;
    }

    public String account() {
        return this.account;
    }

    // public void account(String v) { this.account = v;}
    // public void averageCost(double v) { this.averageCost = v;}
    // public void marketPrice(double v) { this.marketPrice = v;}
    // public void marketValue(double v) { this.marketValue = v;}
    // public void position(int v) { this.position = v;}
    // public void realPnl(double v) { this.realPnl = v;}
    // public void unrealPnl(double v) { this.unrealPnl = v;}

    public Position(Contract contract, String account, double position, double marketPrice, double marketValue, double averageCost, double unrealPnl, double realPnl) {
        this.contract = contract;
        this.account = account;
        this.position = position;
        this.marketPrice = marketPrice;
        this.marketValue = marketValue;
        this.averageCost = averageCost;
        this.unrealPnl = unrealPnl;
        this.realPnl = realPnl;
    }
}
