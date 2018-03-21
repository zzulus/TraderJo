package jo.constant;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

public class Stocks {

    public static Contract TQQQ(boolean smart) {
        Contract contract = new Contract();
        contract.symbol("TQQQ");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        contract.primaryExch("NASDAQ");
        return contract;
    }

    public static Contract SQQQ(boolean smart) {
        Contract contract = new Contract();
        contract.symbol("SQQQ");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        contract.primaryExch("NASDAQ");
        return contract;
    }
    
    public static Contract QQQ(boolean smart) {
        Contract contract = new Contract();
        contract.symbol("QQQ");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        contract.primaryExch("NASDAQ");
        return contract;
    }

    public static Contract SPY(boolean smart) {
        Contract contract = new Contract();
        contract.symbol("SPY");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        contract.primaryExch("ARCA");
        return contract;
    }

    public static Contract MSFT(boolean smart) {
        Contract contract = new Contract();
        contract.symbol("MSFT");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        contract.primaryExch("ISLAND");
        return contract;
    }
    
    public static Contract of(String name, boolean smart) {
        Contract contract = new Contract();
        contract.symbol(name);
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        contract.primaryExch("ISLAND");
        return contract;
    }

    public static Contract AAPL(boolean smart) {
        Contract contract = new Contract();
        contract.symbol("AAPL");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        contract.primaryExch("NASDAQ");
        return contract;
    }

    public static Contract toNasdaq(Contract c) {
        Contract contract = new Contract();
        contract.symbol(c.symbol());
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange("ISLAND");
        contract.primaryExch("ISLAND");
        return contract;
    }
}
