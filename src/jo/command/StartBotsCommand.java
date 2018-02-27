package jo.command;

import java.util.List;

import jo.app.App;
import jo.bot.Bot;
import jo.controller.IBService;

public class StartBotsCommand implements AppCommand {
    private List<Bot> bots;

    public StartBotsCommand(List<Bot> bots) {
        this.bots = bots;
    }

    @Override
    public void execute(IBService ib, App app) {
        for (Bot bot : bots) {
            bot.start(ib, app);
        }
    }
}
