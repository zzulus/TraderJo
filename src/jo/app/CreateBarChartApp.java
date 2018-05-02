package jo.app;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.XYDataset;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;

import jo.chart.MyCandlestickRenderer;
import jo.chart.MyStockDataset;
import jo.constant.Stocks;
import jo.controller.IBService;
import jo.handler.ConnectionHandlerAdapter;
import jo.handler.IHistoricalDataHandler;
import jo.model.Bar;
import jo.model.Bars;
import jo.util.AsyncVal;

public class CreateBarChartApp {
    private final static ChartTheme theme = StandardChartTheme.createDarknessTheme();
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);        
    }

    public static void main(String[] args) throws Exception {
        Contract contract = Stocks.smartOf("FB");

        createImage(contract);
        System.out.println("Done, ignore exception");

        System.exit(0);
    }

    private static void createImage(Contract contract) throws Exception {        
        DateAxis timeAxis = new DateAxis("Time");
        timeAxis.setAutoRange(true);
        timeAxis.setTickLabelsVisible(true);
        timeAxis.setAutoTickUnitSelection(true);

        NumberAxis priceAxis = new NumberAxis("Price");
        priceAxis.setAutoRange(true);
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());

        XYPlot plot = new XYPlot();
        plot.setDomainAxis(timeAxis);
        plot.setRangeAxis(0, priceAxis);
        
        
        MyStockDataset barsDataset = loadDataset(contract);        
        plot.setDataset(0, barsDataset);

        CandlestickRenderer renderer1 = new MyCandlestickRenderer();
        StandardXYItemRenderer renderer2 = new StandardXYItemRenderer();
        plot.setRenderer(0, renderer1);
        
        JFreeChart chart = new JFreeChart(contract.symbol(), JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.getXYPlot().setOrientation(PlotOrientation.VERTICAL);
        theme.apply(chart);

        File f = new File("PNGTimeSeriesChartDemo1.png");

        BufferedImage chartImage = chart.createBufferedImage(barsDataset.getItemCount(0) * 15, 2000);
        ImageIO.write(chartImage, "png", f);
        System.out.println("file://" + f.getAbsolutePath().replaceAll("\\\\", "/"));
        new ProcessBuilder("D:\\Program Files (x86)\\XnView\\xnview.exe", f.getAbsolutePath()).start();
    }

    private static MyStockDataset loadDataset(Contract contract) throws IOException, JsonParseException, JsonMappingException, Exception {
        MyStockDataset barsDataset;
        File cachedDatasetFile = new File("Cached_" + contract.symbol() + ".txt");
        if (cachedDatasetFile.exists()) {
            barsDataset = objectMapper.readValue(cachedDatasetFile, MyStockDataset.class);
        } else {
            Bars bars = retrieveBarsFromIB(contract);
            barsDataset = barsToDataSet(bars);

            try (FileOutputStream fos = new FileOutputStream(cachedDatasetFile)) {
                String str = objectMapper.writeValueAsString(barsDataset);
                fos.write(str.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw e;
            }
        }
        return barsDataset;
    }

    private static MyStockDataset barsToDataSet(Bars bars) {
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

        MyStockDataset barsDataset = new MyStockDataset("", date, high, low, open, close, volume);
        return barsDataset;
    }

    private static Bars retrieveBarsFromIB(Contract contract) {
        BarSize barSize = BarSize._1_min;
        int duration = 1;
        DurationUnit durationUnit = DurationUnit.DAY;

        AsyncVal<Bars> barsEx = new AsyncVal<>();
        IBService ib = new IBService();
        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            @Override
            public void connected() {

                ib.reqHistoricalData(contract, "", duration, durationUnit, barSize, WhatToShow.TRADES, true, new IHistoricalDataHandler() {
                    final Bars bars = new Bars();

                    @Override
                    public void historicalDataEnd() {
                        System.out.println("Bars downloaded\n");

                        barsEx.set(bars);
                    }

                    @Override
                    public void historicalData(Bar bar) {
                        bars.addBar(bar);
                    }
                });
            }
        });

        Bars bars = barsEx.get();
        ib.disconnect();
        return bars;
    }
}
