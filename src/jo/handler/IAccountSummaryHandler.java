package jo.handler;

import jo.controller.model.AccountSummaryTag;

// ---------------------------------------- Account Summary handling ----------------------------------------
public interface IAccountSummaryHandler {
    void accountSummary(String account, AccountSummaryTag tag, String value, String currency);

    void accountSummaryEnd();
}