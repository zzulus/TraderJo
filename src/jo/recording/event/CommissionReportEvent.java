package jo.recording.event;

import com.ib.client.CommissionReport;

public class CommissionReportEvent extends AbstractEvent {
    public static final String TYPE = "CommissionReport";
    private String tradeKey;
    private CommissionReport commissionReport;

    public CommissionReportEvent() {
        super(TYPE);
    }

    public CommissionReportEvent(String tradeKey, CommissionReport commissionReport) {
        super(TYPE);
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