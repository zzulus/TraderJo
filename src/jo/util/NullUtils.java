package jo.util;

public class NullUtils {
    public static boolean anyNull(Object... vals) {
        for (Object val : vals) {
            if (val == null) {
                return true;
            }
        }

        return false;
    }
}
