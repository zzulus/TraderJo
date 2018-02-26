package jo.handler;

import java.util.List;

// ---------------------------------------- Constructor and Connection handling ----------------------------------------
public interface IConnectionHandler {
    void connected();

    void disconnected();

    void accountList(List<String> accounts);

    void error(Exception e);

    void message(int id, int errorCode, String errorMsg);

    void show(String string);
}