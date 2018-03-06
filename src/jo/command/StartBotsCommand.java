package jo.command;

import java.util.List;

import jo.app.TraderApp;
import jo.bot.Bot;
import jo.controller.IBService;

public class StartBotsCommand implements AppCommand {
    private List<Bot> bots;

    public StartBotsCommand(List<Bot> bots) {
        this.bots = bots;
    }

    @Override
    public void execute(IBService ib, TraderApp app) {
        for (Bot bot : bots) {
            bot.start(ib, app);
        }
    }
}
