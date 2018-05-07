package jo.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableSet;

public class MyStocks {
    // http://etfdb.com/compare/volume/
    public static final Set<String> TICKS_ONLY_ETFS = ImmutableSet.of(
            "SPY",
            "SQQQ",
            "TQQQ",
            "QQQ",
            "EEM",
            "XLF",
            "VXX",
            "UVXY",
            "GDX",
            "TVIX",
            "EFA",
            "IWM",
            "FXI",
            "USO",
            "XLU",
            "AMLP",
            "EWZ",
            "XOP",
            "JNK",
            "SVXY",
            "HYG",
            "XLE",
            "VWO",
            "XLK",
            "XLP",
            "XLI",
            "SPXU",
            "GDXJ",
            "JNUG",
            "IAU",
            "IEMG",
            "EWJ",
            "VEA",
            "TZA",
            "TLT",
            "XLV",
            "IEFA",
            "LABD",
            "OIH",
            "RSX",
            "IYR",
            "EZU",
            "SLV",
            "GLD",
            "DGAZ",
            "LQD",
            "XLB",
            "SDS",
            "NUGT",
            "VNQ",
            "XLY",
            "DIA",
            "KRE",
            "QID",
            "XRT",
            "EWT",
            "IVV",
            "EWH",
            "SPXL",
            "SH",
            "DWT",
            "SMH",
            "DUST");

    // SPY holdings
    public static final Set<String> TICKS_ONLY_STOCKS = ImmutableSet.of(
            // "AAPL",
            "MSFT",
            "AMZN",
            "FB",
            "JPM",
            "JNJ",
            // "GOOG",
            // "GOOGL",
            "XOM",
            "BAC",
            "WFC",
            "INTC",
            "T",
            "V",
            "UNH",
            "CVX",
            "PFE",
            "CSCO",
            "HD",
            "VZ",
            "PG",
            "C",
            "BA",
            "ABBV",
            "MA",
            "KO",
            "CMCSA",
            "PM",
            "ORCL",
            "PEP",
            "DWDP",
            "DIS",
            "MRK",
            "NVDA",
            "MMM",
            //"NFLX", book depth sucks
            "IBM",
            "WMT",
            "MCD",
            "GE",
            "AMGN",
            "MO",
            "HON",
            "ADBE",
            "MDT",
            "ABT",
            "BMY",
            "UNP",
            "TXN");

    public static final Set<String> STOCKS_TO_TRADE = ImmutableSet.of(
            "AAPL",
            "MSFT",
            //"AMZN",
            "FB",
            "JPM",
            "JNJ",
            "XOM",
            "BAC",
            "WFC",
            "INTC",
            "T",
            "V",
            "UNH",
            "CVX",
            "PFE",
            "CSCO",
            "HD",
            "VZ",
            "PG",
            "C",
            "BA",
            "ABBV",
            "MA",
            "KO",
            "CMCSA",
            "PM",
            "ORCL",
            "PEP",
            "DWDP",
            "DIS",
            "MRK",
            "NVDA",
            "MMM",
            "IBM",
            "WMT",
            "MCD",
            "GE",
            "AMGN",
            "MO",
            "HON",
            "ADBE",
            "MDT",
            "ABT",
            "BMY",
            "UNP",
            "TXN");

    public static final Set<String> EARNINGS_STOCKS = new LinkedHashSet<>();
    static {
        try (InputStream is = new FileInputStream("earnings.txt")) {
            List<String> lines = IOUtils.readLines(is, Charset.defaultCharset());
            lines.stream()
                    .map(s -> s.trim())
                    .filter(s -> !s.startsWith("#"))
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> EARNINGS_STOCKS.add(s));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
