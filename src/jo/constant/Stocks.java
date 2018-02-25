package jo.constant;

import com.ib.client.Contract;
import com.ib.client.Types.SecType;

public class Stocks {

    public static Contract TQQQ() {
        Contract contract = new Contract();
        contract.secType(SecType.STK);
        contract.currency(Currency.USD);
        contract.exchange("SMART");
        contract.primaryExch("ISLAND");
        contract.symbol("TQQQ");
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
