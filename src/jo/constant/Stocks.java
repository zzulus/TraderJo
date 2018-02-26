package jo.constant;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

public class Stocks {

    public static Contract TQQQ_SMART() {
        Contract contract = new Contract();
        contract.symbol("TQQQ");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange("SMART");
        contract.primaryExch("NASDAQ");
        contract.conid(72539702);
        return contract;
    }

    public static Contract TQQQ_NASDAQ() {
        Contract contract = new Contract();
        contract.symbol("TQQQ");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange("ISLAND");
        contract.primaryExch("NASDAQ");
        contract.conid(72539702);
        return contract;
    }

    public static Contract MSFT_SMART() {
        Contract contract = new Contract();
        contract.symbol("MSFT");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange("SMART");
        contract.primaryExch("ISLAND");
        return contract;
    }

    public static Contract MSFT_NASDAQ() {
        Contract contract = new Contract();
        contract.symbol("MSFT");
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange("ISLAND");
        contract.primaryExch("ISLAND");
        return contract;
    }

    public static Contract RWE_IBIS() {
        Contract contract = new Contract();
        contract.secType(SecType.STK);
        contract.currency(Currency.EUR);
        contract.exchange("SMART");
        contract.symbol("RWE");
        return contract;
    }
}
