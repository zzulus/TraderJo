package jo.cmd;

import jo.app.TraderJoApp;

public class MainCmd {
    public static void main(String[] args) {
        TraderJoApp mainEventListner = new TraderJoApp();
        mainEventListner.connect();
    }
}
