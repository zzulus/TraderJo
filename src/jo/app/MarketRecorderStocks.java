package jo.app;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class MarketRecorderStocks {
    // http://etfdb.com/compare/volume/
    public static final Set<String> TICKS_ONLY_ETFS = ImmutableSet.of(
            "SPY",
            "SQQQ",
            "EEM",
            "XLF",
            "VXX",
            "UVXY",
            "GDX",
            "TVIX",
            "EFA",
            "IWM",
            "SPXL");

    // SPY holdings
    public static final Set<String> TICKS_ONLY_STOCKS = ImmutableSet.of(
            // "AAPL",
            "MSFT",
            "AMZN",
            "FB",
            "JPM",
            "JNJ",
            "GOOG",
            "GOOGL",
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
            "NFLX",
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
}
