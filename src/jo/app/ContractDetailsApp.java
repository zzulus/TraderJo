package jo.app;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;

import jo.controller.IBService;
import jo.controller.IBroker;
import jo.handler.ConnectionHandlerAdapter;
import jo.util.AsyncVal;

public class ContractDetailsApp {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final IBroker ib = new IBService();
        Contract contract = new Contract();
        contract.symbol("AMZN");
        contract.secType("STK");
        // contract.currency("USD");

        AsyncVal<List<ContractDetails>> c = AsyncVal.create();

        ib.connectLocalhostLive(new ConnectionHandlerAdapter() {
            @Override
            public void connected() {
                ib.reqContractDetails(contract, c::set);
            }
        });

        List<ContractDetails> list = c.get();
        for (ContractDetails cd : list) {
            System.out.println(cd);
            // System.out.println(cd.contract().exchange());
            // System.out.println(cd.contract().currency());
            // System.out.println(cd.minTick());
            System.out.println("==================");
            // System.out.println(ToStringBuilder.reflectionToString(cd));
        }

        // ib.disconnect();
        System.exit(0);
    }
}
