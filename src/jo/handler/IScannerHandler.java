package jo.handler;

import com.ib.client.ContractDetails;

// ---------------------------------------- Market Scanners ----------------------------------------
public interface IScannerHandler {
    void scannerParameters(String xml);

    void scannerData(int rank, ContractDetails contractDetails, String legsStr);

    void scannerDataEnd();
}