package jo.app;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;

import jo.constant.Stocks;
import jo.controller.IBService;
import jo.controller.IBroker;
import jo.handler.ConnectionHandlerAdapter;
import jo.handler.IHistoricalDataHandler;
import jo.model.Bar;
import jo.model.Bars;
import jo.util.AsyncVal;

public class CreateBarChartApp {
    private final static Logger log = LogManager.getLogger(CreateBarChartApp.class);
    private final static ChartTheme theme = StandardChartTheme.createDarknessTheme();

    public static void main(String[] args) throws Exception {
        IBroker ib = new IBService();
        Contract contract = Stocks.smartOf("AAPL");
        BarSize barSize = BarSize._30_secs;
        int duration = 1;
        DurationUnit durationUnit = DurationUnit.DAY;

        AsyncVal<Bars> barsEx = new AsyncVal<>();

        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            @Override
            public void connected() {

                ib.reqHistoricalData(contract, "", duration, durationUnit, barSize, WhatToShow.TRADES, true, new IHistoricalDataHandler() {
                    final Bars bars = new Bars();

                    @Override
                    public void historicalDataEnd() {
                        System.out.println("End");
                        barsEx.set(bars);
                    }

                    @Override
                    public void historicalData(Bar bar) {
                        bars.addBar(bar);
                        // (bar.getHigh() < 100 || bar.getLow() < 100 || bar.getOpen() < 100 || bar.getClose() < 100)
                        //System.out.println(hasGaps + " " + bar);
                    }
                });
            }
        });

        Bars bars = barsEx.get();
        createImage(contract, bars);
        System.out.println("Done, ignore exception");
        ib.disconnect();
        System.exit(0);
    }

    private static void createImage(Contract contract, Bars bars) throws Exception {
        int size = bars.getSize();

        Date[] date = new Date[size];
        double[] high = bars.getHigh().toArray();
        double[] low = bars.getLow().toArray();
        double[] open = bars.getOpen().toArray();
        double[] close = bars.getClose().toArray();

        double[] volume = new double[size];

        long[] volumeSrc = bars.getVolume().toArray();
        long[] timeSrc = bars.getTime().toArray();

        for (int i = 0; i < size; i++) {
            volume[i] = volumeSrc[i];
            date[i] = new Date(1000 * timeSrc[i]);
        }

        DefaultHighLowDataset dataset = new DefaultHighLowDataset("", date, high, low, open, close, volume);

        JFreeChart chart = createChart(contract.symbol(), dataset);
        chart.getXYPlot().setOrientation(PlotOrientation.VERTICAL);

        File f = new File("PNGTimeSeriesChartDemo1.png");

        BufferedImage chartImage = chart.createBufferedImage(size * 15, 2000);
        ImageIO.write(chartImage, "png", f);
        System.out.println("file:///" + f.getAbsolutePath().replaceAll("\\\\", "/"));
    }

    private static JFreeChart createChart(String symbol, final DefaultHighLowDataset dataset) {
        final JFreeChart chart = createCandlestickChart(
                symbol,
                "Time",
                "Price",
                dataset,
                true);
        return chart;
    }

    public static JFreeChart createCandlestickChart(String title, String timeAxisLabel, String valueAxisLabel, OHLCDataset dataset, boolean legend) {
        DateAxis timeAxis = new DateAxis(timeAxisLabel);
        timeAxis.setAutoRange(true);
        timeAxis.setTickLabelsVisible(true);
        timeAxis.setAutoTickUnitSelection(true);

        NumberAxis priceAxis = new NumberAxis(valueAxisLabel);
        priceAxis.setAutoRange(true);        
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());


        //priceAxis.setRange(162, 167);

        XYPlot plot = new XYPlot(dataset, timeAxis, priceAxis, null);
        CandlestickRenderer renderer = new MyCandlestickRenderer();

        plot.setRenderer(renderer);
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
        theme.apply(chart);
        return chart;
    }

    public static class MyCandlestickRenderer extends org.jfree.chart.renderer.xy.CandlestickRenderer {
        private final Paint colorRaising = Color.GREEN;
        private final Paint colorFalling = Color.RED;
        private final Paint colorUnknown = Color.GRAY;
        private final Paint colorTransparent = Color.BLACK;

        public MyCandlestickRenderer() {
            setDrawVolume(false);
            setUpPaint(colorUnknown); // use unknown color if error
            setDownPaint(colorUnknown); // use unknown color if error
            
        }

        @Override
        public Paint getItemPaint(int series, int item) {
            OHLCDataset highLowData = (OHLCDataset) getPlot().getDataset(series);
            Number curClose = highLowData.getClose(series, item);
            Number prevClose = highLowData.getClose(series, item > 0 ? item - 1 : 0);

            if (prevClose.doubleValue() <= curClose.doubleValue()) {
                return Color.GREEN;
            } else {
                return Color.RED;
            }
        }

        @Override
        public void drawItem(Graphics2D g2, XYItemRendererState state,
                Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                int series, int item, CrosshairState crosshairState, int pass) {

            OHLCDataset highLowData = (OHLCDataset) dataset;
            double yOpen = highLowData.getOpenValue(series, item);
            double yClose = highLowData.getCloseValue(series, item);

            // set color for filled candle
            if (yClose >= yOpen) {
                setUpPaint(colorRaising);
                setDownPaint(colorFalling);
            }

            // set color for hollow (not filled) candle
            else {
                setUpPaint(colorTransparent);
                setDownPaint(colorTransparent);
            }

            // call parent method
            super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item, crosshairState, pass);
        }

    }
}
