package jo.ib.controller.handler;

import com.ib.client.Types.NewsType;

// ---------------------------------------- Bulletins handling ----------------------------------------
public interface IBulletinHandler {
    void bulletin(int msgId, NewsType newsType, String message, String exchange);
}