package jo.recording.event;

import com.ib.client.CommissionReport;

public class CommissionReportEvent extends BaseEvent {
    private String tradeKey;
    private CommissionReport commissionReport;

    public CommissionReportEvent(String tradeKey, CommissionReport commissionReport) {
        super("CommissionReport");
        this.tradeKey = tradeKey;
        this.commissionReport = commissionReport;
    }

    public String getTradeKey() {
        return tradeKey;
    }

    public void setTradeKey(String tradeKey) {
        this.tradeKey = tradeKey;
    }

    public CommissionReport getCommissionReport() {
        return commissionReport;
    }

    public void setCommissionReport(CommissionReport commissionReport) {
        this.commissionReport = commissionReport;
    }
}