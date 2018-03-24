package jo.tech;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import jo.model.Bar;
import jo.model.Bars;

public class DonchianChannelTest {
    private static final int LOWER_PERIOD = 3;
    private static final int UPPER_PERIOD = 2;
    private Bars bars;
    private Bar bar1;
    private Bar bar2;
    private Bar bar3;
    private Bar bar4;
    private DonchianChannel donchian;

    @Before
    public void setup() {
        bars = new Bars();
        bar1 = new Bar();
        bar2 = new Bar();
        bar3 = new Bar();
        bar4 = new Bar();

        bar1.setLow(1);
        bar1.setHigh(40);

        bar2.setLow(2);
        bar2.setHigh(30);

        bar3.setLow(3);
        bar3.setHigh(20);

        bar4.setLow(4);
        bar4.setHigh(10);

        donchian = new DonchianChannel(bars, LOWER_PERIOD, UPPER_PERIOD);
    }

    @Test
    public void testNoData() {
        Channel val = donchian.get();
        assertNull(val);
    }

    @Test
    public void testNotEnoughData() {
        // empty
        assertNull(donchian.get());

        // less than lower period
        bars.addBar(bar1);
        assertNull(donchian.get());

        // less than upper period
        bars.addBar(bar2);
        assertNull(donchian.get());

        // just enough than upper period
        bars.addBar(bar3);
        assertNotNull(donchian.get());
    }

    @Test
    public void testGetValueWithMinimalNumberOfBars() {
        bars.addBar(bar1);
        bars.addBar(bar2);
        bars.addBar(bar3);

        Channel channel = donchian.get();
        assertEquals(1.0, channel.getLower(), 0.0);
        assertEquals(30.0, channel.getUpper(), 0.0);
        assertEquals((1.0 + 30.0) / 2, channel.getMiddle(), 0.0);
    }

    @Test
    public void testGetValue() {
        bars.addBar(bar1);
        bars.addBar(bar2);
        bars.addBar(bar3);
        bars.addBar(bar4);

        Channel channel = donchian.get();
        assertEquals(2.0, channel.getLower(), 0.0);
        assertEquals(20.0, channel.getUpper(), 0.0);
        assertEquals((2.0 + 20.0) / 2, channel.getMiddle(), 0.0);
    }

    @Test
    public void testGetValueCachesValue() {
        bars.addBar(bar1);
        bars.addBar(bar2);
        bars.addBar(bar3);
        bars.addBar(bar4);

        Channel channel1 = donchian.get();
        Channel channel2 = donchian.get();
        assertSame(channel1, channel2);
    }

}
