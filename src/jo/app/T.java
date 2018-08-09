package jo.app;

import java.util.ArrayList;
import java.util.List;

public class T {
    public static void main(String[] args) {
        getStrikePrices(1244);
    }

    static List<Integer> getStrikePrices(double price) {
        int i = (int) (((int) (price) / 5) * 5);
        if (i > price) {
            i -= 5;
        }
        List<Integer> result = new ArrayList<>();
        if (price >= 200) {
            result.add(i - 10);
        }
        result.add(i - 5);
        result.add(i);
        result.add(i + 5);
        result.add(i + 10);

        
        System.out.println(price + " = " + result);
        return result;
    }
}
