package jo.recording.event;

public class OrderErrorEvent extends AbstractEvent {
    public static final String TYPE = "OrderError";
    private int orderId;
    private int errorCode;
    private String errorMsg;

    public OrderErrorEvent() {
        super(TYPE);
    }

    public OrderErrorEvent(int orderId, int errorCode, String errorMsg) {
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
