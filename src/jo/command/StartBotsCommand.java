package jo.command;

import static java.lang.System.currentTimeMillis;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.Uninterruptibles;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import jo.bot.Bot;
import jo.model.Context;

public class StartBotsCommand implements AppCommand {
    private static final Logger log = LogManager.getLogger(StartBotsCommand.class);
    // https://interactivebrokers.github.io/tws-api/historical_limitations.html
    // Making more than 60 requests within any 10 minute period
    private static final long PACING_TIME_WINDOW_MS = TimeUnit.MINUTES.toMillis(10);
    private static final int PACING_MAX_REQUESTS = 50;

    private List<Bot> bots;

    public StartBotsCommand(List<Bot> bots) {
        this.bots = bots;
    }

    @Override
    public void execute(Context ctx) {
        TLongList botsStartTime = new TLongArrayList();

        int botNum = 1;
        for (Bot bot : bots) {
            log.info("Starting bot {}/{}", botNum++, bots.size());
            botsStartTime.add(currentTimeMillis());

            bot.init(ctx);
            bot.start();

            avoidPacingViolation(botsStartTime);
        }
    }

    private void avoidPacingViolation(TLongList times) {
        // try to not spook IB off
        Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);

        if (times.size() >= PACING_MAX_REQUESTS) {
            int offset = times.size() - PACING_MAX_REQUESTS;
            long timeStampMs = times.get(offset);
            long waitTimeMs = PACING_TIME_WINDOW_MS - (currentTimeMillis() - timeStampMs);

            if (waitTimeMs > 0) {
                log.info("Sleeping {} ms to avoid pacing vioalation", waitTimeMs);
                Uninterruptibles.sleepUninterruptibly(waitTimeMs, TimeUnit.MILLISECONDS);
            }
        }
    }

}
