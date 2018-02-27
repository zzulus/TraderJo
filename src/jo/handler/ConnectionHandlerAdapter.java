package jo.handler;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionHandlerAdapter implements IConnectionHandler {
    private static final Logger log = LogManager.getLogger(ConnectionHandlerAdapter.class);

    @Override
    public void connected() {
        log.info("Connected");
    }

    @Override
    public void disconnected() {
        log.info("Disconnected");
    }

    @Override
    public void accountList(List<String> accounts) {
    }

    @Override
    public void error(Exception e) {
        log.error("Error", e);
    }

    @Override
    public void message(int id, int errorCode, String errorMsg) {
        if (id == -1 && errorMsg.contains("data farm connection is OK")) {
            return;
        }
        log.info("Message: id {}, errorCode {}, errorMsg {}", id, errorCode, errorMsg);
    }

    @Override
    public void show(String string) {
        log.info("Show: {}", string);
    }

}
