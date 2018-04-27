package jo.chart;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;

public class BarChartUtils {
    private static ChartTheme currentTheme = StandardChartTheme.createDarknessTheme();

    public static void main(final String[] args) throws Exception {
        int size = 1;
        Date[] date = new Date[size];
        double[] high = new double[size];
        double[] low = new double[size];
        double[] open = new double[size];
        double[] close = new double[size];
        double[] volume = new double[size];

        date[0] = new Date();
        high[0] = 5;
        low[0] = 1;
        open[0] = 2;
        close[0] = 4;
        volume[0] = 0;

        DefaultHighLowDataset dataset = new DefaultHighLowDataset("hhhkey", date, high, low, open, close, volume);

        JFreeChart chart = createChart(dataset);
        chart.getXYPlot().setOrientation(PlotOrientation.VERTICAL);

        File f = new File("PNGTimeSeriesChartDemo1.png");

        BufferedImage chartImage = chart.createBufferedImage(600, 400, null);
        ImageIO.write(chartImage, "png", f);
        System.out.println(f.getAbsolutePath());
    }

    private static JFreeChart createChart(final DefaultHighLowDataset dataset) {
        final JFreeChart chart = createCandlestickChart(
                "Title",
                "Time",
                "Price",
                dataset,
                true);
        return chart;
    }

    public static JFreeChart createCandlestickChart(String title, String timeAxisLabel, String valueAxisLabel, OHLCDataset dataset, boolean legend) {
        ValueAxis timeAxis = new DateAxis(timeAxisLabel);
        NumberAxis valueAxis = new NumberAxis(valueAxisLabel);
        XYPlot plot = new XYPlot(dataset, timeAxis, valueAxis, null);
        plot.setRenderer(new CandlestickRenderer());
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
        currentTheme.apply(chart);
        return chart;
    }

}
