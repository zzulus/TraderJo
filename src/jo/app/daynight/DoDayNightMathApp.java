package jo.app.daynight;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static jo.util.Formats.fmtDate;

/*

OpenE -> OpenR -> CloseR -> CloseE -> OpenE
 */
public class DoDayNightMathApp {
    public static void main(String[] args) throws IOException {
        List<Megabar> bars = loadBars("TraderJo/historical/day-vs-night/QQQ.json", "2000-06-04");

        List<AccumulatingColumn> columns = new ArrayList<>();

        columns.add(new SetColumn("baseline", (prevDay, day) -> calcPercChange(bars.get(0).tradeOpenR, day.tradeCloseR)));
        columns.add(new AccumulatingColumn("openEToOpenR_SameDay", (prevDay, day) -> calcPercChange(day.tradeOpenE, day.tradeOpenR)));
        columns.add(new AccumulatingColumn("openRToCloseR_SameDay", (prevDay, day) -> calcPercChange(day.tradeOpenR, day.tradeCloseR)));
        columns.add(new AccumulatingColumn("closeRToCloseE_SameDay", (prevDay, day) -> calcPercChange(day.tradeCloseR, day.tradeCloseE)));
        columns.add(new AccumulatingColumn("closeEToOpenE_Overnight", (prevDay, day) -> calcPercChange(prevDay.tradeCloseE, day.tradeOpenE)));

//        columns.add(new SmartColumn("openRToCloseR_SameDay", (prevDay, day) -> calcPercChange(day.tradeOpenR, day.tradeCloseR)));
//        columns.add(new SmartColumn("openRToCloseE_SameDay", (prevDay, day) -> calcPercChange(day.tradeOpenR, day.tradeCloseE)));
//        columns.add(new SmartColumn("openEToCloseR_SameDay", (prevDay, day) -> calcPercChange(day.tradeOpenE, day.tradeCloseR)));
//        columns.add(new SmartColumn("openEToCloseE_SameDay", (prevDay, day) -> calcPercChange(day.tradeOpenE, day.tradeCloseE)));
//        columns.add(new SmartColumn("openEToOpenR_SameDay", (prevDay, day) -> calcPercChange(day.tradeOpenE, day.tradeOpenR)));
//        columns.add(new SmartColumn("closeRToCloseE_SameDay", (prevDay, day) -> calcPercChange(day.tradeCloseR, day.tradeCloseE)));
//        columns.add(new SmartColumn("openRToOpenR", (prevDay, day) -> calcPercChange(prevDay.tradeOpenR, day.tradeOpenR)));
//        columns.add(new SmartColumn("openRToOpenE", (prevDay, day) -> calcPercChange(prevDay.tradeOpenR, day.tradeOpenE)));
//        columns.add(new SmartColumn("openEToOpenR", (prevDay, day) -> calcPercChange(prevDay.tradeOpenE, day.tradeOpenR)));
//        columns.add(new SmartColumn("openEToOpenE", (prevDay, day) -> calcPercChange(prevDay.tradeOpenE, day.tradeOpenE)));
//        columns.add(new SmartColumn("closeRToCloseR", (prevDay, day) -> calcPercChange(prevDay.tradeCloseR, day.tradeCloseR)));
//        columns.add(new SmartColumn("closeRToCloseE", (prevDay, day) -> calcPercChange(prevDay.tradeCloseR, day.tradeCloseE)));
//        columns.add(new SmartColumn("closeEToCloseR", (prevDay, day) -> calcPercChange(prevDay.tradeCloseE, day.tradeCloseR)));
//        columns.add(new SmartColumn("closeEToCloseE", (prevDay, day) -> calcPercChange(prevDay.tradeCloseE, day.tradeCloseE)));
//        columns.add(new SmartColumn("closeRToOpenR", (prevDay, day) -> calcPercChange(prevDay.tradeCloseR, day.tradeOpenR)));
//        columns.add(new SmartColumn("closeRToOpenE", (prevDay, day) -> calcPercChange(prevDay.tradeCloseR, day.tradeOpenE)));
//        columns.add(new SmartColumn("closeEToOpenR", (prevDay, day) -> calcPercChange(prevDay.tradeCloseE, day.tradeOpenR)));
//        columns.add(new SmartColumn("closeEToOpenE", (prevDay, day) -> calcPercChange(prevDay.tradeCloseE, day.tradeOpenE)));

        System.out.println("Date\t" +
                columns.stream().map(col -> col.name).collect(Collectors.joining("\t"))
        );

        for (int i = 1; i < bars.size(); i++) {
            Megabar prevDay = bars.get(i - 1);
            Megabar day = bars.get(i);
            columns.forEach(col -> col.bump(prevDay, day));

            System.out.printf("%s\t" +
                            columns.stream().map(c -> String.format("%.2f", c.value)).collect(Collectors.joining("\t"))
                            + "%n",
                    fmtDate(day.time * 1000)
            );
        }

        System.out.printf("%n%nNo touch %.2f", calcPercChange(bars.get(0).tradeOpenR, bars.get(bars.size() - 1).tradeCloseR));
    }

    private static double calcPercChange(double open, double close) {
        return 100d * (close - open) / open;
    }

    private static List<Megabar> loadBars(String path, String after) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        List<String> lines = Files.readAllLines(Paths.get(path));
        List<Megabar> result = new ArrayList<>();
        for (String line : lines) {
            Megabar bar = objectMapper.readValue(line, Megabar.class);
            if (fmtDate(bar.time * 1000).compareTo(after) >= 0)
                result.add(bar);
        }

        result.sort(Comparator.comparingLong(l -> l.time));
        return result;
    }
}

class AccumulatingColumn {
    String name;
    Calculator megaBrain;
    double value = 0;

    public AccumulatingColumn(String name, Calculator megaBrain) {
        this.name = name;
        this.megaBrain = megaBrain;
    }

    void bump(Megabar prevDay, Megabar day) {
        value += megaBrain.calc(prevDay, day);
    }
}

class SetColumn extends AccumulatingColumn {
    public SetColumn(String name, Calculator megaBrain) {
        super(name, megaBrain);
    }

    void bump(Megabar prevDay, Megabar day) {
        value = megaBrain.calc(prevDay, day);
    }
}

class PosNegColumn extends AccumulatingColumn {
    public PosNegColumn(String name, Calculator megaBrain) {
        super(name, megaBrain);
    }

    void bump(Megabar prevDay, Megabar day) {
        double x = megaBrain.calc(prevDay, day);
        if (x > 0)
            value = 10;
        else
            value = -10;
    }
}

interface Calculator {
    double calc(Megabar prevDay, Megabar day);
}
