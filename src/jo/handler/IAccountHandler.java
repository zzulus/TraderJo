package jo.handler;

import jo.controller.model.Position;

// ---------------------------------------- Account and portfolio updates ----------------------------------------
public interface IAccountHandler {
    public void accountValue(String account, String key, String value, String currency);

    public void accountTime(String timeStamp);

    public void accountDownloadEnd(String account);

    public void updatePortfolio(Position position);
}