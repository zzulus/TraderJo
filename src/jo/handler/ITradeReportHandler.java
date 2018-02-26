package jo.handler;

import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.Execution;

// ---------------------------------------- Trade reports ----------------------------------------
public interface ITradeReportHandler {
    void tradeReport(String tradeKey, Contract contract, Execution execution);

    void tradeReportEnd();

    void commissionReport(String tradeKey, CommissionReport commissionReport);
}