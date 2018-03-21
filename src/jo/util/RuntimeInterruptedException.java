package jo.util;

@SuppressWarnings("serial")
public class RuntimeInterruptedException extends RuntimeException {
    public RuntimeInterruptedException(Throwable cause) {
        super(cause);
    }
}
