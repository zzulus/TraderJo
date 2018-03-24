package jo.bot;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import jo.app.IApp;
import jo.controller.IBroker;
import jo.model.Bars;
import jo.tech.Channel;
import jo.tech.DonchianChannel;
import jo.util.AsyncExec;
import jo.util.SyncSignal;

public class DonchianChannelBot extends BaseBot {
    private SyncSignal sig;
    private DonchianChannel donchian;
    private final int lowerPeriod = 25;
    private final int upperPeriod = 25;

    public DonchianChannelBot(Contract contract, int totalQuantity) {
        super(contract, totalQuantity);
    }

    @Override
    public void start(IBroker ib, IApp app) {
        log.info("Start bot for {}", contract.symbol());

        this.md = app.getMarketData(contract.symbol());
        this.sig = md.getUpdateSignal();
        Bars bars = md.getBars(BarSize._5_secs);
        this.donchian = new DonchianChannel(bars, lowerPeriod, upperPeriod);

        String threadName = "DonchianBot#" + contract.symbol();
        AsyncExec.startThread(threadName, this::run);
    }

    public void runLoop() {
        Channel ch = donchian.get();
        if (ch == null) {
            return;
        }
        
        double lastPrice = md.getLastPrice();

    }

    public void run() {
        while (sig.waitForSignal()) {
            try {
                if (Thread.interrupted()) {
                    return;
                }
                runLoop();

            } catch (Exception e) {
                log.error("Error in bot", e);
            }
        }
    }
}
