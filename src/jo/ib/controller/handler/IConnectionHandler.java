package jo.ib.controller.handler;

import java.util.ArrayList;

// ---------------------------------------- Constructor and Connection handling ----------------------------------------
public interface IConnectionHandler {
    void connected();

    void disconnected();

    void accountList(ArrayList<String> list);

    void error(Exception e);

    void message(int id, int errorCode, String errorMsg);

    void show(String string);
}