package jo.command;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.Uninterruptibles;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import jo.bot.Bot;
import jo.controller.IApp;
import jo.controller.IBroker;

public class StartBotsCommand implements AppCommand {
    private static final Logger log = LogManager.getLogger(StartBotsCommand.class);
    // https://interactivebrokers.github.io/tws-api/historical_limitations.html
    private static final long PACING_TIME_WINDOW_SECONDS = TimeUnit.MINUTES.toSeconds(9) + 50; // 9 min 50 seconds just in case
    private static final int PACING_MAX_REQUESTS = 50;

    private List<Bot> bots;

    public StartBotsCommand(List<Bot> bots) {
        this.bots = bots;
    }

    @Override
    public void execute(IBroker ib, IApp app) {
        TLongList times = new TLongArrayList();

        int botNum = 0;
        for (Bot bot : bots) {
            log.info("Bot {}/{}", botNum++, bots.size());
            times.add(currentTimeSeconds());

            bot.init(ib, app);
            bot.start();

            avoidPacingViolation(times);
        }
    }

    private void avoidPacingViolation(TLongList times) {
        if (times.size() >= PACING_MAX_REQUESTS) {
            int offset = times.size() - PACING_MAX_REQUESTS;
            long timeStamp = times.get(offset);
            long waitPeriodSeconds = PACING_TIME_WINDOW_SECONDS - (currentTimeSeconds() - timeStamp);

            if (waitPeriodSeconds > 0) {
                log.info("Sleeping {} seconds to avoid pacing vioalation", waitPeriodSeconds);
                Uninterruptibles.sleepUninterruptibly(waitPeriodSeconds, TimeUnit.SECONDS);
            }
        }
    }

    private long currentTimeSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
