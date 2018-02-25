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

    public Contract getContract() {
        return this.contract;
    }

    public int getConid() {
        return this.contract.conid();
    }

    public double getAverageCost() {
        return this.averageCost;
    }

    public double getMarketPrice() {
        return this.marketPrice;
    }

    public double getMarketValue() {
        return this.marketValue;
    }

    public double getRealPnl() {
        return this.realPnl;
    }

    public double getUnrealPnl() {
        return this.unrealPnl;
    }

    public double getPosition() {
        return this.position;
    }

    public String getAccount() {
        return this.account;
    }

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
