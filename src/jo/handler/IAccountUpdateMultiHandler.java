package jo.handler;

// ---------------------------------------- Account Update Multi handling ----------------------------------------
public interface IAccountUpdateMultiHandler {
    void accountUpdateMulti(String account, String modelCode, String key, String value, String curreny);

    void accountUpdateMultiEnd();
}