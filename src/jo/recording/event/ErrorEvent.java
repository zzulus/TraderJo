package jo.recording.event;

public class ErrorEvent extends AbstractEvent {
    public static final String TYPE = "Error";
    private int orderId;
    private int errorCode;
    private String errorMsg;

    public ErrorEvent() {
        super(TYPE);
    }

    public ErrorEvent(int orderId, int errorCode, String errorMsg) {
        super(TYPE);
        this.orderId = orderId;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

}
