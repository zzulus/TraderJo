package jo.constant;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

public class Stocks {

    public static Contract of(String name, boolean smart) {
        Contract contract = new Contract();
        contract.symbol(name);
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        contract.primaryExch("ISLAND");
        return contract;
    }

    public static Contract callOptionOf(String name, boolean smart, int strike, String date) {
        Contract contract = new Contract();
        contract.symbol(name);
        contract.secType(SecType.OPT);
        contract.currency(Currency.USD);
        contract.exchange(smart ? "SMART" : "ISLAND");
        //contract.primaryExch("ISLAND");
        contract.lastTradeDateOrContractMonth(date);       
        contract.right("C"); // call
        contract.strike(strike);
        contract.multiplier("100");

        return contract;
    }

    public static Contract smartOf(String name) {
        return of(name, true);
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
