package jo.command;

import java.util.List;

import jo.bot.Bot;
import jo.controller.IBroker;
import jo.model.IApp;

public class StartBotsCommand implements AppCommand {
    private List<Bot> bots;

    public StartBotsCommand(List<Bot> bots) {
        this.bots = bots;
    }

    @Override
    public void execute(IBroker ib, IApp app) {
        for (Bot bot : bots) {
            bot.init(ib, app);
            bot.start();
        }
    }
}
